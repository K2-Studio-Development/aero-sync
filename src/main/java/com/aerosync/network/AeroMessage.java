package com.aerosync.network;

import java.util.Arrays;

public final class AeroMessage {
    private final String key;
    private final Object[] args;

    private AeroMessage(String key, Object[] args) {
        this.key = key;
        this.args = args;
    }

    public static AeroMessage of(String key, Object... args) {
        return new AeroMessage(key, Arrays.copyOf(args, args.length));
    }

    public String key() {
        return key;
    }

    public Object[] args() {
        return Arrays.copyOf(args, args.length);
    }
}
