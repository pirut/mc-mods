package io.bugcourier.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The read-only, wrapped text of the preview: the report exactly as it will be sent. Each version's
 * {@code TextView} scroll widget holds one and draws it.
 */
public final class TextLines {
    private static final int MAX_LINES = 4000;
    private final List<FormattedCharSequence> lines = new ArrayList<>();

    public void set(Font font, String text, int wrapWidth) {
        lines.clear();
        String[] raw = text.split("\\R", -1);
        for (int i = 0; i < raw.length; i++) {
            String line = raw[i];
            if (lines.size() >= MAX_LINES) {
                lines.add(Component.translatable("bugcourier.preview.more_lines", raw.length - i).getVisualOrderText());
                break;
            }
            if (line.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(line.replace('\t', ' ')), wrapWidth));
        }
    }

    public int height(Font font) {
        return lines.size() * font.lineHeight;
    }

    /** Draws the lines that show in a view {@code viewHeight} tall, scrolled down by {@code scroll} pixels. */
    public void draw(Canvas c, Font font, int x, int top, double scroll, int viewHeight) {
        int first = Math.max(0, (int) (scroll / font.lineHeight) - 1);
        int last = Math.min(lines.size(), first + viewHeight / font.lineHeight + 3);
        for (int i = first; i < last; i++) c.text(font, lines.get(i), x, top + i * font.lineHeight, Canvas.TEXT, false);
    }
}
