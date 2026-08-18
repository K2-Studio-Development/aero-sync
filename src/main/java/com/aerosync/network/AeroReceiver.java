package com.aerosync.network;

import com.aerosync.platform.AeroGamePaths;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Receives an AeroSync package through a complete Iroh P2P connection address.
 */
public class AeroReceiver {

    public interface ReceiverListener {
        void onProgress(long bytesReceived, long totalBytes);
        void onStatusChange(AeroMessage statusMessage);
        void onError(AeroMessage errorMessage, Throwable cause);
        void onSuccess(Path extractedGameDir);
    }

    public static CompletableFuture<Path> receivePackageAsync(
            String connectionAddress,
            ReceiverListener listener
    ) {
        Path gameDir = AeroGamePaths.gameDirectory();
        return AeroIrohTransport.receivePackageAsync(connectionAddress, gameDir, listener);
    }
}
