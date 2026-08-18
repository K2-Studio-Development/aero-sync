package com.aerosync.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Mod(value = "aerosync", dist = Dist.CLIENT)
public final class AeroSyncNeoForgeMod {
    private static final Map<String, String> IMPLEMENTATIONS = Map.ofEntries(
            Map.entry("1.21.1", "n01.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.2", "n02.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.3", "n03.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.4", "n04.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.5", "n05.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.6", "n06.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.7", "n07.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.8", "n08.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.9", "n09.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.10", "n10.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.11", "n11.aerosync.AeroSyncClientMod"),
            Map.entry("26.1", "n12.aerosync.AeroSyncClientMod"),
            Map.entry("26.2", "n13.aerosync.AeroSyncClientMod")
    );

    public AeroSyncNeoForgeMod() {
        String minecraftVersion = minecraftVersion();
        String implementation = IMPLEMENTATIONS.get(minecraftVersion);
        if (implementation == null) {
            implementation = "com.aerosync.AeroSyncClientMod";
        }
        try {
            Class.forName(implementation).getMethod("initialize").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("AeroSync does not support Minecraft " + minecraftVersion, exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("AeroSync failed to initialize for Minecraft " + minecraftVersion,
                    exception.getCause());
        }
    }

    private static String minecraftVersion() {
        try {
            Class<?> loader = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object versionInfo;
            try {
                versionInfo = loader.getMethod("versionInfo").invoke(null);
            } catch (NoSuchMethodException ignored) {
                Object currentLoader = loader.getMethod("getCurrent").invoke(null);
                versionInfo = loader.getMethod("getVersionInfo").invoke(currentLoader);
            }
            return String.valueOf(versionInfo.getClass().getMethod("mcVersion").invoke(versionInfo));
        } catch (ReflectiveOperationException exception) {
            try {
                Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
                Object version = sharedConstants.getMethod("getCurrentVersion").invoke(null);
                try {
                    return String.valueOf(version.getClass().getMethod("name").invoke(version));
                } catch (NoSuchMethodException ignored) {
                    return String.valueOf(version.getClass().getMethod("getName").invoke(version));
                }
            } catch (ReflectiveOperationException ignored) {
                return System.getProperty("minecraft.version", "unknown");
            }
        }
    }
}
