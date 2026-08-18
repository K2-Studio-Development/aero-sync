package com.aerosync.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelSummary;

public class AeroWorldSelectScreen extends Screen {

    private final AeroSyncMainScreen parent;
    private final String initiallySelectedWorld;
    private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 61, 0);

    private TextFieldWidget searchBox;
    private WorldListWidget worldList;

    public AeroWorldSelectScreen(AeroSyncMainScreen parent, String initiallySelectedWorld) {
        super(Text.translatable("aerosync.title.world_select"));
        this.parent = parent;
        this.initiallySelectedWorld = initiallySelectedWorld;
    }

    @Override
    protected void init() {
        DirectionalLayoutWidget header = this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
        header.getMainPositioner().alignHorizontalCenter();
        header.add(new TextWidget(this.title, this.textRenderer));

        this.searchBox = header.add(new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - 100,
                22,
                200,
                20,
                this.searchBox,
                Text.translatable("selectWorld.search")
        ));
        this.searchBox.setPlaceholder(Text.translatable("gui.selectWorld.search").setStyle(TextFieldWidget.SEARCH_STYLE));
        this.searchBox.setChangedListener(search -> {
            if (this.worldList != null) {
                this.worldList.setSearch(search);
            }
        });

        this.worldList = this.layout.addBody(new WorldListWidget.Builder(this.client, this)
                .width(this.width)
                .height(this.layout.getContentHeight())
                .search(this.searchBox.getText())
                .predecessor(this.worldList)
                .selectionCallback(this::selectWorld)
                .confirmationCallback(entry -> selectWorld(entry.getLevel()))
                .toWidget());

        this.layout.forEachElement(widget -> {
            if (widget instanceof ClickableWidget clickableWidget) {
                this.addDrawableChild(clickableWidget);
            }
        });
        this.refreshWidgetPositions();
        this.setInitialFocus(this.searchBox);
    }

    private void selectWorld(LevelSummary summary) {
        if (summary == null) {
            return;
        }
        this.parent.selectWorld(summary.getName(), summary.getDisplayName());
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    protected void refreshWidgetPositions() {
        if (this.worldList != null) {
            this.worldList.position(this.width, this.layout);
        }
        this.layout.refreshPositions();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        if (this.worldList != null) {
            this.worldList.children().forEach(WorldListWidget.Entry::close);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0101010);
    }
}
