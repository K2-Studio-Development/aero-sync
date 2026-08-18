package com.aerosync.universal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class AeroSyncVersionMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> PACKAGES = Map.ofEntries(
            Map.entry("1.21.1", "v01.aerosync.mixin."),
            Map.entry("1.21.2", "v02.aerosync.mixin."),
            Map.entry("1.21.3", "v03.aerosync.mixin."),
            Map.entry("1.21.4", "v04.aerosync.mixin."),
            Map.entry("1.21.5", "v05.aerosync.mixin."),
            Map.entry("1.21.6", "v06.aerosync.mixin."),
            Map.entry("1.21.7", "v07.aerosync.mixin."),
            Map.entry("1.21.8", "v08.aerosync.mixin."),
            Map.entry("1.21.9", "v09.aerosync.mixin."),
            Map.entry("1.21.10", "v10.aerosync.mixin."),
            Map.entry("1.21.11", "v11.aerosync.mixin."),
            Map.entry("26.1", "v12.aerosync.mixin."),
            Map.entry("26.2", "v13.aerosync.mixin.")
    );

    private String activePackage;

    @Override
    public void onLoad(String mixinPackage) {
        activePackage = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> PACKAGES.get(container.getMetadata().getVersion().getFriendlyString()))
                .orElse(null);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return activePackage != null && mixinClassName.startsWith(activePackage);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
