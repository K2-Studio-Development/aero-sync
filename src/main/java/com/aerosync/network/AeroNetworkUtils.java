package com.aerosync.network;

import java.security.SecureRandom;

public final class AeroNetworkUtils {
    private static final SecureRandom RANDOM = new SecureRandom();

    private AeroNetworkUtils() {
    }

    public static String generate6DigitCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }
}
