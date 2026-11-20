package io.bugcourier.client.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The little text drawing Bug Courier's screens do, so the screens themselves stay the same on every Minecraft
 * version. Each version's {@code BaseScreen} wraps its own drawing context in one (GuiGraphics up to 1.21.11,
 * GuiGraphicsExtractor from 26.1). Colors are ARGB: 1.21.6 and later draw nothing for a zero alpha.
 */
public interface Canvas {
    int WHITE = 0xFFFFFFFF;
    int GREY = 0xFFA0A0A0;
    int LIGHT_GREY = 0xFFAAAAAA;
    int TEXT = 0xFFE0E0E0;

    void text(Font font, FormattedCharSequence text, int x, int y, int argb, boolean shadow);

    void centered(Font font, FormattedCharSequence text, int centerX, int y, int argb);

    default void text(Font font, Component text, int x, int y, int argb) {
        text(font, text.getVisualOrderText(), x, y, argb, true);
    }

    default void centered(Font font, Component text, int centerX, int y, int argb) {
        centered(font, text.getVisualOrderText(), centerX, y, argb);
    }

    /** Draws already wrapped lines centered, one per {@code font.lineHeight}. */
    default void centeredLines(Font font, List<FormattedCharSequence> lines, int centerX, int y, int argb) {
        for (FormattedCharSequence line : lines) {
            centered(font, line, centerX, y, argb);
            y += font.lineHeight;
        }
    }
}
