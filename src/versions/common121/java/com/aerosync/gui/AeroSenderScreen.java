package com.aerosync.gui;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.AeroPackageArchiver;
import com.aerosync.model.SyncOptions;
import com.aerosync.network.AeroIrohTransport;
import com.aerosync.network.AeroMessage;
import com.aerosync.network.AeroSender;
import com.aerosync.util.AeroToastUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AeroSenderScreen extends Screen {

    private final Screen parent;
    private final String worldName;
    private final SyncOptions options;

    private AeroSender sender;
    private ButtonWidget copyAddressButton;
    private volatile Text phaseMessage = Text.translatable("aerosync.phase.packing");
    private volatile Text statusMessage = Text.translatable("aerosync.status.preparing_file_list");
    private volatile String connectionAddress = "";
    private volatile float progressPercentage = 0.0f;
    private volatile boolean packageReady = false;
    private volatile boolean readyNotificationShown = false;
    private boolean isPreparing = true;
    private volatile boolean cancelled = false;

    public AeroSenderScreen(Screen parent, String worldName, SyncOptions options) {
        super(Text.translatable("aerosync.title.sender", AeroSyncMod.getVersion()));
        this.parent = parent;
        this.worldName = worldName;
        this.options = options;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.copyAddressButton = ButtonWidget.builder(Text.translatable("aerosync.button.packing_world"), button -> {
            if (this.client != null && isReadyToCopy()) {
                this.client.keyboard.setClipboard(connectionAddress);
                AeroToastUtils.showToast("aerosync.toast.clipboard", "aerosync.toast.address_copied");
            }
        }).dimensions(centerX - 70, centerY + 30, 140, 20).build();
        updateCopyButton();
        this.addDrawableChild(this.copyAddressButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.cancel_close"), button -> {
            cancelTransfer();
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }).dimensions(centerX - 60, centerY + 65, 120, 20).build());

        if (isPreparing) {
            startPackageAndSender();
        }
    }

    private void startPackageAndSender() {
        isPreparing = false;
        Path gameDir = FabricLoader.getInstance().getGameDir();
        MinecraftClient client = MinecraftClient.getInstance();
        CompletableFuture<File> packageFuture = AeroPackageArchiver.packAsync(
                gameDir,
                worldName,
                options,
                (currentEntry, processedBytes, totalBytes) -> {
                    if (cancelled || client == null) {
                        return;
                    }
                    float progress = totalBytes > 0
                            ? Math.min(1.0f, (float) processedBytes / totalBytes)
                            : 0.0f;
                    Text fileName = shortenPackEntry(currentEntry);
                    client.execute(() -> {
                        if (!cancelled && !packageReady) {
                            phaseMessage = Text.translatable("aerosync.phase.packing");
                            statusMessage = fileName;
                            progressPercentage = progress;
                        }
                    });
                }
        );
        sender = new AeroSender(packageFuture, worldName);

        phaseMessage = Text.translatable("aerosync.phase.packing");
        statusMessage = Text.translatable("aerosync.status.preparing_files_key");
        updateCopyButton();

        packageFuture.whenComplete((zipFile, throwable) -> {
            if (cancelled) {
                return;
            }
            if (client != null) {
                client.execute(() -> {
                    if (throwable == null) {
                        packageReady = true;
                        phaseMessage = Text.translatable("aerosync.phase.packing_complete");
                        progressPercentage = 1.0f;
                        statusMessage = AeroIrohTransport.isConnectionAddress(connectionAddress)
                                ? Text.translatable("aerosync.status.package_address_ready")
                                : Text.translatable("aerosync.status.package_ready_creating_address");
                        updateCopyButton();
                        showReadyNotification();
                        return;
                    }
                    cancelTransfer();
                    statusMessage = Text.translatable("aerosync.status.package_creation_error", throwable.getMessage());
                    AeroToastUtils.showErrorToast(
                            "aerosync.toast.archive_error",
                            "aerosync.toast.technical_detail",
                            throwable.getMessage()
                    );
                });
            }
        });

        sender.startAsync(new AeroSender.ProgressListener() {
            @Override
            public void onConnectionCode(String connectionCode) {
                if (cancelled) {
                    sender.stop();
                    return;
                }
                if (!AeroIrohTransport.isConnectionAddress(connectionCode)) {
                    onError(AeroMessage.of("aerosync.error.incomplete_address"), null);
                    return;
                }
                if (client != null) {
                    client.execute(() -> {
                        connectionAddress = connectionCode;
                        if (packageReady) {
                            statusMessage = Text.translatable("aerosync.status.package_address_ready");
                        }
                        updateCopyButton();
                        showReadyNotification();
                    });
                }
            }

            @Override
            public void onProgress(long bytesSent, long totalBytes) {
                if (!cancelled && totalBytes > 0) {
                    phaseMessage = Text.translatable("aerosync.phase.transfer");
                    progressPercentage = (float) bytesSent / totalBytes;
                }
            }

            @Override
            public void onStatusChange(AeroMessage status) {
                if (!cancelled && client != null) {
                    client.execute(() -> {
                        if (!packageReady && (status.key().equals("aerosync.status.address_ready_waiting")
                                || status.key().equals("aerosync.status.address_ready_packing"))) {
                            return;
                        }
                        if (status.key().equals("aerosync.status.recipient_unpacking")) {
                            phaseMessage = Text.translatable("aerosync.phase.recipient_verification");
                        } else if (status.key().equals("aerosync.status.sending_package")
                                || status.key().equals("aerosync.status.sent_waiting")) {
                            phaseMessage = Text.translatable("aerosync.phase.transfer");
                        }
                        statusMessage = Text.translatable(status.key(), status.args());
                    });
                }
            }

            @Override
            public void onError(AeroMessage errorMessage, Throwable cause) {
                if (!cancelled && client != null) {
                    client.execute(() -> {
                        Text translated = Text.translatable(errorMessage.key(), errorMessage.args());
                        statusMessage = Text.translatable("aerosync.status.error", translated);
                        phaseMessage = Text.translatable("aerosync.phase.error");
                        AeroToastUtils.showErrorToast(
                                "aerosync.toast.p2p_error",
                                "aerosync.toast.technical_detail",
                                translated
                        );
                    });
                }
            }

            @Override
            public void onCompleted() {
                if (!cancelled && client != null) {
                    client.execute(() -> {
                        statusMessage = Text.translatable("aerosync.status.transfer_complete");
                        phaseMessage = Text.translatable("aerosync.phase.done");
                        progressPercentage = 1.0f;
                        AeroToastUtils.showToast(
                                "aerosync.toast.transfer_complete",
                                "aerosync.toast.transfer_complete_description"
                        );
                    });
                }
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        drawCenteredText(context, this.title, centerX, 20, 0xFFFFFF);
        drawCenteredText(context, phaseMessage, centerX, centerY - 68, 0xFFFF55);
        drawCenteredText(context, statusMessage, centerX, centerY - 53, 0xE0E0E0);
        drawCenteredText(context, Text.translatable("aerosync.label.address_for_friend"), centerX, centerY - 35, 0xAAAAAA);
        Text displayedAddress = isReadyToCopy()
                ? Text.literal(shortenCodeForDisplay(connectionAddress))
                : Text.translatable("aerosync.label.address_after_packing");
        drawCenteredText(context, displayedAddress, centerX, centerY - 15, 0x55FF55);

        int barWidth = 200;
        int barHeight = 12;
        int barX = centerX - (barWidth / 2);
        int barY = centerY + 10;

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

    private String shortenCodeForDisplay(String code) {
        if (code == null || code.length() <= 48) {
            return code;
        }
        return code.substring(0, 24) + "..." + code.substring(code.length() - 16);
    }

    private Text shortenPackEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return Text.translatable("aerosync.status.preparing");
        }
        if (entry.startsWith("aerosync.")) {
            return Text.translatable(entry);
        }
        String normalized = entry.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (fileName.length() <= 64) {
            return Text.literal(fileName);
        }
        return Text.literal(fileName.substring(0, 30) + "..." + fileName.substring(fileName.length() - 24));
    }

    private boolean isReadyToCopy() {
        return packageReady && AeroIrohTransport.isConnectionAddress(connectionAddress);
    }

    private void updateCopyButton() {
        if (copyAddressButton == null) {
            return;
        }
        copyAddressButton.active = isReadyToCopy();
        if (copyAddressButton.active) {
            copyAddressButton.setMessage(Text.translatable("aerosync.button.copy_address"));
        } else if (packageReady) {
            copyAddressButton.setMessage(Text.translatable("aerosync.button.creating_address"));
        } else {
            copyAddressButton.setMessage(Text.translatable("aerosync.button.packing_world"));
        }
    }

    private void showReadyNotification() {
        if (isReadyToCopy() && !readyNotificationShown) {
            readyNotificationShown = true;
            AeroToastUtils.showToast("aerosync.name", "aerosync.toast.ready_description");
        }
    }

    @Override
    public void close() {
        cancelTransfer();
        super.close();
    }

    @Override
    public void removed() {
        cancelTransfer();
        super.removed();
    }

    private void cancelTransfer() {
        cancelled = true;
        if (sender != null) {
            sender.stop();
        }
    }
}
