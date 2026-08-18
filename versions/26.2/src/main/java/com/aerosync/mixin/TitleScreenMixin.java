package com.aerosync.mixin;

import com.aerosync.gui.AeroSyncMainScreen;
import com.aerosync.platform.AeroClientUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addAeroSyncTitleButton(CallbackInfo callbackInfo) {
        int thresholdY = this.height / 4 + 115;
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget && widget.getY() >= thresholdY) {
                widget.setY(widget.getY() + 24);
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.menu"), button -> {
            Minecraft client = Minecraft.getInstance();
            AeroClientUi.setScreen(client, new AeroSyncMainScreen(this));
        }).bounds(this.width / 2 - 102, this.height / 4 + 120, 204, 20).build());
    }
}
