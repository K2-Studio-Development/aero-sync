package com.aerosync.network;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Sends an AeroSync package over a single Iroh P2P connection address.
 */
public class AeroSender {

    private final String sessionSecret;
    private final CompletableFuture<File> packageFuture;
    private final String worldName;

    public interface ProgressListener {
        default void onConnectionCode(String connectionAddress) {}
        void onProgress(long bytesSent, long totalBytes);
        void onStatusChange(AeroMessage status);
        void onError(AeroMessage error, Throwable cause);
        void onCompleted();
    }

    public AeroSender(CompletableFuture<File> packageFuture, String worldName) {
        this.sessionSecret = AeroNetworkUtils.generate6DigitCode();
        this.packageFuture = packageFuture;
        this.worldName = worldName;
    }

    public CompletableFuture<Void> startAsync(ProgressListener listener) {
        return AeroIrohTransport.startSender(packageFuture, worldName, sessionSecret, listener);
    }

    public void stop() {
        AeroIrohTransport.stopSender();
    }
}
