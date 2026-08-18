package com.aerosync.gui;

import com.aerosync.model.SyncOptions;
import com.aerosync.util.AeroToastUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

public class AeroSyncMainScreen extends Screen {

    private final Screen parent;

    private ButtonWidget selectWorldButton;
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
        super(Text.translatable("aerosync.title.main"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 22;
        int checkboxX = centerX - 100;

        this.selectWorldButton = ButtonWidget.builder(getWorldButtonText(), button -> {
            if (this.client != null && syncWorld) {
                this.client.setScreen(new AeroWorldSelectScreen(this, selectedWorldFolder));
            }
        }).dimensions(centerX - 100, startY + 15, 200, 20).build();
        this.selectWorldButton.active = syncWorld;
        this.addDrawableChild(this.selectWorldButton);

        CheckboxWidget syncWorldCheckbox = CheckboxWidget.builder(Text.translatable("aerosync.option.world"), this.textRenderer)
                .pos(checkboxX, startY + 43)
                .checked(syncWorld)
                .callback((checkbox, checked) -> {
                    syncWorld = checked;
                    selectWorldButton.active = checked;
                })
                .build();
        this.addDrawableChild(syncWorldCheckbox);

        this.addDrawableChild(createCheckbox("aerosync.option.config", checkboxX, startY + 65, syncConfig, value -> syncConfig = value));
        this.addDrawableChild(createCheckbox("aerosync.option.shaderpacks", checkboxX, startY + 87, syncShaderpacks, value -> syncShaderpacks = value));
        this.addDrawableChild(createCheckbox("aerosync.option.mods", checkboxX, startY + 109, syncMods, value -> syncMods = value));
        this.addDrawableChild(createCheckbox("aerosync.option.resourcepacks", checkboxX, startY + 131, syncResourcepacks, value -> syncResourcepacks = value));
        this.addDrawableChild(createCheckbox("aerosync.option.screenshots", checkboxX, startY + 153, syncScreenshots, value -> syncScreenshots = value));
        this.addDrawableChild(createCheckbox("aerosync.option.options", checkboxX, startY + 175, syncOptions, value -> syncOptions = value));

        int buttonY = startY + 205;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.send"), button -> startSending())
                .dimensions(centerX - 125, buttonY, 120, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.receive"), button -> {
            if (this.client != null) {
                this.client.setScreen(new AeroReceiverScreen(this));
            }
        }).dimensions(centerX + 5, buttonY, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("aerosync.button.back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(centerX - 50, buttonY + 26, 100, 20).build());
    }

    private CheckboxWidget createCheckbox(
            String translationKey,
            int x,
            int y,
            boolean checked,
            java.util.function.Consumer<Boolean> callback
    ) {
        return CheckboxWidget.builder(Text.translatable(translationKey), this.textRenderer)
                .pos(x, y)
                .checked(checked)
                .callback((checkbox, value) -> callback.accept(value))
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
        if (this.client != null) {
            String worldFolder = selectedWorldFolder == null ? "" : selectedWorldFolder;
            this.client.setScreen(new AeroSenderScreen(this, worldFolder, options));
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

    private Text getWorldButtonText() {
        if (selectedWorldDisplayName == null || selectedWorldDisplayName.isBlank()) {
            return Text.translatable("aerosync.button.select_world");
        }
        return Text.translatable("aerosync.button.selected_world", selectedWorldDisplayName);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.title, this.width / 2, 8, 0xFFFFFF);
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
