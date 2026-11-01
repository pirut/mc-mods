package net.neoforged.neoforge.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class UnicodeGlyphButton extends ExtendedButton {
   public String glyph;
   public float glyphScale;

   public UnicodeGlyphButton(int xPos, int yPos, int width, int height, Component displayString, String glyph, float glyphScale, Button.OnPress handler) {
      super(xPos, yPos, width, height, displayString, handler);
      this.glyph = glyph;
      this.glyphScale = glyphScale;
   }

   @Override
   public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      guiGraphics.blitSprite(
         RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight()
      );
      Component buttonText = this.createNarrationMessage();
      int glyphWidth = (int)(mc.font.width(this.glyph) * this.glyphScale);
      int strWidth = mc.font.width(buttonText);
      int ellipsisWidth = mc.font.width("...");
      int totalWidth = strWidth + glyphWidth;
      if (totalWidth > this.width - 6 && totalWidth > ellipsisWidth) {
         buttonText = Component.literal(mc.font.substrByWidth(buttonText, this.width - 6 - ellipsisWidth).getString().trim() + "...");
      }

      strWidth = mc.font.width(buttonText);
      totalWidth = glyphWidth + strWidth;
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().scale(this.glyphScale, this.glyphScale);
      guiGraphics.drawCenteredString(
         mc.font,
         Component.literal(this.glyph),
         (int)((this.getX() + this.width / 2 - strWidth / 2) / this.glyphScale - glyphWidth / (2.0F * this.glyphScale) + 2.0F),
         (int)((this.getY() + (this.height - 8) / this.glyphScale / 2.0F - 1.0F) / this.glyphScale),
         this.getFGColor()
      );
      guiGraphics.pose().popMatrix();
      guiGraphics.drawCenteredString(
         mc.font, buttonText, (int)(this.getX() + this.width / 2 + glyphWidth / this.glyphScale), this.getY() + (this.height - 8) / 2, this.getFGColor()
      );
   }
}
