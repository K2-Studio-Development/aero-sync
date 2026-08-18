package com.aerosync.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

public class AeroToastUtils {

    public static void showToast(String titleKey, String descriptionKey, Object... descriptionArgs) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                SystemToast.add(
                        client.getToastManager(),
                        SystemToast.Type.NARRATOR_TOGGLE,
                        Text.translatable("aerosync.toast.title", Text.translatable(titleKey)),
                        Text.translatable(descriptionKey, descriptionArgs)
                );
            });
        }
    }

    public static void showErrorToast(String titleKey, String descriptionKey, Object... descriptionArgs) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                SystemToast.add(
                        client.getToastManager(),
                        SystemToast.Type.NARRATOR_TOGGLE,
                        Text.translatable("aerosync.toast.error_title", Text.translatable(titleKey)),
                        Text.translatable(descriptionKey, descriptionArgs)
                );
            });
        }
    }
}
