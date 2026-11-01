package io.bugcourier.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minecraft 26.1 and later: screens extract their render state into a GuiGraphicsExtractor. */
public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        draw(new ExtractorCanvas(g));
    }

    /** Draws the screen's own text, over its widgets. */
    protected abstract void draw(Canvas c);
}
