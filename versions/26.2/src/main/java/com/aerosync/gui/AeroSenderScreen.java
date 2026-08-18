package com.aerosync.gui;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.AeroPackageArchiver;
import com.aerosync.model.SyncOptions;
import com.aerosync.network.AeroIrohTransport;
import com.aerosync.network.AeroMessage;
import com.aerosync.network.AeroSender;
import com.aerosync.platform.AeroClientUi;
import com.aerosync.platform.AeroGamePaths;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class AeroSenderScreen extends Screen {

    private final Screen parent;
    private final String worldName;
    private final SyncOptions options;

    private AeroSender sender;
    private Button copyAddressButton;
    private volatile Component phaseMessage = Component.translatable("aerosync.phase.packing");
    private volatile Component statusMessage = Component.translatable("aerosync.status.preparing_file_list");
    private volatile String connectionAddress = "";
    private volatile float progressPercentage;
    private volatile boolean packageReady;
    private volatile boolean readyNotificationShown;
    private boolean preparing = true;
    private volatile boolean cancelled;

    public AeroSenderScreen(Screen parent, String worldName, SyncOptions options) {
        super(Component.translatable("aerosync.title.sender", AeroSyncMod.getVersion()));
        this.parent = parent;
        this.worldName = worldName;
        this.options = options;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.copyAddressButton = Button.builder(Component.translatable("aerosync.button.packing_world"), button -> {
            if (this.minecraft != null && isReadyToCopy()) {
                this.minecraft.keyboardHandler.setClipboard(connectionAddress);
                AeroToastUtils.showToast("aerosync.toast.clipboard", "aerosync.toast.address_copied");
            }
        }).bounds(centerX - 70, centerY + 30, 140, 20).build();
        updateCopyButton();
        this.addRenderableWidget(this.copyAddressButton);

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.cancel_close"), button -> {
            cancelTransfer();
            if (this.minecraft != null) {
                AeroClientUi.setScreen(this.minecraft, parent);
            }
        }).bounds(centerX - 60, centerY + 65, 120, 20).build());

        if (preparing) {
            startPackageAndSender();
        }
    }

    private void startPackageAndSender() {
        preparing = false;
        Path gameDir = AeroGamePaths.gameDirectory();
        Minecraft client = Minecraft.getInstance();
        CompletableFuture<File> packageFuture = AeroPackageArchiver.packAsync(
                gameDir,
                worldName,
                options,
                (currentEntry, processedBytes, totalBytes) -> {
                    if (cancelled) {
                        return;
                    }
                    float progress = totalBytes > 0
                            ? Math.min(1.0f, (float) processedBytes / totalBytes)
                            : 0.0f;
                    Component fileName = shortenPackEntry(currentEntry);
                    client.execute(() -> {
                        if (!cancelled && !packageReady) {
                            phaseMessage = Component.translatable("aerosync.phase.packing");
                            statusMessage = fileName;
                            progressPercentage = progress;
                        }
                    });
                }
        );
        sender = new AeroSender(packageFuture, worldName);

        phaseMessage = Component.translatable("aerosync.phase.packing");
        statusMessage = Component.translatable("aerosync.status.preparing_files_key");
        updateCopyButton();

        packageFuture.whenComplete((zipFile, throwable) -> client.execute(() -> {
            if (cancelled) {
                return;
            }
            if (throwable == null) {
                packageReady = true;
                phaseMessage = Component.translatable("aerosync.phase.packing_complete");
                progressPercentage = 1.0f;
                statusMessage = AeroIrohTransport.isConnectionAddress(connectionAddress)
                        ? Component.translatable("aerosync.status.package_address_ready")
                        : Component.translatable("aerosync.status.package_ready_creating_address");
                updateCopyButton();
                showReadyNotification();
                return;
            }
            cancelTransfer();
            statusMessage = Component.translatable("aerosync.status.package_creation_error", throwable.getMessage());
            AeroToastUtils.showErrorToast(
                    "aerosync.toast.archive_error",
                    "aerosync.toast.technical_detail",
                    throwable.getMessage()
            );
        }));

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
                client.execute(() -> {
                    connectionAddress = connectionCode;
                    if (packageReady) {
                        statusMessage = Component.translatable("aerosync.status.package_address_ready");
                    }
                    updateCopyButton();
                    showReadyNotification();
                });
            }

            @Override
            public void onProgress(long bytesSent, long totalBytes) {
                if (!cancelled && totalBytes > 0) {
                    phaseMessage = Component.translatable("aerosync.phase.transfer");
                    progressPercentage = (float) bytesSent / totalBytes;
                }
            }

            @Override
            public void onStatusChange(AeroMessage status) {
                if (!cancelled) {
                    client.execute(() -> {
                        if (!packageReady && (status.key().equals("aerosync.status.address_ready_waiting")
                                || status.key().equals("aerosync.status.address_ready_packing"))) {
                            return;
                        }
                        if (status.key().equals("aerosync.status.recipient_unpacking")) {
                            phaseMessage = Component.translatable("aerosync.phase.recipient_verification");
                        } else if (status.key().equals("aerosync.status.sending_package")
                                || status.key().equals("aerosync.status.sent_waiting")) {
                            phaseMessage = Component.translatable("aerosync.phase.transfer");
                        }
                        statusMessage = Component.translatable(status.key(), status.args());
                    });
                }
            }

            @Override
            public void onError(AeroMessage errorMessage, Throwable cause) {
                if (!cancelled) {
                    client.execute(() -> {
                        Component translated = Component.translatable(errorMessage.key(), errorMessage.args());
                        statusMessage = Component.translatable("aerosync.status.error", translated);
                        phaseMessage = Component.translatable("aerosync.phase.error");
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
                if (!cancelled) {
                    client.execute(() -> {
                        statusMessage = Component.translatable("aerosync.status.transfer_complete");
                        phaseMessage = Component.translatable("aerosync.phase.done");
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.centeredText(this.font, this.title, centerX, 20, 0xFFFFFF);
        graphics.centeredText(this.font, phaseMessage, centerX, centerY - 68, 0xFFFF55);
        graphics.centeredText(this.font, statusMessage, centerX, centerY - 53, 0xE0E0E0);
        graphics.centeredText(this.font, Component.translatable("aerosync.label.address_for_friend"), centerX, centerY - 35, 0xAAAAAA);

        Component displayedAddress = isReadyToCopy()
                ? Component.literal(shortenCodeForDisplay(connectionAddress))
                : Component.translatable("aerosync.label.address_after_packing");
        graphics.centeredText(this.font, displayedAddress, centerX, centerY - 15, 0x55FF55);

        int barWidth = 200;
        int barY = centerY + 10;
        int barX = centerX - barWidth / 2;
        graphics.fill(barX, barY, barX + barWidth, barY + 12, 0xFF555555);
        graphics.fill(barX, barY, barX + (int) (barWidth * progressPercentage), barY + 12, 0xFF00AA00);
        graphics.centeredText(this.font, Component.literal((int) (progressPercentage * 100) + "%"), centerX, barY + 2, 0xFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private String shortenCodeForDisplay(String code) {
        if (code == null || code.length() <= 48) {
            return code;
        }
        return code.substring(0, 24) + "..." + code.substring(code.length() - 16);
    }

    private Component shortenPackEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return Component.translatable("aerosync.status.preparing");
        }
        if (entry.startsWith("aerosync.")) {
            return Component.translatable(entry);
        }
        String normalized = entry.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (fileName.length() <= 64) {
            return Component.literal(fileName);
        }
        return Component.literal(fileName.substring(0, 30) + "..." + fileName.substring(fileName.length() - 24));
    }

    private boolean isReadyToCopy() {
        return packageReady && AeroIrohTransport.isConnectionAddress(connectionAddress);
    }

    private void updateCopyButton() {
        if (copyAddressButton == null) {
            return;
        }
        copyAddressButton.active = isReadyToCopy();
        copyAddressButton.setMessage(Component.translatable(
                copyAddressButton.active
                        ? "aerosync.button.copy_address"
                        : packageReady ? "aerosync.button.creating_address" : "aerosync.button.packing_world"
        ));
    }

    private void showReadyNotification() {
        if (isReadyToCopy() && !readyNotificationShown) {
            readyNotificationShown = true;
            AeroToastUtils.showToast("aerosync.name", "aerosync.toast.ready_description");
        }
    }

    @Override
    public void onClose() {
        cancelTransfer();
        if (this.minecraft != null) {
            AeroClientUi.setScreen(this.minecraft, this.parent);
        }
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
