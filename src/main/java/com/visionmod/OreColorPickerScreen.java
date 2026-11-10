
package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Locale;
import java.util.Map;

public final class OreColorPickerScreen extends Screen {

    private static final int PANEL = 0xF016202D;
    private static final int TEXT = 0xFFF2F6FF;
    private static final int MUTED = 0xFF9BAABE;
    private static final int ACCENT = 0xFF67E8F9;

    private static final int SQUARE = 152;
    private static final int HUE_WIDTH = 15;

    private static final Map<String, Integer> NAMED =
        Map.ofEntries(
            Map.entry("red", 0xFF5555),
            Map.entry("green", 0xFF55FF55),
            Map.entry("blue", 0xFF5555FF),
            Map.entry("yellow", 0xFFFFFF55),
            Map.entry("cyan", 0xFF55FFFF),
            Map.entry("pink", 0xFFFF55AA),
            Map.entry("purple", 0xFFAA55FF),
            Map.entry("orange", 0xFFFFAA33),
            Map.entry("white", 0xFFFFFFFF),
            Map.entry("black", 0xFF000000),
            Map.entry("lime", 0xFF00FF00),
            Map.entry("gold", 0xFFFFD700),
            Map.entry("красный", 0xFFFF5555),
            Map.entry("зелёный", 0xFF55FF55),
            Map.entry("синий", 0xFF5555FF),
            Map.entry("жёлтый", 0xFFFFFF55),
            Map.entry("розовый", 0xFFFF55AA),
            Map.entry("фиолетовый", 0xFFAA55FF),
            Map.entry("голубой", 0xFF55FFFF),
            Map.entry("белый", 0xFFFFFFFF),
            Map.entry("чёрный", 0xFF000000)
        );

    private final int oreIndex;

    private float hue;
    private float saturation;
    private float brightness;

    private int previewColor;

    private EditBox colorField;

    private boolean draggingSquare = false;
    private boolean draggingHue = false;
    private boolean updatingField = false;
    private boolean validInput = true;

    public OreColorPickerScreen(int oreIndex) {
        super(Component.literal("Ore Color Picker"));

        this.oreIndex = Math.max(
            0,
            Math.min(OreColors.NAMES.length - 1, oreIndex)
        );

        previewColor = OreColors.getColor(this.oreIndex);

        loadHSV(previewColor);
    }

    private int panelX() {
        return (width - 284) / 2;
    }

    private int panelY() {
        return Math.max(8, height - 288);
    }

    private int squareX() {
        return panelX() + 19;
    }

    private int squareY() {
        return panelY() + 48;
    }

    private int hueX() {
        return squareX() + SQUARE + 13;
    }

    private void loadHSV(int color) {
        float[] hsv = Color.RGBtoHSB(
            (color >> 16) & 255,
            (color >> 8) & 255,
            color & 255,
            null
        );

        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    private void updatePreview() {
        previewColor = 0xFF000000 |
            (Color.HSBtoRGB(
                hue,
                saturation,
                brightness
            ) & 0xFFFFFF);

        validInput = true;
        updateText();
    }

    private void updateText() {
        if (colorField == null) {
            return;
        }

        updatingField = true;

        colorField.setValue(
            String.format(
                Locale.ROOT,
                "#%06X",
                previewColor & 0xFFFFFF
            )
        );

        updatingField = false;
    }

    private void commitColor() {
        OreColors.setColor(oreIndex, previewColor);
    }

    @Override
    protected void init() {
        int px = panelX();
        int py = panelY();

        colorField = new EditBox(
            font,
            px + 19,
            py + 225,
            152,
            20,
            Component.literal("HEX or color name")
        );

        colorField.setMaxLength(32);
        colorField.setValue(
            String.format(
                Locale.ROOT,
                "#%06X",
                previewColor & 0xFFFFFF
            )
        );

        colorField.setResponder(value -> {
            if (updatingField) {
                return;
            }

            Integer parsed = parseColor(value);

            validInput = parsed != null;

            if (parsed == null) {
                return;
            }

            previewColor = 0xFF000000 | parsed;

            loadHSV(previewColor);
            commitColor();
        });

        addRenderableWidget(colorField);
    }

    private static Integer parseColor(String input) {
        String value = input.trim()
            .toLowerCase(Locale.ROOT);

        if (NAMED.containsKey(value)) {
            return NAMED.get(value) & 0xFFFFFF;
        }

        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x")) {
            value = value.substring(2);
        }

        if (!value.matches("[0-9a-f]{6}")) {
            return null;
        }

        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean inside(
        double x,
        double y,
        int bx,
        int by,
        int w,
        int h
    ) {
        return x >= bx && x < bx + w
            && y >= by && y < by + h;
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    private void selectSquare(double mx, double my) {
        saturation = clamp(
            (float) ((mx - squareX()) / (SQUARE - 1))
        );

        brightness = 1F - clamp(
            (float) ((my - squareY()) / (SQUARE - 1))
        );

        updatePreview();
    }

    private void selectHue(double my) {
        hue = clamp(
            (float) ((my - squareY()) / (SQUARE - 1))
        );

        updatePreview();
    }

    private static void drawRing(
        GuiGraphics g,
        int x,
        int y,
        int radius,
        int color
    ) {
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0 * i / 24.0;

            int px = x + (int) Math.round(
                Math.cos(angle) * radius
            );

            int py = y + (int) Math.round(
                Math.sin(angle) * radius
            );

            g.fill(px, py, px + 2, py + 2, color);
        }
    }

    @Override
    public void renderBackground(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float delta
    ) {
        // No fullscreen blur or darkening.
    }

    @Override
    public void render(
        GuiGraphics g,
        int mouseX,
        int mouseY,
        float delta
    ) {
        int px = panelX();
        int py = panelY();

        g.fill(
            px - 3, py + 4,
            px + 287, py + 292,
            0x66000000
        );

        g.fill(
            px, py,
            px + 284, py + 284,
            PANEL
        );

        g.fill(
            px, py,
            px + 284, py + 2,
            ACCENT
        );

        g.drawString(
            font,
            "ORE COLOR PICKER",
            px + 19, py + 13,
            TEXT
        );

        g.drawString(
            font,
            OreColors.NAMES[oreIndex],
            px + 19, py + 29,
            ACCENT
        );

        int sx = squareX();
        int sy = squareY();

        // Hue/saturation gradient.
        for (int x = 0; x < SQUARE; x++) {
            float s = (float) x / (SQUARE - 1);

            int color = 0xFF000000 |
                (Color.HSBtoRGB(hue, s, 1F) & 0xFFFFFF);

            g.fill(
                sx + x, sy,
                sx + x + 1, sy + SQUARE,
                color
            );
        }

        // Brightness overlay.
        g.fillGradient(
            sx, sy,
            sx + SQUARE, sy + SQUARE,
            0x00000000,
            0xFF000000
        );

        g.renderOutline(
            sx - 1, sy - 1,
            SQUARE + 2, SQUARE + 2,
            0xFF677789
        );

        // Vertical rainbow hue bar.
        int hx = hueX();

        for (int y = 0; y < SQUARE; y++) {
            float h = (float) y / (SQUARE - 1);

            int color = 0xFF000000 |
                (Color.HSBtoRGB(h, 1F, 1F) & 0xFFFFFF);

            g.fill(
                hx, sy + y,
                hx + HUE_WIDTH, sy + y + 1,
                color
            );
        }

        g.renderOutline(
            hx - 1, sy - 1,
            HUE_WIDTH + 2, SQUARE + 2,
            0xFF677789
        );

        // Draggable selection circle.
        int cx = sx + Math.round(
            saturation * (SQUARE - 1)
        );

        int cy = sy + Math.round(
            (1F - brightness) * (SQUARE - 1)
        );

        drawRing(g, cx, cy, 5, 0xFF000000);
        drawRing(g, cx, cy, 4, 0xFFFFFFFF);

        // Hue selection indicator.
        int hy = sy + Math.round(
            hue * (SQUARE - 1)
        );

        g.fill(
            hx - 3, hy - 2,
            hx + HUE_WIDTH + 3, hy + 2,
            0xFFFFFFFF
        );

        // Current color preview.
        g.fill(
            px + 207, py + 48,
            px + 265, py + 94,
            previewColor
        );

        g.renderOutline(
            px + 207, py + 48,
            58, 46,
            0xFFFFFFFF
        );

        g.drawString(
            font,
            "PREVIEW",
            px + 207, py + 99,
            MUTED
        );

        g.drawString(
            font,
            String.format(
                Locale.ROOT,
                "#%06X",
                previewColor & 0xFFFFFF
            ),
            px + 207, py + 114,
            TEXT
        );

        g.drawString(
            font,
            "HEX / NAME",
            px + 19, py + 211,
            MUTED
        );

        g.drawString(
            font,
            validInput ? "OK" : "INVALID",
            px + 179, py + 230,
            validInput ? 0xFF65F0B2 : 0xFFFF657D
        );

        g.fill(
            px + 19, py + 255,
            px + 265, py + 274,
            0xFF26394B
        );

        g.drawCenteredString(
            font,
            "SAVE & BACK",
            px + 142, py + 261,
            ACCENT
        );

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(
        MouseButtonEvent click,
        boolean doubled
    ) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            if (inside(
                click.x(), click.y(),
                squareX(), squareY(),
                SQUARE, SQUARE
            )) {
                draggingSquare = true;
                selectSquare(click.x(), click.y());
                return true;
            }

            if (inside(
                click.x(), click.y(),
                hueX(), squareY(),
                HUE_WIDTH, SQUARE
            )) {
                draggingHue = true;
                selectHue(click.y());
                return true;
            }

            if (inside(
                click.x(), click.y(),
                panelX() + 19,
                panelY() + 255,
                246, 19
            )) {
                commitColor();
                minecraft.setScreen(new VisionScreen(true));
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(
        MouseButtonEvent click,
        double dx,
        double dy
    ) {
        if (draggingSquare) {
            selectSquare(click.x(), click.y());
            return true;
        }

        if (draggingHue) {
            selectHue(click.y());
            return true;
        }

        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (draggingSquare || draggingHue) {
            draggingSquare = false;
            draggingHue = false;
            commitColor();
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        commitColor();
        minecraft.setScreen(new VisionScreen(true));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
