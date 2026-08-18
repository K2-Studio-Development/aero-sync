package com.aerosync.gui;

import com.aerosync.model.SyncOptions;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class AeroSyncMainScreen extends Screen {
    private final Screen parent;
    private Button selectWorldButton;
    private String selectedWorldFolder;
    private String selectedWorldDisplayName;
    private boolean syncWorld;
    private boolean syncConfig;
    private boolean syncShaderpacks;
    private boolean syncMods;
    private boolean syncResourcepacks;
    private boolean syncScreenshots;
    private boolean syncOptions;

    public AeroSyncMainScreen(Screen parent) {
        super(Component.translatable("aerosync.title.main"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = 22;
        int checkboxX = centerX - 100;
        selectWorldButton = Button.builder(worldButtonText(), button -> {
            if (syncWorld) minecraft.setScreen(new AeroWorldSelectScreen(this, selectedWorldFolder));
        }).bounds(centerX - 100, startY + 15, 200, 20).build();
        selectWorldButton.active = syncWorld;
        addRenderableWidget(selectWorldButton);

        addRenderableWidget(checkbox("aerosync.option.world", checkboxX, startY + 43, syncWorld, value -> {
            syncWorld = value;
            selectWorldButton.active = value;
        }));
        addRenderableWidget(checkbox("aerosync.option.config", checkboxX, startY + 65, syncConfig, value -> syncConfig = value));
        addRenderableWidget(checkbox("aerosync.option.shaderpacks", checkboxX, startY + 87, syncShaderpacks, value -> syncShaderpacks = value));
        addRenderableWidget(checkbox("aerosync.option.mods", checkboxX, startY + 109, syncMods, value -> syncMods = value));
        addRenderableWidget(checkbox("aerosync.option.resourcepacks", checkboxX, startY + 131, syncResourcepacks, value -> syncResourcepacks = value));
        addRenderableWidget(checkbox("aerosync.option.screenshots", checkboxX, startY + 153, syncScreenshots, value -> syncScreenshots = value));
        addRenderableWidget(checkbox("aerosync.option.options", checkboxX, startY + 175, syncOptions, value -> syncOptions = value));

        int buttonY = startY + 205;
        addRenderableWidget(Button.builder(Component.translatable("aerosync.button.send"), button -> startSending())
                .bounds(centerX - 125, buttonY, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("aerosync.button.receive"), button -> minecraft.setScreen(new AeroReceiverScreen(this)))
                .bounds(centerX + 5, buttonY, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("aerosync.button.back"), button -> minecraft.setScreen(parent))
                .bounds(centerX - 50, buttonY + 26, 100, 20).build());
    }

    private Checkbox checkbox(String translationKey, int x, int y, boolean selected, Consumer<Boolean> callback) {
        return Checkbox.builder(Component.translatable(translationKey), font).pos(x, y).selected(selected)
                .onValueChange((checkbox, value) -> callback.accept(value)).build();
    }

    private void startSending() {
        if (!(syncWorld || syncConfig || syncShaderpacks || syncMods || syncResourcepacks || syncScreenshots || syncOptions)) {
            AeroToastUtils.showErrorToast("aerosync.toast.no_selection", "aerosync.toast.no_selection_description");
            return;
        }
        if (syncWorld && (selectedWorldFolder == null || selectedWorldFolder.isBlank())) {
            AeroToastUtils.showErrorToast("aerosync.toast.no_world", "aerosync.toast.no_world_description");
            return;
        }
        SyncOptions options = new SyncOptions(syncWorld, syncConfig, syncShaderpacks, syncMods,
                syncResourcepacks, syncScreenshots, syncOptions);
        minecraft.setScreen(new AeroSenderScreen(this, selectedWorldFolder == null ? "" : selectedWorldFolder, options));
    }

    void selectWorld(String folderName, String displayName) {
        selectedWorldFolder = folderName;
        selectedWorldDisplayName = displayName;
        if (selectWorldButton != null) selectWorldButton.setMessage(worldButtonText());
    }

    private Component worldButtonText() {
        return selectedWorldDisplayName == null || selectedWorldDisplayName.isBlank()
                ? Component.translatable("aerosync.button.select_world")
                : Component.translatable("aerosync.button.selected_world", selectedWorldDisplayName);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
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
