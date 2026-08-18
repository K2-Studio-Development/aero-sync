package com.aerosync.neoforge;

import com.aerosync.AeroSyncMod;
import com.aerosync.gui.AeroSyncMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class AeroScreenHooks {
    private AeroScreenHooks() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) {
            return;
        }
        int thresholdY = screen.height / 4 + (screen instanceof TitleScreen ? 115 : 98);
        for (var listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget && widget.getY() >= thresholdY) {
                widget.setY(widget.getY() + 24);
            }
        }
        Component label = screen instanceof PauseScreen
                ? Component.translatable("aerosync.button.menu_version", AeroSyncMod.getVersion())
                : Component.translatable("aerosync.button.menu");
        int y = screen.height / 4 + (screen instanceof TitleScreen ? 120 : 104);
        event.addListener(Button.builder(label, button -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new AeroSyncMainScreen(screen));
        }).bounds(screen.width / 2 - 102, y, 204, 20).build());
    }
}
