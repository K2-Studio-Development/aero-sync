package com.aerosync.archiver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

/**
 * Interface representing a dedicated category sub-archiver (World, Config, Mods, Shaders, Resourcepacks).
 */
public interface CategoryArchiver {

    /**
     * Gets the identifier prefix of this category in the zip archive (e.g. "saves", "config", "mods").
     */
    String getCategoryPrefix();

    /**
     * Packs the specific category directory into the target ZipOutputStream.
     *
     * @param zos Target ZIP output stream
     * @param gameDir Base .minecraft/ directory path
     * @param worldName Target world folder name (relevant for World category)
     */
    void pack(ZipOutputStream zos, Path gameDir, String worldName) throws IOException;
}
