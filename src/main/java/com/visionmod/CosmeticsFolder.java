package com.visionmod;

import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opens the user's own local cosmetics folder, without shell commands. */
public final class CosmeticsFolder {
    private CosmeticsFolder() {}

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir()
            .resolve("visionmod").resolve("cosmetics");
    }

    public static boolean open() {
        try {
            Path directory = path();
            Files.createDirectories(directory);
            Files.createDirectories(directory.resolve("head"));
            Files.createDirectories(directory.resolve("back"));

            Path instructions = directory.resolve("README.txt");
            if (Files.notExists(instructions)) {
                Files.writeString(instructions,
                    "VISION MOD 0.3.0 - COSMETICS FOLDER\n\n" +
                    "head/ - files for head cosmetics\n" +
                    "back/ - files for back cosmetics\n\n" +
                    "NOTE: In version 0.3.0 custom PNG / 3D models are NOT yet loaded by the mod.\n" +
                    "Placing files here is only preparation for a future importer.\n" +
                    "Do not place private information in this folder.\n",
                    StandardCharsets.UTF_8);
            }

            if (!Desktop.isDesktopSupported()) {
                return false;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                return false;
            }
            desktop.open(directory.toFile());
            return true;
        } catch (IOException | RuntimeException exception) {
            System.err.println("[VisionMod] Failed to open cosmetics folder: " + exception);
            return false;
        }
    }
}
