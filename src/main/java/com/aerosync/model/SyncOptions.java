package com.aerosync.model;

/**
 * Configuration model specifying which folders and files should be included in the P2P synchronization package.
 */
public class SyncOptions {
    private boolean syncWorld = false;
    private boolean syncConfig = false;
    private boolean syncShaderpacks = false;
    private boolean syncMods = false;
    private boolean syncResourcepacks = false;
    private boolean syncScreenshots = false;
    private boolean syncOptionsFile = false;

    public SyncOptions() {}

    public SyncOptions(boolean syncWorld, boolean syncConfig, boolean syncShaderpacks, boolean syncMods, boolean syncResourcepacks, boolean syncScreenshots, boolean syncOptionsFile) {
        this.syncWorld = syncWorld;
        this.syncConfig = syncConfig;
        this.syncShaderpacks = syncShaderpacks;
        this.syncMods = syncMods;
        this.syncResourcepacks = syncResourcepacks;
        this.syncScreenshots = syncScreenshots;
        this.syncOptionsFile = syncOptionsFile;
    }

    public boolean isSyncWorld() {
        return syncWorld;
    }

    public void setSyncWorld(boolean syncWorld) {
        this.syncWorld = syncWorld;
    }

    public boolean isSyncConfig() {
        return syncConfig;
    }

    public void setSyncConfig(boolean syncConfig) {
        this.syncConfig = syncConfig;
    }

    public boolean isSyncShaderpacks() {
        return syncShaderpacks;
    }

    public void setSyncShaderpacks(boolean syncShaderpacks) {
        this.syncShaderpacks = syncShaderpacks;
    }

    public boolean isSyncMods() {
        return syncMods;
    }

    public void setSyncMods(boolean syncMods) {
        this.syncMods = syncMods;
    }

    public boolean isSyncResourcepacks() {
        return syncResourcepacks;
    }

    public void setSyncResourcepacks(boolean syncResourcepacks) {
        this.syncResourcepacks = syncResourcepacks;
    }

    public boolean isSyncScreenshots() {
        return syncScreenshots;
    }

    public void setSyncScreenshots(boolean syncScreenshots) {
        this.syncScreenshots = syncScreenshots;
    }

    public boolean isSyncOptionsFile() {
        return syncOptionsFile;
    }

    public void setSyncOptionsFile(boolean syncOptionsFile) {
        this.syncOptionsFile = syncOptionsFile;
    }
}
