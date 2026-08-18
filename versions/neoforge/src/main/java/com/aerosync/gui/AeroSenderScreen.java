package com.aerosync.gui;

import com.aerosync.AeroSyncMod;
import com.aerosync.archiver.AeroPackageArchiver;
import com.aerosync.model.SyncOptions;
import com.aerosync.network.AeroIrohTransport;
import com.aerosync.network.AeroMessage;
import com.aerosync.network.AeroSender;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class AeroSenderScreen extends Screen {
    private final Screen parent;
    private final String worldName;
    private final SyncOptions options;
    private AeroSender sender;
    private Button copyButton;
    private volatile Component phase = Component.translatable("aerosync.phase.packing");
    private volatile Component status = Component.translatable("aerosync.status.preparing_file_list");
    private volatile String address = "";
    private volatile float progress;
    private volatile boolean packageReady;
    private volatile boolean notified;
    private volatile boolean cancelled;
    private boolean started;

    public AeroSenderScreen(Screen parent, String worldName, SyncOptions options) {
        super(Component.translatable("aerosync.title.sender", AeroSyncMod.getVersion()));
        this.parent = parent;
        this.worldName = worldName;
        this.options = options;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        copyButton = Button.builder(Component.translatable("aerosync.button.packing_world"), button -> {
            if (ready()) {
                minecraft.keyboardHandler.setClipboard(address);
                AeroToastUtils.showToast("aerosync.toast.clipboard", "aerosync.toast.address_copied");
            }
        }).bounds(centerX - 70, centerY + 30, 140, 20).build();
        updateCopyButton();
        addRenderableWidget(copyButton);
        addRenderableWidget(Button.builder(Component.translatable("aerosync.button.cancel_close"), button -> {
            cancel();
            minecraft.setScreen(parent);
        }).bounds(centerX - 60, centerY + 65, 120, 20).build());
        if (!started) start();
    }

    private void start() {
        started = true;
        Minecraft client = Minecraft.getInstance();
        Path gameDir = FMLPaths.GAMEDIR.get();
        CompletableFuture<File> packageFuture = AeroPackageArchiver.packAsync(gameDir, worldName, options,
                (entry, processed, total) -> {
                    if (cancelled) return;
                    float value = total > 0 ? Math.min(1.0f, (float) processed / total) : 0.0f;
                    Component file = shortEntry(entry);
                    client.execute(() -> {
                        if (!cancelled && !packageReady) {
                            phase = Component.translatable("aerosync.phase.packing");
                            status = file;
                            progress = value;
                        }
                    });
                });
        sender = new AeroSender(packageFuture, worldName);
        sender.startAsync(new AeroSender.ProgressListener() {
            @Override public void onConnectionCode(String code) {
                client.execute(() -> {
                    if (!cancelled && AeroIrohTransport.isConnectionAddress(code)) {
                        address = code;
                        if (packageReady) status = Component.translatable("aerosync.status.package_address_ready");
                        updateCopyButton();
                        notifyReady();
                    }
                });
            }
            @Override public void onProgress(long sent, long total) {
                if (!cancelled && total > 0) {
                    phase = Component.translatable("aerosync.phase.transfer");
                    progress = Math.min(1.0f, (float) sent / total);
                }
            }
            @Override public void onStatusChange(AeroMessage value) {
                client.execute(() -> {
                    if (!cancelled) {
                        if (value.key().equals("aerosync.status.recipient_unpacking")) {
                            phase = Component.translatable("aerosync.phase.recipient_verification");
                        } else if (value.key().equals("aerosync.status.sending_package")
                                || value.key().equals("aerosync.status.sent_waiting")) {
                            phase = Component.translatable("aerosync.phase.transfer");
                        }
                        status = Component.translatable(value.key(), value.args());
                    }
                });
            }
            @Override public void onError(AeroMessage message, Throwable cause) {
                client.execute(() -> {
                    if (!cancelled) {
                        Component translated = Component.translatable(message.key(), message.args());
                        phase = Component.translatable("aerosync.phase.error");
                        status = Component.translatable("aerosync.status.error", translated);
                        AeroToastUtils.showErrorToast(
                                "aerosync.toast.p2p_error",
                                "aerosync.toast.technical_detail",
                                translated
                        );
                    }
                });
            }
            @Override public void onCompleted() {
                client.execute(() -> {
                    if (!cancelled) {
                        phase = Component.translatable("aerosync.phase.done");
                        status = Component.translatable("aerosync.status.transfer_complete");
                        progress = 1.0f;
                        AeroToastUtils.showToast(
                                "aerosync.toast.transfer_complete",
                                "aerosync.toast.transfer_complete_description"
                        );
                    }
                });
            }
        });
        packageFuture.whenComplete((file, error) -> client.execute(() -> {
            if (cancelled) return;
            if (error != null) {
                cancel();
                phase = Component.translatable("aerosync.phase.error");
                status = Component.translatable("aerosync.status.package_creation_error", error.getMessage());
                AeroToastUtils.showErrorToast(
                        "aerosync.toast.archive_error",
                        "aerosync.toast.technical_detail",
                        error.getMessage()
                );
                return;
            }
            packageReady = true;
            phase = Component.translatable("aerosync.phase.packing_complete");
            progress = 1.0f;
            status = AeroIrohTransport.isConnectionAddress(address)
                    ? Component.translatable("aerosync.status.package_address_ready")
                    : Component.translatable("aerosync.status.package_ready_creating_address");
            updateCopyButton();
            notifyReady();
        }));
    }

    private boolean ready() {
        return packageReady && AeroIrohTransport.isConnectionAddress(address);
    }

    private void updateCopyButton() {
        if (copyButton == null) return;
        copyButton.active = ready();
        copyButton.setMessage(Component.translatable(
                ready() ? "aerosync.button.copy_address"
                        : packageReady ? "aerosync.button.creating_address" : "aerosync.button.packing_world"
        ));
    }

    private void notifyReady() {
        if (ready() && !notified) {
            notified = true;
            AeroToastUtils.showToast("aerosync.name", "aerosync.toast.ready_description");
        }
    }

    private Component shortEntry(String entry) {
        if (entry == null || entry.isBlank()) return Component.translatable("aerosync.status.preparing");
        if (entry.startsWith("aerosync.")) return Component.translatable(entry);
        String normalized = entry.replace('\\', '/');
        String file = normalized.substring(normalized.lastIndexOf('/') + 1);
        return Component.literal(file.length() <= 64
                ? file
                : file.substring(0, 30) + "..." + file.substring(file.length() - 24));
    }

    private Component displayAddress() {
        if (!ready()) return Component.translatable("aerosync.label.address_after_packing");
        return Component.literal(address.length() <= 48
                ? address
                : address.substring(0, 24) + "..." + address.substring(address.length() - 16));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = width / 2;
        int centerY = height / 2;
        graphics.drawCenteredString(font, title, centerX, 20, 0xFFFFFF);
        graphics.drawCenteredString(font, phase, centerX, centerY - 68, 0xFFFF55);
        graphics.drawCenteredString(font, status, centerX, centerY - 53, 0xE0E0E0);
        graphics.drawCenteredString(font, Component.translatable("aerosync.label.address_for_friend"), centerX, centerY - 35, 0xAAAAAA);
        graphics.drawCenteredString(font, displayAddress().copy().withStyle(style -> style.withUnderlined(true).withBold(true)), centerX, centerY - 15, 0x55FF55);
        int barX = centerX - 100;
        int barY = centerY + 10;
        graphics.fill(barX, barY, barX + 200, barY + 12, 0xFF555555);
        graphics.fill(barX, barY, barX + (int) (200 * progress), barY + 12, 0xFF00AA00);
        graphics.drawCenteredString(font, (int) (progress * 100) + "%", centerX, barY + 2, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC0101010);
    }

    @Override public void onClose() { cancel(); minecraft.setScreen(parent); }
    @Override public void removed() { cancel(); super.removed(); }

    private void cancel() {
        cancelled = true;
        if (sender != null) sender.stop();
    }
}
