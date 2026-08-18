package com.aerosync.gui;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.world.level.storage.LevelSummary;

import java.util.List;

public class AeroWorldSelectScreen extends SelectWorldScreen {

    private final AeroSyncMainScreen aeroParent;

    public AeroWorldSelectScreen(AeroSyncMainScreen parent, String initiallySelectedWorld) {
        super(parent);
        this.aeroParent = parent;
    }

    @Override
    protected void init() {
        super.init();
        for (Element element : List.copyOf(this.children())) {
            if (element instanceof ButtonWidget) {
                this.remove(element);
            }
        }
    }

    @Override
    public void worldSelected(LevelSummary summary) {
        if (summary == null) {
            return;
        }
        this.aeroParent.selectWorld(summary.getName(), summary.getDisplayName());
        if (this.client != null) {
            this.client.setScreen(this.aeroParent);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.aeroParent);
        }
    }
}

