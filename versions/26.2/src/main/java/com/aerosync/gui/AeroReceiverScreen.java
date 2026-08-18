package com.aerosync.gui;

import com.aerosync.network.AeroIrohTransport;
import com.aerosync.network.AeroMessage;
import com.aerosync.network.AeroReceiver;
import com.aerosync.platform.AeroClientUi;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class AeroReceiverScreen extends Screen {

    private final Screen parent;
    private EditBox addressField;
    private volatile Component statusMessage = Component.translatable("aerosync.status.input_address");
    private volatile float progressPercentage;
    private boolean isConnecting;

    public AeroReceiverScreen(Screen parent) {
        super(Component.translatable("aerosync.title.receiver"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 45;

        this.addressField = new EditBox(this.font, centerX - 140, startY + 20, 280, 20, Component.translatable("aerosync.label.address_field"));
        this.addressField.setMaxLength(4096);
        this.addressField.setHint(Component.literal("AEROSYNC:..."));
        this.addRenderableWidget(this.addressField);

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.connect_download"), button -> {
            if (!isConnecting) {
                startReceiving();
            }
        }).bounds(centerX - 90, startY + 60, 180, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.back"), button -> {
            if (this.minecraft != null) {
                AeroClientUi.setScreen(this.minecraft, parent);
            }
        }).bounds(centerX - 50, startY + 90, 100, 20).build());
    }

    private void startReceiving() {
        String connectionAddress = addressField.getValue().trim();
        if (!AeroIrohTransport.isConnectionAddress(connectionAddress)) {
            statusMessage = Component.translatable("aerosync.status.invalid_address");
            AeroToastUtils.showErrorToast("aerosync.toast.input_error", "aerosync.toast.input_error_description");
            return;
        }

        isConnecting = true;
        Minecraft client = Minecraft.getInstance();

        AeroReceiver.receivePackageAsync(connectionAddress, new AeroReceiver.ReceiverListener() {
            @Override
            public void onProgress(long bytesReceived, long totalBytes) {
                if (totalBytes > 0) {
                    progressPercentage = (float) bytesReceived / totalBytes;
                }
            }

            @Override
            public void onStatusChange(AeroMessage value) {
                client.execute(() -> statusMessage = Component.translatable(value.key(), value.args()));
            }

            @Override
            public void onError(AeroMessage errorMessage, Throwable cause) {
                client.execute(() -> {
                    Component translated = Component.translatable(errorMessage.key(), errorMessage.args());
                    statusMessage = Component.translatable("aerosync.status.error", translated);
                    isConnecting = false;
                    AeroToastUtils.showErrorToast(
                            "aerosync.toast.connection_failure",
                            "aerosync.toast.technical_detail",
                            translated
                    );
                });
            }

            @Override
            public void onSuccess(Path extractedGameDir) {
                client.execute(() -> {
                    statusMessage = Component.translatable("aerosync.status.received");
                    progressPercentage = 1.0f;
                    isConnecting = false;
                    AeroToastUtils.showToast("aerosync.toast.sync_complete", "aerosync.toast.sync_complete_description");
                });
            }
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int startY = 45;
        graphics.centeredText(this.font, this.title, centerX, 15, 0xFFFFFF);
        graphics.centeredText(this.font, Component.translatable("aerosync.label.receiver_prompt"), centerX, startY, 0xA0A0A0);
        graphics.centeredText(this.font, statusMessage, centerX, startY + 125, 0xFFFF55);

        int barWidth = 200;
        int barY = startY + 145;
        int barX = centerX - barWidth / 2;
        graphics.fill(barX, barY, barX + barWidth, barY + 12, 0xFF555555);
        graphics.fill(barX, barY, barX + (int) (barWidth * progressPercentage), barY + 12, 0xFF00AA00);
        graphics.centeredText(this.font, Component.literal((int) (progressPercentage * 100) + "%"), centerX, barY + 2, 0xFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            AeroClientUi.setScreen(this.minecraft, this.parent);
        }
    }
}
