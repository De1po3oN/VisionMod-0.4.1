
package com.visionmod;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

public final class VisionModClient implements ClientModInitializer {

    public static boolean enabled = true;

    public static boolean fullBright = true;
    public static boolean oreHighlight = true;
    public static boolean clearWater = true;
    public static boolean clearLava = true;
    public static boolean playerHearts = true;

    // Главный переключатель фильтрации частиц
    public static boolean particles = false;
    public static boolean cosmetics = true;
    public static boolean showOtherCosmetics = true;

    // Настройки частиц
    public static boolean hideBlockParticles = true;
    public static boolean hideSprintParticles = true;
    public static boolean hideEnderChestParticles = true;
    public static boolean hideAllParticles = false;

    public static boolean isFullBrightEnabled() {
        return enabled && fullBright;
    }

    public static boolean isOreEnabled() {
        return enabled && oreHighlight;
    }

    public static boolean isWaterEnabled() {
        return enabled && clearWater;
    }

    public static boolean isLavaEnabled() {
        return enabled && clearLava;
    }

    public static boolean isHeartsEnabled() {
        return enabled && playerHearts;
    }

    public static boolean isParticlesEnabled() {
        return enabled && particles;
    }

    @Override
    public void onInitializeClient() {

        OreHighlight.init();
        VisionCosmetics.init();

        KeyMapping.Category category =
            KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(
                    "visionmod", "general"
                )
            );

        KeyMapping toggleAll =
            KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                    "key.visionmod.toggle",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_END,
                    category
                )
            );

        KeyMapping openMenu =
            KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                    "key.visionmod.menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    category
                )
            );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (toggleAll.consumeClick()) {
                enabled = !enabled;

                if (client.player != null) {
                    client.player.displayClientMessage(
                        Component.literal(
                            enabled
                                ? "Vision Mod: ON"
                                : "Vision Mod: OFF"
                        ),
                        true
                    );
                }
            }

            while (openMenu.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new VisionScreen());
                }
            }
        });
    }
}
