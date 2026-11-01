package io.bugcourier.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

/** A {@link Canvas} over GuiGraphics (Minecraft 1.21.1 to 1.21.11). */
public record GraphicsCanvas(GuiGraphics g) implements Canvas {
    @Override
    public void text(Font font, FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
        g.drawString(font, text, x, y, argb, shadow);
    }

    @Override
    public void centered(Font font, FormattedCharSequence text, int centerX, int y, int argb) {
        g.drawCenteredString(font, text, centerX, y, argb);
    }
}
