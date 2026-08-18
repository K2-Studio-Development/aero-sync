package com.aerosync.archiver.categories;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.CategoryArchiver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OptionsCategoryArchiver implements CategoryArchiver {

    @Override
    public String getCategoryPrefix() {
        return "options";
    }

    @Override
    public void pack(ZipOutputStream zos, Path gameDir, String worldName) throws IOException {
        // List of options files to include (standard options.txt, OptiFine optionsof.txt, optionsshaders.txt)
        String[] optionFiles = new String[] {
            "options.txt",
            "optionsof.txt",
            "optionsshaders.txt"
        };

        for (String fileName : optionFiles) {
            Path file = gameDir.resolve(fileName);
            if (Files.exists(file) && Files.isRegularFile(file)) {
                zos.putNextEntry(new ZipEntry(fileName));
                Files.copy(file, zos);
                zos.closeEntry();
                AeroSyncMod.LOGGER.info("Packed options file: {}", fileName);
            }
        }
    }
}
