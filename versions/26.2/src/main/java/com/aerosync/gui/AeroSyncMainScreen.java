package com.aerosync.gui;

import com.aerosync.model.SyncOptions;
import com.aerosync.platform.AeroClientUi;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AeroSyncMainScreen extends Screen {

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
        int centerX = this.width / 2;
        int startY = 22;
        int checkboxX = centerX - 100;

        this.selectWorldButton = Button.builder(getWorldButtonText(), button -> {
            if (this.minecraft != null && syncWorld) {
                AeroClientUi.setScreen(this.minecraft, new AeroWorldSelectScreen(this, selectedWorldFolder));
            }
        }).bounds(centerX - 100, startY + 15, 200, 20).build();
        this.selectWorldButton.active = syncWorld;
        this.addRenderableWidget(this.selectWorldButton);

        this.addRenderableWidget(Checkbox.builder(Component.translatable("aerosync.option.world"), this.font)
                .pos(checkboxX, startY + 43)
                .selected(syncWorld)
                .onValueChange((checkbox, checked) -> {
                    syncWorld = checked;
                    selectWorldButton.active = checked;
                })
                .build());

        this.addRenderableWidget(createCheckbox("aerosync.option.config", checkboxX, startY + 65, syncConfig, value -> syncConfig = value));
        this.addRenderableWidget(createCheckbox("aerosync.option.shaderpacks", checkboxX, startY + 87, syncShaderpacks, value -> syncShaderpacks = value));
        this.addRenderableWidget(createCheckbox("aerosync.option.mods", checkboxX, startY + 109, syncMods, value -> syncMods = value));
        this.addRenderableWidget(createCheckbox("aerosync.option.resourcepacks", checkboxX, startY + 131, syncResourcepacks, value -> syncResourcepacks = value));
        this.addRenderableWidget(createCheckbox("aerosync.option.screenshots", checkboxX, startY + 153, syncScreenshots, value -> syncScreenshots = value));
        this.addRenderableWidget(createCheckbox("aerosync.option.options", checkboxX, startY + 175, syncOptions, value -> syncOptions = value));

        int buttonY = startY + 205;
        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.send"), button -> startSending())
                .bounds(centerX - 125, buttonY, 120, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.receive"), button -> {
            if (this.minecraft != null) {
                AeroClientUi.setScreen(this.minecraft, new AeroReceiverScreen(this));
            }
        }).bounds(centerX + 5, buttonY, 120, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("aerosync.button.back"), button -> {
            if (this.minecraft != null) {
                AeroClientUi.setScreen(this.minecraft, this.parent);
            }
        }).bounds(centerX - 50, buttonY + 26, 100, 20).build());
    }

    private Checkbox createCheckbox(String translationKey, int x, int y, boolean checked, java.util.function.Consumer<Boolean> callback) {
        return Checkbox.builder(Component.translatable(translationKey), this.font)
                .pos(x, y)
                .selected(checked)
                .onValueChange((checkbox, value) -> callback.accept(value))
                .build();
    }

    private void startSending() {
        if (!hasSelectedCategory()) {
            AeroToastUtils.showErrorToast("aerosync.toast.no_selection", "aerosync.toast.no_selection_description");
            return;
        }
        if (syncWorld && (selectedWorldFolder == null || selectedWorldFolder.isBlank())) {
            AeroToastUtils.showErrorToast("aerosync.toast.no_world", "aerosync.toast.no_world_description");
            return;
        }

        SyncOptions options = new SyncOptions(
                syncWorld,
                syncConfig,
                syncShaderpacks,
                syncMods,
                syncResourcepacks,
                syncScreenshots,
                syncOptions
        );
        if (this.minecraft != null) {
            AeroClientUi.setScreen(this.minecraft, new AeroSenderScreen(
                    this,
                    selectedWorldFolder == null ? "" : selectedWorldFolder,
                    options
            ));
        }
    }

    private boolean hasSelectedCategory() {
        return syncWorld || syncConfig || syncShaderpacks || syncMods
                || syncResourcepacks || syncScreenshots || syncOptions;
    }

    void selectWorld(String folderName, String displayName) {
        this.selectedWorldFolder = folderName;
        this.selectedWorldDisplayName = displayName;
        if (this.selectWorldButton != null) {
            this.selectWorldButton.setMessage(getWorldButtonText());
        }
    }

    private Component getWorldButtonText() {
        return selectedWorldDisplayName == null || selectedWorldDisplayName.isBlank()
                ? Component.translatable("aerosync.button.select_world")
                : Component.translatable("aerosync.button.selected_world", selectedWorldDisplayName);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
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
