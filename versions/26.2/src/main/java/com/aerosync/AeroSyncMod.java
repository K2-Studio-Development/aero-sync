package com.aerosync;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AeroSyncMod implements ModInitializer {
    public static final String MOD_ID = "aerosync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static String getVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("AeroSync P2P World Synchronization Mod Initialized!");
    }
}
