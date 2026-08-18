package com.aerosync.util;

import com.aerosync.platform.AeroClientUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class AeroToastUtils {

    private AeroToastUtils() {
    }

    public static void showToast(String titleKey, String descriptionKey, Object... descriptionArgs) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> SystemToast.add(
                AeroClientUi.toastManager(client),
                SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.translatable("aerosync.toast.title", Component.translatable(titleKey)),
                Component.translatable(descriptionKey, descriptionArgs)
        ));
    }

    public static void showErrorToast(String titleKey, String descriptionKey, Object... descriptionArgs) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> SystemToast.add(
                AeroClientUi.toastManager(client),
                SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.translatable("aerosync.toast.error_title", Component.translatable(titleKey)),
                Component.translatable(descriptionKey, descriptionArgs)
        ));
    }
}
