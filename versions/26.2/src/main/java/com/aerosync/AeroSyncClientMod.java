package com.aerosync;

import net.fabricmc.api.ClientModInitializer;

public class AeroSyncClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AeroSyncMod.LOGGER.info("AeroSync Client Initialized Successfully!");
    }
}
