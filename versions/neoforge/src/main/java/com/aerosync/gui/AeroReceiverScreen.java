package com.aerosync.gui;

import com.aerosync.network.AeroIrohTransport;
import com.aerosync.network.AeroMessage;
import com.aerosync.network.AeroReceiver;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class AeroReceiverScreen extends Screen {
    private final Screen parent;
    private EditBox addressField;
    private volatile Component statusMessage = Component.translatable("aerosync.status.input_address");
    private volatile float progress;
    private boolean connecting;

    public AeroReceiverScreen(Screen parent) {
        super(Component.translatable("aerosync.title.receiver"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = 45;
        addressField = new EditBox(font, centerX - 140, startY + 20, 280, 20, Component.translatable("aerosync.label.address_field"));
        addressField.setMaxLength(4096);
        addressField.setHint(Component.literal("AEROSYNC:..."));
        addRenderableWidget(addressField);
        addRenderableWidget(Button.builder(Component.translatable("aerosync.button.connect_download"), button -> {
            if (!connecting) startReceiving();
        }).bounds(centerX - 90, startY + 60, 180, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("aerosync.button.back"), button -> minecraft.setScreen(parent))
                .bounds(centerX - 50, startY + 90, 100, 20).build());
    }

    private void startReceiving() {
        String address = addressField.getValue().trim();
        if (!AeroIrohTransport.isConnectionAddress(address)) {
            statusMessage = Component.translatable("aerosync.status.invalid_address");
            AeroToastUtils.showErrorToast("aerosync.toast.input_error", "aerosync.toast.input_error_description");
            return;
        }
        connecting = true;
        Minecraft client = Minecraft.getInstance();
        AeroReceiver.receivePackageAsync(address, new AeroReceiver.ReceiverListener() {
            @Override public void onProgress(long received, long total) {
                if (total > 0) progress = Math.min(1.0f, (float) received / total);
            }
            @Override public void onStatusChange(AeroMessage status) {
                client.execute(() -> statusMessage = Component.translatable(status.key(), status.args()));
            }
            @Override public void onError(AeroMessage message, Throwable cause) {
                client.execute(() -> {
                    Component translated = Component.translatable(message.key(), message.args());
                    statusMessage = Component.translatable("aerosync.status.error", translated);
                    connecting = false;
                    AeroToastUtils.showErrorToast(
                            "aerosync.toast.connection_failure",
                            "aerosync.toast.technical_detail",
                            translated
                    );
                });
            }
            @Override public void onSuccess(Path extractedGameDir) {
                client.execute(() -> {
                    statusMessage = Component.translatable("aerosync.status.received");
                    progress = 1.0f;
                    connecting = false;
                    AeroToastUtils.showToast("aerosync.toast.sync_complete", "aerosync.toast.sync_complete_description");
                });
            }
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = width / 2;
        int startY = 45;
        graphics.drawCenteredString(font, title, centerX, 15, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("aerosync.label.receiver_prompt"), centerX, startY, 0xA0A0A0);
        graphics.drawCenteredString(font, statusMessage, centerX, startY + 125, 0xFFFF55);
        int barX = centerX - 100;
        int barY = startY + 145;
        graphics.fill(barX, barY, barX + 200, barY + 12, 0xFF555555);
        graphics.fill(barX, barY, barX + (int) (200 * progress), barY + 12, 0xFF00AA00);
        graphics.drawCenteredString(font, (int) (progress * 100) + "%", centerX, barY + 2, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC0101010);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
