package io.bugcourier.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** A read-only, scrolling block of text: the preview shows the report exactly as it will be sent. */
final class TextView extends AbstractScrollWidget {
    private static final int MAX_LINES = 4000;
    private final Font font;
    private final List<FormattedCharSequence> lines = new ArrayList<>();

    TextView(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.font = font;
    }

    void setText(String text) {
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
            lines.addAll(font.split(Component.literal(line.replace('\t', ' ')), width - totalInnerPadding()));
        }
        setScrollAmount(0);
    }

    @Override
    protected int getInnerHeight() {
        return lines.size() * font.lineHeight;
    }

    @Override
    protected double scrollRate() {
        return font.lineHeight * 3;
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int top = getY() + innerPadding();
        int first = Math.max(0, (int) (scrollAmount() / font.lineHeight) - 1);
        int last = Math.min(lines.size(), first + height / font.lineHeight + 3);
        for (int i = first; i < last; i++) {
            g.drawString(font, lines.get(i), getX() + innerPadding(), top + i * font.lineHeight, 0xE0E0E0, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.translatable("bugcourier.preview.title"));
    }
}
