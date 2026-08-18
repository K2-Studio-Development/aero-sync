package com.aerosync.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;

public final class AeroClientUi {

    private AeroClientUi() {
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    public static ToastManager toastManager(Minecraft client) {
        return client.getToastManager();
    }
}

