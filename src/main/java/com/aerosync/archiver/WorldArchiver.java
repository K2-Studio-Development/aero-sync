package com.aerosync.archiver;

import com.aerosync.model.SyncOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Backward compatibility wrapper delegating to AeroPackageArchiver.
 */
@Deprecated
public class WorldArchiver {

    public static File compressWorld(Path savesDir, String worldName) throws IOException {
        SyncOptions options = new SyncOptions(true, false, false, false, false, false, false);
        return AeroPackageArchiver.pack(savesDir.getParent() != null ? savesDir.getParent() : savesDir, worldName, options);
    }

    public static CompletableFuture<File> compressWorldAsync(Path savesDir, String worldName) {
        SyncOptions options = new SyncOptions(true, false, false, false, false, false, false);
        return AeroPackageArchiver.packAsync(savesDir.getParent() != null ? savesDir.getParent() : savesDir, worldName, options);
    }

    public static Path decompressWorld(File zipFile, Path targetSavesDir, String targetWorldName) throws IOException {
        return AeroPackageArchiver.unpack(zipFile, targetSavesDir.getParent() != null ? targetSavesDir.getParent() : targetSavesDir);
    }

    public static CompletableFuture<Path> decompressWorldAsync(File zipFile, Path targetSavesDir, String targetWorldName) {
        return AeroPackageArchiver.unpackAsync(zipFile, targetSavesDir.getParent() != null ? targetSavesDir.getParent() : targetSavesDir);
    }
}
