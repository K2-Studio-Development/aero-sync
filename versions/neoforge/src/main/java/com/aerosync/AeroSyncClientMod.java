package com.aerosync;

import com.aerosync.neoforge.AeroScreenHooks;
import net.neoforged.neoforge.common.NeoForge;

public final class AeroSyncClientMod {
    private static boolean initialized;

    private AeroSyncClientMod() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        NeoForge.EVENT_BUS.addListener(AeroScreenHooks::onScreenInit);
        AeroSyncMod.LOGGER.info("AeroSync NeoForge client initialized successfully!");
    }
}
