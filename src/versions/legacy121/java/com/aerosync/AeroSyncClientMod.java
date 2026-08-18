package com.aerosync;

import com.aerosync.gui.AeroSyncMainScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AeroSyncClientMod implements ClientModInitializer {

    private static KeyBinding syncKeyBinding;

    @Override
    public void onInitializeClient() {
        try {
            syncKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.aerosync.open_gui",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_O,
                    KeyBinding.MISC_CATEGORY
            ));

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (syncKeyBinding != null) {
                    while (syncKeyBinding.wasPressed()) {
                        if (client.currentScreen == null) {
                            client.setScreen(new AeroSyncMainScreen(null));
                        }
                    }
                }
            });
            AeroSyncMod.LOGGER.info("AeroSync Client Initialized Successfully!");
        } catch (Throwable t) {
            AeroSyncMod.LOGGER.error("Failed to initialize AeroSync Client", t);
        }
    }
}

