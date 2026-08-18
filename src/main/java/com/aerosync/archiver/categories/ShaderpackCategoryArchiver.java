package com.aerosync.archiver.categories;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.CategoryArchiver;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ShaderpackCategoryArchiver implements CategoryArchiver {

    @Override
    public String getCategoryPrefix() {
        return "shaderpacks";
    }

    @Override
    public void pack(ZipOutputStream zos, Path gameDir, String worldName) throws IOException {
        Path shaderDir = gameDir.resolve("shaderpacks");
        if (!Files.exists(shaderDir) || !Files.isDirectory(shaderDir)) return;

        Path targetPrefix = Paths.get("shaderpacks");

        Files.walkFileTree(shaderDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativeInSource = shaderDir.relativize(file);
                String entryPath = targetPrefix.resolve(relativeInSource).toString().replace('\\', '/');

                zos.putNextEntry(new ZipEntry(entryPath));
                Files.copy(file, zos);
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                AeroSyncMod.LOGGER.warn("Skipping unreadable shaderpack file: {}", file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
