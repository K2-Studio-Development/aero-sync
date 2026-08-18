package com.aerosync.universal;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AeroSyncUniversalClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("aerosync");
    private static final Map<String, String> IMPLEMENTATIONS = Map.ofEntries(
            Map.entry("1.21.1", "v01.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.2", "v02.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.3", "v03.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.4", "v04.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.5", "v05.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.6", "v06.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.7", "v07.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.8", "v08.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.9", "v09.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.10", "v10.aerosync.AeroSyncClientMod"),
            Map.entry("1.21.11", "v11.aerosync.AeroSyncClientMod"),
            Map.entry("26.1", "v12.aerosync.AeroSyncClientMod"),
            Map.entry("26.2", "v13.aerosync.AeroSyncClientMod")
    );

    @Override
    public void onInitializeClient() {
        String minecraftVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElseThrow(() -> new IllegalStateException("Minecraft version is unavailable"));
        String implementationClass = IMPLEMENTATIONS.get(minecraftVersion);
        if (implementationClass == null) {
            throw new IllegalStateException("AeroSync does not support Minecraft " + minecraftVersion);
        }

        try {
            ClientModInitializer implementation = (ClientModInitializer) Class.forName(implementationClass)
                    .getDeclaredConstructor()
                    .newInstance();
            implementation.onInitializeClient();
            LOGGER.info("Loaded AeroSync compatibility layer for Minecraft {}", minecraftVersion);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException && exception.getCause() != null
                    ? exception.getCause()
                    : exception;
            throw new IllegalStateException(
                    "Failed to load AeroSync compatibility layer for Minecraft " + minecraftVersion,
                    cause
            );
        }
    }
}
