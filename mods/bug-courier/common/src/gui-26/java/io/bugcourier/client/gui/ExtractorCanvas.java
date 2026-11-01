package io.bugcourier.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

/** A {@link Canvas} over GuiGraphicsExtractor (Minecraft 26.1 and later). */
public record ExtractorCanvas(GuiGraphicsExtractor g) implements Canvas {
    @Override
    public void text(Font font, FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
        g.text(font, text, x, y, argb, shadow);
    }

    @Override
    public void centered(Font font, FormattedCharSequence text, int centerX, int y, int argb) {
        g.centeredText(font, text, centerX, y, argb);
    }
}
