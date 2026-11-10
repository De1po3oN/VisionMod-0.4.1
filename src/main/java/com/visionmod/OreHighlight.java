
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

        // Проверяем ближайшие руды раз в 20 тиков.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (!VisionModClient.isOreEnabled()
                    || client.level == null
                    || client.player == null) {

                visibleOres.clear();
                ticks = 0;
                return;
            }

            ticks++;

            if (ticks < 20) {
                return;
            }

            ticks = 0;
            visibleOres.clear();

            BlockPos center =
                client.player.blockPosition();

            BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

            scan:
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = -VERTICAL_RADIUS;
                         y <= VERTICAL_RADIUS; y++) {

                    for (int z = -RADIUS;
                         z <= RADIUS; z++) {

                        if (visibleOres.size() >= MAX_ORES) {
                            break scan;
                        }

                        cursor.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                        );

                        BlockState state =
                            client.level.getBlockState(cursor);

                        if (!ORES.contains(state.getBlock())) {
                            continue;
                        }

                        // Подсвечиваем только руды,
                        // имеющие хотя бы одну открытую сторону.
                        boolean exposed = false;

                        for (Direction side : Direction.values()) {

                            BlockPos neighbor =
                                cursor.relative(side);

                            BlockState neighborState =
                                client.level.getBlockState(neighbor);

                            if (!neighborState.isSolidRender()) {
                                exposed = true;
                                break;
                            }
                        }

                        if (exposed) {
                            visibleOres.add(cursor.immutable());
                        }
                    }
                }
            }
        });

        // ВАЖНО:
        // BEFORE_DEBUG_RENDER вместо BEFORE_ENTITIES.
        // Линии добавляются после большинства
        // непрозрачных объектов и эффектов блоков.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {

            if (!VisionModClient.isOreEnabled()
                    || visibleOres.isEmpty()
                    || context.consumers() == null
                    || context.matrices() == null) {
                return;
            }

            Minecraft client = Minecraft.getInstance();

            if (client.level == null) {
                return;
            }

            Vec3 camera =
                client.gameRenderer
                    .getMainCamera()
                    .position();

            VertexConsumer lines =
                context.consumers()
                    .getBuffer(RenderTypes.lines());

            for (BlockPos pos : visibleOres) {

                Block block = client.level
                    .getBlockState(pos)
                    .getBlock();

                if (!ORES.contains(block)) {
                    continue;
                }

                // Цвет из настроек Vision Mod.
                int color = OreColors.colorFor(block);

                // Координаты относительно камеры.
                double x = pos.getX() - camera.x;
                double y = pos.getY() - camera.y;
                double z = pos.getZ() - camera.z;

                // Тонкий контур немного внутри блока.
                ShapeRenderer.renderShape(
                    context.matrices(),
                    lines,
                    Shapes.box(
                        0.002, 0.002, 0.002,
                        0.998, 0.998, 0.998
                    ),
                    x,
                    y,
                    z,
                    color,
                    2.0F
                );
            }
        });
    }
}
