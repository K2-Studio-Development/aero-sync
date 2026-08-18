package com.aerosync.gui;

import com.aerosync.platform.AeroClientUi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
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
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (child instanceof Button) {
                this.removeWidget(child);
            }
        }
    }

    @Override
    public void updateButtonStatus(LevelSummary summary) {
        if (summary == null) {
            return;
        }
        this.aeroParent.selectWorld(summary.getLevelId(), summary.getLevelName());
        if (this.minecraft != null) {
            AeroClientUi.setScreen(this.minecraft, this.aeroParent);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            AeroClientUi.setScreen(this.minecraft, this.aeroParent);
        }
    }
}
