package io.bugcourier.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minecraft 1.21.1 to 1.21.11: screens draw with GuiGraphics in render. */
public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        draw(new GraphicsCanvas(g));
    }

    /** Draws the screen's own text, over its widgets. */
    protected abstract void draw(Canvas c);
}
