package com.aerosync.archiver.categories;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.CategoryArchiver;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldCategoryArchiver implements CategoryArchiver {

    @Override
    public String getCategoryPrefix() {
        return "saves";
    }

    @Override
    public void pack(ZipOutputStream zos, Path gameDir, String worldName) throws IOException {
        if (worldName == null || worldName.isBlank()) return;

        Path savesDir = gameDir.resolve("saves").resolve(worldName);
        if (!Files.exists(savesDir) || !Files.isDirectory(savesDir)) {
            AeroSyncMod.LOGGER.warn("World directory not found for sync: {}", savesDir);
            return;
        }

        Path targetPrefix = Paths.get("saves").resolve(worldName);

        Files.walkFileTree(savesDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equalsIgnoreCase("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativeInSource = savesDir.relativize(file);
                String entryPath = targetPrefix.resolve(relativeInSource).toString().replace('\\', '/');

                zos.putNextEntry(new ZipEntry(entryPath));
                Files.copy(file, zos);
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                AeroSyncMod.LOGGER.warn("Skipping unreadable world file: {}", file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
