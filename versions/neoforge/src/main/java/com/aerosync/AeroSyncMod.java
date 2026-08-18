package com.aerosync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AeroSyncMod {
    public static final String MOD_ID = "aerosync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private AeroSyncMod() {
    }

    public static String getVersion() {
        return "0.0.9";
    }
}
