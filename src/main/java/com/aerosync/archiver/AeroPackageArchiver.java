package com.aerosync.archiver;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.categories.*;
import com.aerosync.model.SyncOptions;
import com.aerosync.modrinth.ModrinthModDownloader;
import com.aerosync.modrinth.ModrinthModManifest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Unified Orchestrator for archiving and extracting AeroSync packages.
 * Includes automatic backup creation prior to overwriting existing world directories.
 */
public class AeroPackageArchiver {

    public interface PackProgressListener {
        void onProgress(String currentEntry, long processedBytes, long totalBytes);
    }

    private static final ExecutorService ARCHIVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AeroSync-Package-Archiver");
        thread.setDaemon(true);
        return thread;
    });

    private static final CategoryArchiver WORLD_ARCHIVER = new WorldCategoryArchiver();
    private static final CategoryArchiver CONFIG_ARCHIVER = new ConfigCategoryArchiver();
    private static final CategoryArchiver SHADERPACK_ARCHIVER = new ShaderpackCategoryArchiver();
    private static final CategoryArchiver MOD_ARCHIVER = new ModCategoryArchiver();
    private static final CategoryArchiver RESOURCEPACK_ARCHIVER = new ResourcepackCategoryArchiver();
    private static final CategoryArchiver SCREENSHOTS_ARCHIVER = new ScreenshotsCategoryArchiver();
    private static final CategoryArchiver OPTIONS_ARCHIVER = new OptionsCategoryArchiver();

    /**
     * Packs all requested categories into a single ZIP archive package.
     */
    public static File pack(Path gameDir, String worldName, SyncOptions options) throws IOException {
        return pack(gameDir, worldName, options, null);
    }

    public static File pack(
            Path gameDir,
            String worldName,
            SyncOptions options,
            PackProgressListener progressListener
    ) throws IOException {
        Path zipPath = Files.createTempFile("aerosync_package_" + (worldName != null ? worldName : "custom") + "_", ".zip");
        long totalBytes = calculateTotalInputBytes(gameDir, worldName, options);

        try (ZipOutputStream zos = new ProgressZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipPath)),
                progressListener,
                totalBytes
        )) {
            if (options.isSyncWorld()) {
                AeroSyncMod.LOGGER.info("Archiving World category...");
                WORLD_ARCHIVER.pack(zos, gameDir, worldName);
            }
            if (options.isSyncConfig()) {
                AeroSyncMod.LOGGER.info("Archiving Config category...");
                CONFIG_ARCHIVER.pack(zos, gameDir, worldName);
            }
            if (options.isSyncShaderpacks()) {
                AeroSyncMod.LOGGER.info("Archiving Shaderpacks category...");
                SHADERPACK_ARCHIVER.pack(zos, gameDir, worldName);
            }
            if (options.isSyncMods()) {
                AeroSyncMod.LOGGER.info("Archiving Mods category...");
                MOD_ARCHIVER.pack(zos, gameDir, worldName);
            }
            if (options.isSyncResourcepacks()) {
                AeroSyncMod.LOGGER.info("Archiving Resourcepacks category...");
                RESOURCEPACK_ARCHIVER.pack(zos, gameDir, worldName);
            }
            if (options.isSyncScreenshots()) {
                AeroSyncMod.LOGGER.info("Archiving Screenshots category...");
                SCREENSHOTS_ARCHIVER.pack(zos, gameDir, worldName);
            }
            if (options.isSyncOptionsFile()) {
                AeroSyncMod.LOGGER.info("Archiving Options category...");
                OPTIONS_ARCHIVER.pack(zos, gameDir, worldName);
            }
        }

        if (progressListener != null) {
            progressListener.onProgress("aerosync.phase.done", totalBytes, totalBytes);
        }
        AeroSyncMod.LOGGER.info("Package successfully created at: {}", zipPath.toAbsolutePath());
        return zipPath.toFile();
    }

    public static CompletableFuture<File> packAsync(Path gameDir, String worldName, SyncOptions options) {
        return packAsync(gameDir, worldName, options, null);
    }

    public static CompletableFuture<File> packAsync(
            Path gameDir,
            String worldName,
            SyncOptions options,
            PackProgressListener progressListener
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return pack(gameDir, worldName, options, progressListener);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create package", e);
            }
        }, ARCHIVE_EXECUTOR);
    }

    private static long calculateTotalInputBytes(Path gameDir, String worldName, SyncOptions options) {
        long totalBytes = 0L;
        if (options.isSyncWorld() && worldName != null && !worldName.isBlank()) {
            totalBytes += sumRegularFileBytes(
                    gameDir.resolve("saves").resolve(worldName),
                    path -> !path.getFileName().toString().equalsIgnoreCase("session.lock")
            );
        }
        if (options.isSyncConfig()) {
            totalBytes += sumRegularFileBytes(gameDir.resolve("config"), path -> true);
        }
        if (options.isSyncShaderpacks()) {
            totalBytes += sumRegularFileBytes(gameDir.resolve("shaderpacks"), path -> true);
        }
        if (options.isSyncResourcepacks()) {
            totalBytes += sumRegularFileBytes(gameDir.resolve("resourcepacks"), path -> true);
        }
        if (options.isSyncScreenshots()) {
            totalBytes += sumRegularFileBytes(gameDir.resolve("screenshots"), path -> true);
        }
        if (options.isSyncOptionsFile()) {
            totalBytes += regularFileSize(gameDir.resolve("options.txt"));
            totalBytes += regularFileSize(gameDir.resolve("optionsof.txt"));
            totalBytes += regularFileSize(gameDir.resolve("optionsshaders.txt"));
        }
        if (options.isSyncMods()) {
            totalBytes += 1L;
        }
        return Math.max(totalBytes, 1L);
    }

    private static long sumRegularFileBytes(Path root, Predicate<Path> filter) {
        if (!Files.isDirectory(root)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(filter)
                    .mapToLong(AeroPackageArchiver::regularFileSize)
                    .sum();
        } catch (IOException e) {
            AeroSyncMod.LOGGER.warn("Could not calculate package size for {}: {}", root, e.getMessage());
            return 0L;
        }
    }

    private static long regularFileSize(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.size(path) : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    private static final class ProgressZipOutputStream extends ZipOutputStream {
        private static final long REPORT_STEP_BYTES = 1024L * 1024L;

        private final PackProgressListener listener;
        private final long totalBytes;
        private long processedBytes;
        private long lastReportedBytes;
        private String currentEntry = "aerosync.status.preparing";

        private ProgressZipOutputStream(
                OutputStream outputStream,
                PackProgressListener listener,
                long totalBytes
        ) {
            super(outputStream);
            this.listener = listener;
            this.totalBytes = totalBytes;
        }

        @Override
        public void putNextEntry(ZipEntry entry) throws IOException {
            currentEntry = entry.getName();
            reportProgress(true);
            super.putNextEntry(entry);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            super.write(bytes, offset, length);
            processedBytes += length;
            reportProgress(false);
        }

        private void reportProgress(boolean force) {
            if (listener == null) {
                return;
            }
            if (force || processedBytes - lastReportedBytes >= REPORT_STEP_BYTES || processedBytes >= totalBytes) {
                lastReportedBytes = processedBytes;
                listener.onProgress(currentEntry, Math.min(processedBytes, totalBytes), totalBytes);
            }
        }
    }

    /**
     * Unpacks all package categories into the base game directory (.minecraft/).
     * Creates automatic backups of existing target world directories before overwriting.
     */
    public static Path unpack(File zipFile, Path gameDir) throws IOException {
        byte[] buffer = new byte[8192];
        Set<String> backedUpWorlds = new HashSet<>();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (ModrinthModManifest.ENTRY_NAME.equals(entry.getName())) {
                    String manifestJson = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    ModrinthModDownloader.DownloadResult result;
                    try {
                        result = ModrinthModDownloader.installFromManifest(manifestJson, gameDir);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while downloading mods from Modrinth", e);
                    }
                    if (!result.missing().isEmpty()) {
                        AeroSyncMod.LOGGER.warn("Modrinth could not install {} mods: {}", result.missing().size(), result.missing());
                    }
                    AeroSyncMod.LOGGER.info("Modrinth mods installed: {}, skipped: {}", result.installed(), result.skipped());
                    zis.closeEntry();
                    continue;
                }

                Path targetPath = zipSlipProtect(entry, gameDir);

                // Auto-backup existing world folder before overwriting
                if (entry.getName().startsWith("saves/")) {
                    String[] parts = entry.getName().split("/");
                    if (parts.length > 1) {
                        String worldFolder = parts[1];
                        if (!backedUpWorlds.contains(worldFolder)) {
                            backedUpWorlds.add(worldFolder);
                            Path existingWorldPath = gameDir.resolve("saves").resolve(worldFolder);
                            if (Files.exists(existingWorldPath)) {
                                createWorldBackup(existingWorldPath);
                            }
                        }
                    }
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Path parent = targetPath.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetPath))) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        AeroSyncMod.LOGGER.info("Package unpacked successfully into {}", gameDir.toAbsolutePath());
        return gameDir;
    }

    private static void createWorldBackup(Path worldPath) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupPath = worldPath.getParent().resolve(worldPath.getFileName().toString() + "_backup_" + timeStamp);
            
            Files.walkFileTree(worldPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = worldPath.relativize(file);
                    Path target = backupPath.resolve(relative);
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            AeroSyncMod.LOGGER.info("Created safety backup of existing world: {}", backupPath.toAbsolutePath());
        } catch (Exception e) {
            AeroSyncMod.LOGGER.warn("Failed to create world backup prior to extraction: {}", e.getMessage());
        }
    }

    public static CompletableFuture<Path> unpackAsync(File zipFile, Path gameDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return unpack(zipFile, gameDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to unpack package", e);
            }
        }, ARCHIVE_EXECUTOR);
    }

    public static void deleteTempPackage(File zipFile) {
        if (zipFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(zipFile.toPath());
        } catch (IOException e) {
            AeroSyncMod.LOGGER.warn("Failed to delete temp package: {}", zipFile, e);
        }
    }

    private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetPath = targetDir.resolve(zipEntry.getName()).normalize();
        if (!targetPath.startsWith(targetDir.normalize())) {
            throw new IOException("Bad zip entry (Zip Slip attack detected): " + zipEntry.getName());
        }
        return targetPath;
    }
}
