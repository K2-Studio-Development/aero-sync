package com.aerosync.platform;

import java.nio.file.Path;

public final class AeroGamePaths {
    private AeroGamePaths() {
    }

    public static Path gameDirectory() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (Path) loaderClass.getMethod("getGameDir").invoke(loader);
        } catch (ReflectiveOperationException ignored) {
            try {
                Class<?> pathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                Object gameDir = pathsClass.getField("GAMEDIR").get(null);
                return (Path) gameDir.getClass().getMethod("get").invoke(gameDir);
            } catch (ReflectiveOperationException exception) {
                return Path.of(System.getProperty("user.dir"));
            }
        }
    }
}
