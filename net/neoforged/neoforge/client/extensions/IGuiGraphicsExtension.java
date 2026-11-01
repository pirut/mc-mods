package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public interface IGuiGraphicsExtension {
   int DEFAULT_BACKGROUND_COLOR = -267386864;
   int DEFAULT_BORDER_COLOR_START = 1347420415;
   int DEFAULT_BORDER_COLOR_END = 1344798847;
   String UNDO_CHAR = "↶";
   String RESET_CHAR = "☄";
   String VALID = "✔";
   String INVALID = "✕";
   int[] TEXT_COLOR_CODES = new int[]{
      0,
      170,
      43520,
      43690,
      11141120,
      11141290,
      16755200,
      11184810,
      5592405,
      5592575,
      5635925,
      5636095,
      16733525,
      16733695,
      16777045,
      16777215,
      0,
      42,
      10752,
      10794,
      2752512,
      2752554,
      2763264,
      2763306,
      1381653,
      1381695,
      1392405,
      1392447,
      4134165,
      4134207,
      4144917,
      4144959
   };

   private GuiGraphics self() {
      return (GuiGraphics)this;
   }

   default int getColorFromFormattingCharacter(char c, boolean isLighter) {
      return TEXT_COLOR_CODES[isLighter ? "0123456789abcdef".indexOf(c) : "0123456789abcdef".indexOf(c) + 16];
   }

   default void drawScrollingString(ActiveTextCollector textCollector, Font font, Component text, int minX, int maxX, int y) {
      int maxWidth = maxX - minX;
      int textWidth = font.width(text.getVisualOrderText());
      if (textWidth <= maxWidth) {
         this.self().drawString(font, text, minX, y, -1);
      } else {
         textCollector.acceptScrollingWithDefaultCenter(text, minX, maxX, y - 1, y + 9);
      }
   }

   default void blitInscribed(Identifier texture, int x, int y, int boundsWidth, int boundsHeight, int rectWidth, int rectHeight) {
      this.blitInscribed(texture, x, y, boundsWidth, boundsHeight, rectWidth, rectHeight, true, true);
   }

   default void blitInscribed(
      Identifier texture, int x, int y, int boundsWidth, int boundsHeight, int rectWidth, int rectHeight, boolean centerX, boolean centerY
   ) {
      if (rectWidth * boundsHeight > rectHeight * boundsWidth) {
         int h = boundsHeight;
         boundsHeight = (int)(boundsWidth * ((double)rectHeight / rectWidth));
         if (centerY) {
            y += (h - boundsHeight) / 2;
         }
      } else {
         int w = boundsWidth;
         boundsWidth = (int)(boundsHeight * ((double)rectWidth / rectHeight));
         if (centerX) {
            x += (w - boundsWidth) / 2;
         }
      }

      this.self().blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, boundsWidth, boundsHeight, rectWidth, rectHeight, rectWidth, rectHeight);
   }
}
