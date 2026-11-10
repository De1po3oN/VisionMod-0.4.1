package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CosmeticsScreen extends Screen {
    private static final int CYAN = 0xFF67E8F9;
    private static final int TEXT = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9BAABD;

    public CosmeticsScreen() { super(Component.literal("Vision Cosmetics")); }
    private int x() { return (width - 258) / 2; }
    private int y() { return Math.max(8, height - 228); }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int x=x(), y=y();
        g.fill(x-3,y+3,x+261,y+230,0x66000000);
        g.fill(x,y,x+258,y+224,0xEF101722);
        g.fill(x,y,x+258,y+2,CYAN);
        g.drawString(font,"VISION COSMETICS",x+12,y+13,TEXT);
        g.drawString(font,"3D MODELS / CLOUD SYNC",x+12,y+35,MUTED);
        drawButton(g,x,y+55,"Head: " + VisionCosmetics.localHead);
        drawButton(g,x,y+87,"Back: " + VisionCosmetics.localBack);
        drawButton(g,x,y+119,"Other players: " + (VisionModClient.showOtherCosmetics?"ON":"OFF"));
        drawButton(g,x,y+151,"OPEN COSMETICS FOLDER");
        g.drawCenteredString(font,"SYNC: " + VisionCosmetics.syncStatus,x+129,y+180,MUTED);
        drawButton(g,x,y+190,"BACK");
    }

    private void drawButton(GuiGraphics g,int x,int y,String text) {
        g.fill(x+12,y,x+246,y+25,0xFF26394B);
        g.drawCenteredString(font,text,x+129,y+8,TEXT);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent click,boolean doubled) {
        if(click.button()!=GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click,doubled);
        double mx=click.x(),my=click.y();int x=x(),y=y();
        if(mx<x+12 || mx>=x+246) return super.mouseClicked(click,doubled);
        if(my>=y+55 && my<y+80) {VisionCosmetics.localHead=next(VisionCosmetics.localIds("head"),VisionCosmetics.localHead);VisionCosmetics.selectionChanged();return true;}
        if(my>=y+87 && my<y+112) {VisionCosmetics.localBack=next(VisionCosmetics.localIds("back"),VisionCosmetics.localBack);VisionCosmetics.selectionChanged();return true;}
        if(my>=y+119 && my<y+144) {VisionModClient.showOtherCosmetics=!VisionModClient.showOtherCosmetics;return true;}
        if(my>=y+151 && my<y+176) {
            if (!CosmeticsFolder.open() && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                    Component.literal("Vision Mod: Cannot open cosmetics folder"), false);
            }
            return true;
        }
        if(my>=y+190 && my<y+215) {minecraft.setScreen(new VisionScreen());return true;}
        return super.mouseClicked(click,doubled);
    }
    private static String next(java.util.List<String> items,String current) {
        for (int i=0;i<items.size();i++) if(items.get(i).equals(current)) return items.get((i+1)%items.size());
        return items.get(0);
    }
    @Override public void onClose() {minecraft.setScreen(new VisionScreen());}
    @Override public boolean isPauseScreen() {return false;}
}
