
package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class OreColors {

    public static final String[] NAMES = {
        "Diamond",
        "Ancient Debris",
        "Iron",
        "Gold",
        "Redstone",
        "Emerald",
        "Lapis",
        "Copper",
        "Coal",
        "Nether Quartz"
    };

    private static final int[] DEFAULTS = {
        0xFF67E8F9,
        0xFFFF78CB,
        0xFFFFA65C,
        0xFFFFD166,
        0xFFFF657D,
        0xFF65F0B2,
        0xFFB48CFF,
        0xFFFFA65C,
        0xFFF0F6FF,
        0xFFF0F6FF
    };

    private static final int[] colors = DEFAULTS.clone();

    private static final Path FILE =
        FabricLoader.getInstance()
            .getConfigDir()
            .resolve("visionmod-ore-colors.properties");

    static {
        load();
    }

    private OreColors() {
    }

    public static int getColor(int index) {
        if (index < 0 || index >= colors.length) {
            return 0xFFFFFFFF;
        }
        return colors[index];
    }

    public static void setColor(int index, int color) {
        if (index < 0 || index >= colors.length) {
            return;
        }

        colors[index] = color | 0xFF000000;
        save();
    }

    public static int colorFor(Block block) {

        if (block == Blocks.DIAMOND_ORE ||
            block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return colors[0];
        }

        if (block == Blocks.ANCIENT_DEBRIS) {
            return colors[1];
        }

        if (block == Blocks.IRON_ORE ||
            block == Blocks.DEEPSLATE_IRON_ORE) {
            return colors[2];
        }

        if (block == Blocks.GOLD_ORE ||
            block == Blocks.DEEPSLATE_GOLD_ORE ||
            block == Blocks.NETHER_GOLD_ORE) {
            return colors[3];
        }

        if (block == Blocks.REDSTONE_ORE ||
            block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return colors[4];
        }

        if (block == Blocks.EMERALD_ORE ||
            block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return colors[5];
        }

        if (block == Blocks.LAPIS_ORE ||
            block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return colors[6];
        }

        if (block == Blocks.COPPER_ORE ||
            block == Blocks.DEEPSLATE_COPPER_ORE) {
            return colors[7];
        }

        if (block == Blocks.COAL_ORE ||
            block == Blocks.DEEPSLATE_COAL_ORE) {
            return colors[8];
        }

        if (block == Blocks.NETHER_QUARTZ_ORE) {
            return colors[9];
        }

        return 0xFFFFFFFF;
    }

    private static void load() {
        if (!Files.exists(FILE)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream input =
                 Files.newInputStream(FILE)) {

            properties.load(input);

            for (int i = 0; i < colors.length; i++) {
                String value = properties.getProperty(
                    "ore." + i
                );

                if (value != null) {
                    colors[i] = (int)
                        Long.parseLong(value, 16);
                }
            }

        } catch (Exception exception) {
            System.err.println(
                "[VisionMod] Could not load ore colors: "
                    + exception.getMessage()
            );
        }
    }

    private static void save() {
        Properties properties = new Properties();

        for (int i = 0; i < colors.length; i++) {
            properties.setProperty(
                "ore." + i,
                Integer.toHexString(colors[i])
            );
        }

        try {
            Files.createDirectories(
                FILE.getParent()
            );

            try (OutputStream output =
                     Files.newOutputStream(FILE)) {

                properties.store(
                    output,
                    "Vision Mod ore highlight colors"
                );
            }

        } catch (Exception exception) {
            System.err.println(
                "[VisionMod] Could not save ore colors: "
                    + exception.getMessage()
            );
        }
    }
}
