package com.aerosync.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class AeroToastUtils {
    private AeroToastUtils() {
    }

    public static void showToast(String titleKey, String descriptionKey, Object... descriptionArgs) {
        show(
                Component.translatable("aerosync.toast.title", Component.translatable(titleKey)),
                Component.translatable(descriptionKey, descriptionArgs)
        );
    }

    public static void showErrorToast(String titleKey, String descriptionKey, Object... descriptionArgs) {
        show(
                Component.translatable("aerosync.toast.error_title", Component.translatable(titleKey)),
                Component.translatable(descriptionKey, descriptionArgs)
        );
    }

    private static void show(Component title, Component description) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            try {
                Object manager;
                try {
                    manager = Minecraft.class.getMethod("getToasts").invoke(minecraft);
                } catch (NoSuchMethodException ignored) {
                    manager = Minecraft.class.getMethod("getToastManager").invoke(minecraft);
                }
                for (Method method : SystemToast.class.getMethods()) {
                    if (method.getName().equals("add") && Modifier.isStatic(method.getModifiers())
                            && method.getParameterCount() == 4) {
                        method.invoke(null, manager, SystemToast.SystemToastId.NARRATOR_TOGGLE,
                                title, description);
                        return;
                    }
                }
                throw new NoSuchMethodException("SystemToast.add");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to display AeroSync toast", exception);
            }
        });
    }
}
