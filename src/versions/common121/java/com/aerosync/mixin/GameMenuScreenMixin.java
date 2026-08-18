package com.aerosync.mixin;

import com.aerosync.AeroSyncMod;
import com.aerosync.gui.AeroSyncMainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void addAeroSyncPauseButton(CallbackInfo ci) {
        int thresholdY = this.height / 4 + 98;

        // Shift lower buttons (Save & Quit to Title) down by 24px to free up space
        for (Element element : this.children()) {
            if (element instanceof ClickableWidget widget) {
                if (widget.getY() >= thresholdY) {
                    widget.setY(widget.getY() + 24);
                }
            }
        }

        // Insert full-width AeroSync P2P button into the newly created row
        int buttonWidth = 204;
        int buttonHeight = 20;
        int x = this.width / 2 - 102;
        int y = this.height / 4 + 104;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.menu_version", AeroSyncMod.getVersion()), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new AeroSyncMainScreen(this));
            }
        }).dimensions(x, y, buttonWidth, buttonHeight).build());
    }
}
