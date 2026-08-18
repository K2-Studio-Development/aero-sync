package com.aerosync.archiver.categories;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.CategoryArchiver;
import com.aerosync.modrinth.ModrinthModManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ModCategoryArchiver implements CategoryArchiver {

    @Override
    public String getCategoryPrefix() {
        return "mods";
    }

    @Override
    public void pack(ZipOutputStream zos, Path gameDir, String worldName) throws IOException {
        Path modsDir = gameDir.resolve("mods");
        if (!Files.exists(modsDir) || !Files.isDirectory(modsDir)) return;

        String manifestJson = ModrinthModManifest.createJson(modsDir);
        zos.putNextEntry(new ZipEntry(ModrinthModManifest.ENTRY_NAME));
        zos.write(manifestJson.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        AeroSyncMod.LOGGER.info("Packed Modrinth mod manifest instead of raw mod jars.");
    }
}
