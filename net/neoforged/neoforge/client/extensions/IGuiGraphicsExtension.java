package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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

   default int drawScrollingString(Font font, Component text, int minX, int maxX, int y, int color) {
      int maxWidth = maxX - minX;
      int textWidth = font.width(text.getVisualOrderText());
      if (textWidth <= maxWidth) {
         return this.self().drawString(font, text, minX, y, color);
      } else {
         AbstractWidget.renderScrollingString(this.self(), font, text, minX, y, maxX, y + 9, color);
         return maxWidth;
      }
   }

   default void blitWithBorder(ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, int borderSize) {
      this.blitWithBorder(texture, x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize);
   }

   default void blitWithBorder(
      ResourceLocation texture,
      int x,
      int y,
      int u,
      int v,
      int width,
      int height,
      int textureWidth,
      int textureHeight,
      int topBorder,
      int bottomBorder,
      int leftBorder,
      int rightBorder
   ) {
      int fillerWidth = textureWidth - leftBorder - rightBorder;
      int fillerHeight = textureHeight - topBorder - bottomBorder;
      int canvasWidth = width - leftBorder - rightBorder;
      int canvasHeight = height - topBorder - bottomBorder;
      int xPasses = canvasWidth / fillerWidth;
      int remainderWidth = canvasWidth % fillerWidth;
      int yPasses = canvasHeight / fillerHeight;
      int remainderHeight = canvasHeight % fillerHeight;
      this.self().blit(texture, x, y, u, v, leftBorder, topBorder);
      this.self().blit(texture, x + leftBorder + canvasWidth, y, u + leftBorder + fillerWidth, v, rightBorder, topBorder);
      this.self().blit(texture, x, y + topBorder + canvasHeight, u, v + topBorder + fillerHeight, leftBorder, bottomBorder);
      this.self()
         .blit(
            texture,
            x + leftBorder + canvasWidth,
            y + topBorder + canvasHeight,
            u + leftBorder + fillerWidth,
            v + topBorder + fillerHeight,
            rightBorder,
            bottomBorder
         );

      for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
         this.self().blit(texture, x + leftBorder + i * fillerWidth, y, u + leftBorder, v, i == xPasses ? remainderWidth : fillerWidth, topBorder);
         this.self()
            .blit(
               texture,
               x + leftBorder + i * fillerWidth,
               y + topBorder + canvasHeight,
               u + leftBorder,
               v + topBorder + fillerHeight,
               i == xPasses ? remainderWidth : fillerWidth,
               bottomBorder
            );

         for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            this.self()
               .blit(
                  texture,
                  x + leftBorder + i * fillerWidth,
                  y + topBorder + j * fillerHeight,
                  u + leftBorder,
                  v + topBorder,
                  i == xPasses ? remainderWidth : fillerWidth,
                  j == yPasses ? remainderHeight : fillerHeight
               );
         }
      }

      for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
         this.self().blit(texture, x, y + topBorder + j * fillerHeight, u, v + topBorder, leftBorder, j == yPasses ? remainderHeight : fillerHeight);
         this.self()
            .blit(
               texture,
               x + leftBorder + canvasWidth,
               y + topBorder + j * fillerHeight,
               u + leftBorder + fillerWidth,
               v + topBorder,
               rightBorder,
               j == yPasses ? remainderHeight : fillerHeight
            );
      }
   }

   default void blitInscribed(ResourceLocation texture, int x, int y, int boundsWidth, int boundsHeight, int rectWidth, int rectHeight) {
      this.blitInscribed(texture, x, y, boundsWidth, boundsHeight, rectWidth, rectHeight, true, true);
   }

   default void blitInscribed(
      ResourceLocation texture, int x, int y, int boundsWidth, int boundsHeight, int rectWidth, int rectHeight, boolean centerX, boolean centerY
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

      this.self().blit(texture, x, y, boundsWidth, boundsHeight, 0.0F, 0.0F, rectWidth, rectHeight, rectWidth, rectHeight);
   }
}
