package com.aerosync.gui;

import com.aerosync.network.AeroReceiver;
import com.aerosync.network.AeroMessage;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;

public class AeroReceiverScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget addressField;

    private volatile Text statusMessage = Text.translatable("aerosync.status.input_address");
    private volatile float progressPercentage = 0.0f;
    private boolean isConnecting = false;

    public AeroReceiverScreen(Screen parent) {
        super(Text.translatable("aerosync.title.receiver"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 45;

        this.addressField = new TextFieldWidget(this.textRenderer, centerX - 140, startY + 20, 280, 20, Text.translatable("aerosync.label.address_field"));
        this.addressField.setMaxLength(4096);
        this.addressField.setPlaceholder(Text.literal("AEROSYNC:..."));
        this.addDrawableChild(this.addressField);

        // Connect Button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.connect_download"), button -> {
            if (!isConnecting) {
                startReceiving();
            }
        }).dimensions(centerX - 90, startY + 60, 180, 20).build());

        // Cancel Button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.back"), button -> {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(centerX - 50, startY + 90, 100, 20).build());
    }

    private void startReceiving() {
        String connectionAddress = addressField.getText().trim();
        if (!com.aerosync.network.AeroIrohTransport.isConnectionAddress(connectionAddress)) {
            statusMessage = Text.translatable("aerosync.status.invalid_address");
            AeroToastUtils.showErrorToast("aerosync.toast.input_error", "aerosync.toast.input_error_description");
            return;
        }

        isConnecting = true;
        MinecraftClient client = MinecraftClient.getInstance();

        AeroReceiver.receivePackageAsync(connectionAddress, new AeroReceiver.ReceiverListener() {
            @Override
            public void onProgress(long bytesReceived, long totalBytes) {
                if (totalBytes > 0) {
                    progressPercentage = (float) bytesReceived / totalBytes;
                }
            }

            @Override
            public void onStatusChange(AeroMessage statusMessageText) {
                Text translated = Text.translatable(statusMessageText.key(), statusMessageText.args());
                if (client != null) {
                    client.execute(() -> statusMessage = translated);
                } else {
                    statusMessage = translated;
                }
            }

            @Override
            public void onError(AeroMessage errorMessage, Throwable cause) {
                if (client != null) {
                    client.execute(() -> {
                        Text translated = Text.translatable(errorMessage.key(), errorMessage.args());
                        statusMessage = Text.translatable("aerosync.status.error", translated);
                        isConnecting = false;
                        AeroToastUtils.showErrorToast(
                                "aerosync.toast.connection_failure",
                                "aerosync.toast.technical_detail",
                                translated
                        );
                    });
                }
            }

            @Override
            public void onSuccess(Path extractedGameDir) {
                if (client != null) {
                    client.execute(() -> {
                        statusMessage = Text.translatable("aerosync.status.received");
                        progressPercentage = 1.0f;
                        isConnecting = false;
                        AeroToastUtils.showToast("aerosync.toast.sync_complete", "aerosync.toast.sync_complete_description");
                    });
                }
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int startY = 45;

        drawCenteredText(context, this.title, centerX, 15, 0xFFFFFF);
        drawCenteredText(context, Text.translatable("aerosync.label.receiver_prompt"), centerX, startY, 0xA0A0A0);

        drawCenteredText(context, statusMessage, centerX, startY + 125, 0xFFFF55);

        int barWidth = 200;
        int barHeight = 12;
        int barX = centerX - (barWidth / 2);
        int barY = startY + 145;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF555555);
        context.fill(barX, barY, barX + (int) (barWidth * progressPercentage), barY + barHeight, 0xFF00AA00);
        drawCenteredText(context, Text.literal((int)(progressPercentage * 100) + "%"), centerX, barY + 2, 0xFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        int textWidth = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, centerX - textWidth / 2, y, color, true);
    }
}
