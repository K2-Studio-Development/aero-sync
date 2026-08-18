package com.aerosync.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.world.level.storage.LevelSummary;

import java.util.List;

public final class AeroWorldSelectScreen extends SelectWorldScreen {
    private final AeroSyncMainScreen aeroParent;

    public AeroWorldSelectScreen(AeroSyncMainScreen parent, String initiallySelectedWorld) {
        super(parent);
        this.aeroParent = parent;
    }

    @Override
    protected void init() {
        super.init();
        for (var child : List.copyOf(children())) {
            if (child instanceof Button) removeWidget(child);
        }
    }

    @Override
    public void updateButtonStatus(LevelSummary summary) {
        if (summary == null) return;
        aeroParent.selectWorld(summary.getLevelId(), summary.getLevelName());
        minecraft.setScreen(aeroParent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(aeroParent);
    }
}
