
package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class VisionScreen extends Screen {

    private static final int PANEL = 0xEF101722;
    private static final int ROW = 0xE0212C3B;
    private static final int HOVER = 0xFF34485D;
    private static final int CYAN = 0xFF67E8F9;
    private static final int WHITE = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9BAABD;
    private static final int GREEN = 0xFF65F0B2;

    private static final String[] MODULES = {
        "FullBright",
        "Ore Highlight",
        "Clear Water",
        "Clear Lava",
        "Player Hearts",
        "Particles",
        "Cosmetics"
    };

    private boolean orePage;
    private long animationStart;

    // Главное меню
    public VisionScreen() {
        this(false);
    }

    // true = открыть сразу страницу руд
    public VisionScreen(boolean openOrePage) {
        super(Component.literal("Vision Mod"));
        this.orePage = openOrePage;
        this.animationStart = System.nanoTime();
    }

    private int panelWidth() {
        return orePage ? 310 : 244;
    }

    private int panelHeight() {
        return orePage ? 245 : 245;
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private float progress() {
        float t = Math.max(0F, Math.min(
            1F,
            (System.nanoTime() - animationStart)
                / 230_000_000F
        ));

        float remaining = 1F - t;
        return 1F - remaining * remaining * remaining;
    }

    private int top() {
        int endY = Math.max(
            8,
            height - panelHeight() - 46
        );

        int startY = height - 42;

        return Math.round(
            startY + (endY - startY) * progress()
        );
    }

    private void switchPage(boolean ores) {
        orePage = ores;
        animationStart = System.nanoTime();
    }

    private static boolean inside(
        double mx,
        double my,
        int x,
        int y,
        int w,
        int h
    ) {
        return mx >= x && mx < x + w
            && my >= y && my < y + h;
    }

    private boolean active(int id) {
        return switch (id) {
            case 0 -> VisionModClient.fullBright;
            case 1 -> VisionModClient.oreHighlight;
            case 2 -> VisionModClient.clearWater;
            case 3 -> VisionModClient.clearLava;
            case 4 -> VisionModClient.playerHearts;
            case 5 -> VisionModClient.particles;
            case 6 -> VisionModClient.cosmetics;
            default -> false;
        };
    }

    private void toggle(int id) {
        switch (id) {
            case 0 -> VisionModClient.fullBright =
                !VisionModClient.fullBright;

            case 1 -> VisionModClient.oreHighlight =
                !VisionModClient.oreHighlight;

            case 2 -> VisionModClient.clearWater =
                !VisionModClient.clearWater;

            case 3 -> VisionModClient.clearLava =
                !VisionModClient.clearLava;

            case 4 -> VisionModClient.playerHearts =
                !VisionModClient.playerHearts;

            case 5 -> VisionModClient.particles =
                !VisionModClient.particles;

            case 6 -> VisionModClient.cosmetics =
                !VisionModClient.cosmetics;

            default -> {
            }
        }
    }

    @Override
    public void renderBackground(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
        // Не затемняем весь экран.
    }

    @Override
    public void render(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
        super.render(graphics, mouseX, mouseY, delta);

        int x = left();
        int y = top();
        int w = panelWidth();
        int h = panelHeight();

        // Тень
        graphics.fill(
            x - 3, y + 4,
            x + w + 3, y + h + 5,
            0x66000000
        );

        // Панель
        graphics.fill(
            x, y,
            x + w, y + h,
            PANEL
        );

        // Верхняя линия
        graphics.fill(
            x, y,
            x + w, y + 2,
            CYAN
        );

        graphics.drawString(
            font,
            orePage ? "ORE COLORS" : "VISION MOD",
            x + 12,
            y + 13,
            WHITE
        );

        graphics.drawString(
            font,
            "1.21.11",
            x + w - 55,
            y + 13,
            MUTED
        );

        graphics.fill(
            x + 10, y + 30,
            x + w - 10, y + 31,
            0xFF354555
        );

        if (orePage) {
            drawOres(graphics, mouseX, mouseY, x, y);
        } else {
            drawMain(graphics, mouseX, mouseY, x, y, w);
        }
    }

    private void drawMain(
        GuiGraphics graphics,
        int mx,
        int my,
        int x,
        int y,
        int w
    ) {
        for (int i = 0; i < MODULES.length; i++) {
            int by = y + 37 + i * 25;

            boolean hovered = inside(
                mx, my,
                x + 9, by,
                w - 18, 22
            );

            boolean on = active(i);

            graphics.fill(
                x + 9, by,
                x + w - 9, by + 22,
                hovered ? HOVER : ROW
            );

            graphics.fill(
                x + 9, by + 3,
                x + 12, by + 19,
                on ? CYAN : 0xFF526070
            );

            graphics.drawString(
                font,
                MODULES[i],
                x + 20,
                by + 7,
                WHITE
            );

            // Small folder shortcut alongside Cosmetics, separate from its toggle.
            if (i == 6) {
                int folderX = x + w - 99;
                boolean folderHover = inside(mx, my, folderX, by + 2, 43, 18);
                graphics.fill(folderX, by + 2, folderX + 43, by + 20,
                    folderHover ? 0xFF386B7C : 0xFF1B4050);
                graphics.drawCenteredString(font, "DIR",
                    folderX + 21, by + 7, CYAN);
            }

            graphics.drawString(
                font,
                on ? "ON" : "OFF",
                x + w - 40,
                by + 7,
                on ? GREEN : MUTED
            );
        }

        graphics.drawCenteredString(
            font,
            "RMB: Ore / Particles / Cosmetics",
            x + w / 2,
            y + 220,
            MUTED
        );
    }

    private void drawOres(
        GuiGraphics graphics,
        int mx,
        int my,
        int x,
        int y
    ) {
        for (int i = 0; i < OreColors.NAMES.length; i++) {

            int column = i / 5;
            int row = i % 5;

            int bx = x + 9 + column * 147;
            int by = y + 40 + row * 29;

            boolean hovered = inside(
                mx, my,
                bx, by,
                142, 25
            );

            graphics.fill(
                bx, by,
                bx + 142, by + 25,
                hovered ? HOVER : ROW
            );

            graphics.fill(
                bx + 7, by + 7,
                bx + 20, by + 19,
                OreColors.getColor(i)
            );

            graphics.drawString(
                font,
                OreColors.NAMES[i],
                bx + 26,
                by + 9,
                WHITE
            );
        }

        graphics.drawCenteredString(
            font,
            "RMB ON ORE: EDIT COLOR",
            x + 155,
            y + 192,
            CYAN
        );

        graphics.fill(
            x + 11, y + 214,
            x + 299, y + 235,
            ROW
        );

        graphics.drawCenteredString(
            font,
            "BACK",
            x + 155,
            y + 221,
            WHITE
        );
    }

    @Override
    public boolean mouseClicked(
        MouseButtonEvent click,
        boolean doubled
    ) {
        double mx = click.x();
        double my = click.y();

        int x = left();
        int y = top();
        int w = panelWidth();

        boolean leftClick =
            click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;

        boolean rightClick =
            click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;

        if (!orePage) {

            for (int i = 0; i < MODULES.length; i++) {
                int by = y + 37 + i * 25;

                if (!inside(
                    mx, my,
                    x + 9, by,
                    w - 18, 22
                )) {
                    continue;
                }

                if (leftClick && i == 6 && inside(
                    mx, my, x + w - 99, by + 2, 43, 18
                )) {
                    if (!CosmeticsFolder.open() && minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                            Component.literal("Vision Mod: Cannot open folder. Check config/visionmod/cosmetics"),
                            false
                        );
                    }
                    return true;
                }

                if (leftClick) {
                    toggle(i);
                    return true;
                }

                if (rightClick && i == 1) {
                    switchPage(true);
                    return true;
                }

                if (rightClick && i == 5) {
                    minecraft.setScreen(
                        new ParticleSettingsScreen()
                    );
                    return true;
                }

                if (rightClick && i == 6) {
                    minecraft.setScreen(new CosmeticsScreen());
                    return true;
                }
            }

        } else {

            for (int i = 0; i < OreColors.NAMES.length; i++) {

                int column = i / 5;
                int row = i % 5;

                int bx = x + 9 + column * 147;
                int by = y + 40 + row * 29;

                if (inside(
                    mx, my,
                    bx, by,
                    142, 25
                )) {
                    if (rightClick || leftClick) {
                        minecraft.setScreen(
                            new OreColorPickerScreen(i)
                        );
                        return true;
                    }
                }
            }

            if (leftClick && inside(
                mx, my,
                x + 11, y + 214,
                288, 21
            )) {
                switchPage(false);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
