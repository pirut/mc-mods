package io.bugcourier.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A read-only, scrolling block of text. Minecraft 1.21.4 to 1.21.11: built on AbstractTextAreaWidget. */
public final class TextView extends AbstractTextAreaWidget {
    private final Font font;
    private final TextLines lines = new TextLines();

    public TextView(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
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
    protected double scrollRate() {
        return font.lineHeight * 3;
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        lines.draw(new GraphicsCanvas(g), font, getX() + innerPadding(), getY() + innerPadding(), scrollAmount(), height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.translatable("bugcourier.preview.title"));
    }
}
