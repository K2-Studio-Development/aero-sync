package com.aerosync.network

import com.aerosync.AeroSyncMod
import com.aerosync.archiver.AeroPackageArchiver
import computer.iroh.Endpoint
import computer.iroh.EndpointOptions
import computer.iroh.EndpointTicket
import computer.iroh.IrohException
import computer.iroh.SendStream
import computer.iroh.presetN0
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

object AeroIrohTransport {
    private val ALPN = "aerosync/package/3".toByteArray(StandardCharsets.UTF_8)
    private const val ADDRESS_PREFIX = "AEROSYNC:"
    private const val LEGACY_ADDRESS_PREFIX = "IROH:"
    private const val BUFFER_SIZE = 256 * 1024
    private const val PREPARING_STATUS = "PREPARING"
    private const val UNPACKING_STATUS = "UNPACKING"
    private val activeSenderEndpoint = AtomicReference<Endpoint?>()

    @JvmStatic
    fun isConnectionAddress(address: String): Boolean {
        val normalized = address.trim().uppercase()
        return normalized.startsWith(ADDRESS_PREFIX) || normalized.startsWith(LEGACY_ADDRESS_PREFIX)
    }

    @JvmStatic
    fun stopSender() {
        activeSenderEndpoint.getAndSet(null)?.close()
    }

    @JvmStatic
    fun startSender(
        packageFuture: CompletableFuture<File>,
        worldName: String,
        sessionCode: String,
        listener: AeroSender.ProgressListener?
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            runBlocking {
                var endpoint: Endpoint? = null
                var zipFile: File? = null
                try {
                    endpoint = Endpoint.bind(EndpointOptions(preset = presetN0(), alpns = listOf(ALPN)))
                    activeSenderEndpoint.getAndSet(endpoint)?.close()
                    try {
                        withTimeout(30_000) {
                            endpoint.online()
                        }
                    } catch (e: Exception) {
                        throw IOException("Could not connect to the Iroh relay: ${describeError(e)}", e)
                    }

                    val ticket = EndpointTicket.fromAddr(endpoint.addr()).toString()
                    val connectionAddress = "$ADDRESS_PREFIX$sessionCode:$ticket"
                    listener?.onConnectionCode(connectionAddress)
                    listener?.onStatusChange(AeroMessage.of(
                        if (packageFuture.isDone) {
                            "aerosync.status.address_ready_waiting"
                        } else {
                            "aerosync.status.address_ready_packing"
                        }
                    ))
                    AeroSyncMod.LOGGER.info("AeroSync Iroh sender is waiting for receiver.")

                    val incoming = withTimeoutOrNull(600_000) {
                        endpoint.acceptNext()
                    }
                    if (incoming == null) {
                        listener?.onStatusChange(AeroMessage.of("aerosync.status.address_expired"))
                        return@runBlocking
                    }
                    val accepting = incoming.accept()
                    val connection = accepting.connect()
                    val stream = connection.acceptBi()

                    val recv = stream.recv()
                    val send = stream.send()
                    val receivedPin = readString(recv)
                    if (receivedPin != sessionCode) {
                        writeString(send, "ERR_INVALID_PIN")
                        send.finish()
                        listener?.onError(AeroMessage.of("aerosync.error.invalid_code"), null)
                        return@runBlocking
                    }

                    if (!packageFuture.isDone) {
                        listener?.onStatusChange(AeroMessage.of("aerosync.status.friend_connected_packing"))
                    }
                    while (!packageFuture.isDone) {
                        writeString(send, PREPARING_STATUS)
                        delay(3_000)
                    }
                    zipFile = try {
                        packageFuture.get()
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        throw IOException("Could not create the archive: ${describeError(cause)}", cause)
                    }

                    writeString(send, "OK")
                    writeString(send, worldName)
                    writeLong(send, zipFile.length())

                    listener?.onStatusChange(AeroMessage.of("aerosync.status.sending_package", worldName))
                    var sentBytes = 0L
                    val totalBytes = zipFile.length()
                    val buffer = ByteArray(BUFFER_SIZE)
                    zipFile.inputStream().buffered().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) {
                                break
                            }
                            send.writeAll(buffer.copyOf(read))
                            sentBytes += read
                            listener?.onProgress(sentBytes, totalBytes)
                        }
                    }
                    send.finish()

                    listener?.onStatusChange(AeroMessage.of("aerosync.status.sent_waiting"))
                    val acknowledgement = withTimeout(1_800_000) {
                        var response = readString(recv)
                        while (response == UNPACKING_STATUS) {
                            listener?.onStatusChange(AeroMessage.of("aerosync.status.recipient_unpacking"))
                            response = readString(recv)
                        }
                        response
                    }
                    if (acknowledgement != "RECEIVED_OK") {
                        throw IOException(
                            acknowledgement.removePrefix("RECEIVED_ERROR:").ifBlank {
                                "The recipient did not confirm successful synchronization"
                            }
                        )
                    }
                    listener?.onCompleted()
                } catch (e: Exception) {
                    val details = describeError(e)
                    AeroSyncMod.LOGGER.error("Iroh sender failed: {}", details, e)
                    listener?.onError(AeroMessage.of("aerosync.error.sender", details), e)
                } finally {
                    activeSenderEndpoint.compareAndSet(endpoint, null)
                    endpoint?.close()
                    if (zipFile != null) {
                        AeroPackageArchiver.deleteTempPackage(zipFile)
                    } else {
                        packageFuture.thenAccept(AeroPackageArchiver::deleteTempPackage)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun receivePackageAsync(
        connectionCode: String,
        gameDir: Path,
        listener: AeroReceiver.ReceiverListener?
    ): CompletableFuture<Path> {
        return CompletableFuture.supplyAsync {
            runBlocking {
                var endpoint: Endpoint? = null
                var tempZip: Path? = null
                var acknowledgementSend: SendStream? = null
                try {
                    val parsed = parseConnectionCode(connectionCode)
                    endpoint = Endpoint.bind(EndpointOptions(preset = presetN0(), alpns = listOf(ALPN)))
                    listener?.onStatusChange(AeroMessage.of("aerosync.status.sender_connecting"))

                    val ticket = EndpointTicket.fromString(parsed.ticket)
                    val connection = endpoint.connect(ticket.endpointAddr(), ALPN)
                    val stream = connection.openBi()

                    val send = stream.send()
                    acknowledgementSend = send
                    writeString(send, parsed.pin)

                    val recv = stream.recv()
                    var status = readString(recv)
                    while (status == PREPARING_STATUS) {
                        listener?.onStatusChange(AeroMessage.of("aerosync.status.sender_packing"))
                        status = readString(recv)
                    }
                    if (status != "OK") {
                        throw IOException("Iroh authorization failed: $status")
                    }

                    val packageName = readString(recv)
                    val totalBytes = readLong(recv)
                    listener?.onStatusChange(AeroMessage.of(
                        "aerosync.status.downloading_package",
                        packageName,
                        totalBytes / 1024 / 1024
                    ))

                    tempZip = Files.createTempFile("aerosync_iroh_", ".zip")
                    var receivedBytes = 0L
                    BufferedOutputStream(Files.newOutputStream(tempZip)).use { output ->
                        while (receivedBytes < totalBytes) {
                            val chunkLimit = min(BUFFER_SIZE.toLong(), totalBytes - receivedBytes).toInt()
                            val chunk = recv.read(chunkLimit.toUInt())
                            if (chunk.isEmpty()) {
                                throw IOException("Iroh stream ended early")
                            }
                            output.write(chunk)
                            receivedBytes += chunk.size
                            listener?.onProgress(receivedBytes, totalBytes)
                        }
                    }

                    listener?.onStatusChange(AeroMessage.of("aerosync.status.unpacking"))
                    val unpackFuture = AeroPackageArchiver.unpackAsync(tempZip.toFile(), gameDir)
                    while (!unpackFuture.isDone) {
                        writeString(send, UNPACKING_STATUS)
                        delay(3_000)
                    }
                    val extractedPath = try {
                        unpackFuture.get()
                    } catch (e: Exception) {
                        val cause = e.cause ?: e
                        throw IOException("Could not extract the archive: ${describeError(cause)}", cause)
                    }
                    writeString(send, "RECEIVED_OK")
                    send.finish()
                    acknowledgementSend = null
                    listener?.onSuccess(extractedPath)
                    extractedPath
                } catch (e: Exception) {
                    val details = describeError(e)
                    acknowledgementSend?.let { send ->
                        runCatching {
                            writeString(send, "RECEIVED_ERROR:$details")
                            send.finish()
                        }
                    }
                    AeroSyncMod.LOGGER.error("Iroh receive failed: {}", details, e)
                    listener?.onError(AeroMessage.of("aerosync.error.receiver", details), e)
                    throw RuntimeException(e)
                } finally {
                    tempZip?.let { Files.deleteIfExists(it) }
                    endpoint?.close()
                }
            }
        }
    }

    private data class ParsedCode(val pin: String, val ticket: String)

    private fun parseConnectionCode(connectionCode: String): ParsedCode {
        val parts = connectionCode.trim().split(":", limit = 3)
        require(parts.size == 3 && (parts[0].equals("AEROSYNC", true) || parts[0].equals("IROH", true))) {
            "Invalid AeroSync P2P address"
        }
        require(parts[1].length == 6 && parts[1].all(Char::isDigit)) {
            "Corrupted P2P address secret"
        }
        return ParsedCode(parts[1], parts[2])
    }

    private suspend fun writeString(send: computer.iroh.SendStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        send.writeAll(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
        send.writeAll(bytes)
    }

    private suspend fun readString(recv: computer.iroh.RecvStream): String {
        val length = ByteBuffer.wrap(recv.readExact(Int.SIZE_BYTES.toUInt())).int
        return String(recv.readExact(length.toUInt()), StandardCharsets.UTF_8)
    }

    private suspend fun writeLong(send: computer.iroh.SendStream, value: Long) {
        send.writeAll(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(value).array())
    }

    private suspend fun readLong(recv: computer.iroh.RecvStream): Long {
        return ByteBuffer.wrap(recv.readExact(Long.SIZE_BYTES.toUInt())).long
    }

    private fun describeError(error: Throwable): String {
        if (error is IrohException) {
            val kind = runCatching { error.kind().name }.getOrDefault("IROH")
            val message = runCatching { error.message() }.getOrNull()
            val debugMessage = runCatching { error.debugMessage() }.getOrNull()
            return listOfNotNull(kind, message, debugMessage)
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(": ")
        }
        return error.message ?: error.javaClass.simpleName
    }
}
