package com.visionmod;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Optional render injections must never be forced onto incompatible
 * player-animation renderers. Vision Mod does not depend on those mods.
 */
public final class VisionMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Emergency diagnostic switch. Does not affect the GUI and ore colors.
        if (Boolean.getBoolean("visionmod.disableMixins")) {
            return false;
        }

        // Do NOT disable PlayerHeartsMixin just because Emotecraft or
        // Player Animation Library is installed. The heart rendering hook
        // does not modify animation data and can coexist with their hooks.
        // Individual injections are optional (require = 0).

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) { }
}
