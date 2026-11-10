
package com.visionmod;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OreHighlight {

    private static final int RADIUS = 12;
    private static final int VERTICAL_RADIUS = 8;
    private static final int MAX_ORES = 300;

    private static final Set<Block> ORES = Set.of(
        Blocks.COAL_ORE,
        Blocks.DEEPSLATE_COAL_ORE,

        Blocks.IRON_ORE,
        Blocks.DEEPSLATE_IRON_ORE,

        Blocks.COPPER_ORE,
        Blocks.DEEPSLATE_COPPER_ORE,

        Blocks.GOLD_ORE,
        Blocks.DEEPSLATE_GOLD_ORE,

        Blocks.REDSTONE_ORE,
        Blocks.DEEPSLATE_REDSTONE_ORE,

        Blocks.EMERALD_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE,

        Blocks.LAPIS_ORE,
        Blocks.DEEPSLATE_LAPIS_ORE,

        Blocks.DIAMOND_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE,

        Blocks.NETHER_GOLD_ORE,
        Blocks.NETHER_QUARTZ_ORE,
        Blocks.ANCIENT_DEBRIS
    );

    private static final List<BlockPos> visibleOres =
        new ArrayList<>();

    private static int ticks = 0;

    private OreHighlight() {
    }

    public static void init() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (!VisionModClient.isOreEnabled()
                    || client.level == null
                    || client.player == null) {

                visibleOres.clear();
                return;
            }

            if (++ticks % 20 != 0) {
                return;
            }

            visibleOres.clear();

            BlockPos center =
                client.player.blockPosition();

            BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = -VERTICAL_RADIUS;
                         y <= VERTICAL_RADIUS; y++) {
                    for (int z = -RADIUS;
                         z <= RADIUS; z++) {

                        if (visibleOres.size() >= MAX_ORES) {
                            return;
                        }

                        cursor.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                        );

                        BlockState state =
                            client.level.getBlockState(cursor);

                        Block block = state.getBlock();

                        if (!ORES.contains(block)) {
                            continue;
                        }

                        boolean exposed = false;

                        for (Direction side : Direction.values()) {

                            BlockPos neighbor =
                                cursor.relative(side);

                            if (!client.level
                                    .getBlockState(neighbor)
                                    .isSolidRender()) {
                                exposed = true;
                                break;
                            }
                        }

                        if (exposed) {
                            visibleOres.add(
                                cursor.immutable()
                            );
                        }
                    }
                }
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {

            if (!VisionModClient.isOreEnabled()
                    || visibleOres.isEmpty()
                    || context.consumers() == null) {
                return;
            }

            Minecraft client =
                Minecraft.getInstance();

            if (client.level == null) {
                return;
            }

            Vec3 camera =
                client.gameRenderer
                    .getMainCamera().position();

            VertexConsumer lines =
                context.consumers()
                    .getBuffer(RenderTypes.lines());

            for (BlockPos pos : visibleOres) {

                Block block =
                    client.level
                        .getBlockState(pos)
                        .getBlock();

                if (!ORES.contains(block)) {
                    continue;
                }

                // Live color from the menu.
                int argb = OreColors.colorFor(block);

                double x = pos.getX() - camera.x;
                double y = pos.getY() - camera.y;
                double z = pos.getZ() - camera.z;

                ShapeRenderer.renderShape(
                    context.matrices(),
                    lines,
                    Shapes.box(
                        0.002, 0.002, 0.002,
                        0.998, 0.998, 0.998
                    ),
                    x, y, z,
                    argb,
                    2.0F
                );
            }
        });
    }
}
