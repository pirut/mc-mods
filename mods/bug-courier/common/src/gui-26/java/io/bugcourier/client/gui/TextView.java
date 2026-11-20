package io.bugcourier.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A read-only, scrolling block of text. Minecraft 26.1 and later: AbstractTextAreaWidget with scrollbar settings. */
public final class TextView extends AbstractTextAreaWidget {
    private final Font font;
    private final TextLines lines = new TextLines();

    public TextView(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), defaultSettings(font.lineHeight * 3));
        this.font = font;
    }

    public void setText(String text) {
        lines.set(font, text, width - totalInnerPadding());
        setScrollAmount(0);
    }

    @Override
    protected int getInnerHeight() {
        return lines.height(font);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        lines.draw(new ExtractorCanvas(g), font, getX() + innerPadding(), getY() + innerPadding(), scrollAmount(), height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.translatable("bugcourier.preview.title"));
    }
}
