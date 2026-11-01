package net.minecraft.realms;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class RealmsLabel implements Renderable {
   private final Component text;
   private final int x;
   private final int y;
   private final int color;

   public RealmsLabel(Component text, int x, int y, int color) {
      this.text = text;
      this.x = x;
      this.y = y;
      this.color = color;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.text, this.x, this.y, this.color);
   }

   public Component getText() {
      return this.text;
   }
}
