
package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ParticleSettingsScreen extends Screen {

    public ParticleSettingsScreen() {
        super(Component.literal("Particle Settings"));
    }

    @Override
    protected void init() {

        int width = 200;
        int height = 20;
        int spacing = 26;

        int x = (this.width - width) / 2;
        int y = Math.max(40, (this.height - 155) / 2);

        this.addRenderableWidget(
            Button.builder(
                label("Block Particles",
                    VisionModClient.hideBlockParticles),
                button -> {
                    VisionModClient.hideBlockParticles =
                        !VisionModClient.hideBlockParticles;

                    button.setMessage(label(
                        "Block Particles",
                        VisionModClient.hideBlockParticles
                    ));
                }
            ).bounds(x, y, width, height).build()
        );

        this.addRenderableWidget(
            Button.builder(
                label("Sprint Particles",
                    VisionModClient.hideSprintParticles),
                button -> {
                    VisionModClient.hideSprintParticles =
                        !VisionModClient.hideSprintParticles;

                    button.setMessage(label(
                        "Sprint Particles",
                        VisionModClient.hideSprintParticles
                    ));
                }
            ).bounds(
                x, y + spacing, width, height
            ).build()
        );

        this.addRenderableWidget(
            Button.builder(
                label("Ender Chest / Portal",
                    VisionModClient.hideEnderChestParticles),
                button -> {
                    VisionModClient.hideEnderChestParticles =
                        !VisionModClient.hideEnderChestParticles;

                    button.setMessage(label(
                        "Ender Chest / Portal",
                        VisionModClient.hideEnderChestParticles
                    ));
                }
            ).bounds(
                x, y + spacing * 2, width, height
            ).build()
        );

        this.addRenderableWidget(
            Button.builder(
                label("ALL Particles",
                    VisionModClient.hideAllParticles),
                button -> {
                    VisionModClient.hideAllParticles =
                        !VisionModClient.hideAllParticles;

                    button.setMessage(label(
                        "ALL Particles",
                        VisionModClient.hideAllParticles
                    ));
                }
            ).bounds(
                x, y + spacing * 3, width, height
            ).build()
        );

        this.addRenderableWidget(
            Button.builder(
                Component.literal("Back"),
                button -> this.minecraft.setScreen(
                    new VisionScreen()
                )
            ).bounds(
                x, y + spacing * 4 + 8,
                width, height
            ).build()
        );
    }

    private static Component label(
            String name,
            boolean hidden
    ) {
        return Component.literal(
            name + (hidden ? "  HIDE" : "  SHOW")
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(graphics, mouseX, mouseY, delta);

        int y = Math.max(40, (this.height - 155) / 2);

        graphics.drawCenteredString(
            this.font,
            "PARTICLE SETTINGS",
            this.width / 2,
            y - 25,
            0xFFFFFFFF
        );

        graphics.drawCenteredString(
            this.font,
            "HIDE = remove / SHOW = display",
            this.width / 2,
            y + 142,
            0xFFAAAAAA
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
