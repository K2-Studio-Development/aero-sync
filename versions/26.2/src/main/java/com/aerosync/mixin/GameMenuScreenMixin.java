package com.aerosync.mixin;

import com.aerosync.AeroSyncMod;
import com.aerosync.gui.AeroSyncMainScreen;
import com.aerosync.platform.AeroClientUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addAeroSyncPauseButton(CallbackInfo callbackInfo) {
        int thresholdY = this.height / 4 + 98;
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget && widget.getY() >= thresholdY) {
                widget.setY(widget.getY() + 24);
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.menu_version", AeroSyncMod.getVersion()), button -> {
            Minecraft client = Minecraft.getInstance();
            AeroClientUi.setScreen(client, new AeroSyncMainScreen(this));
        }).bounds(this.width / 2 - 102, this.height / 4 + 104, 204, 20).build());
    }
}
