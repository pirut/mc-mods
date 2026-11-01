package net.neoforged.neoforge.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

@Deprecated(
   forRemoval = true
)
public class ScreenUtils {
   public static final int DEFAULT_BACKGROUND_COLOR = -267386864;
   public static final int DEFAULT_BORDER_COLOR_START = 1347420415;
   public static final int DEFAULT_BORDER_COLOR_END = 1344798847;
   public static final String UNDO_CHAR = "↶";
   public static final String RESET_CHAR = "☄";
   public static final String VALID = "✔";
   public static final String INVALID = "✕";
   public static int[] TEXT_COLOR_CODES = new int[]{
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

   public static int getColorFromFormattingCharacter(char c, boolean isLighter) {
      return TEXT_COLOR_CODES[isLighter ? "0123456789abcdef".indexOf(c) : "0123456789abcdef".indexOf(c) + 16];
   }

   public static void blitWithBorder(
      GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, int borderSize, float zLevel
   ) {
      blitWithBorder(guiGraphics, x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize, zLevel);
   }

   public static void blitWithBorder(
      GuiGraphics guiGraphics,
      ResourceLocation res,
      int x,
      int y,
      int u,
      int v,
      int width,
      int height,
      int textureWidth,
      int textureHeight,
      int borderSize,
      float zLevel
   ) {
      blitWithBorder(guiGraphics, res, x, y, u, v, width, height, textureWidth, textureHeight, borderSize, borderSize, borderSize, borderSize, zLevel);
   }

   public static void blitWithBorder(
      GuiGraphics guiGraphics,
      ResourceLocation res,
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
      int rightBorder,
      float zLevel
   ) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, res);
      blitWithBorder(guiGraphics, x, y, u, v, width, height, textureWidth, textureHeight, topBorder, bottomBorder, leftBorder, rightBorder, zLevel);
   }

   public static void blitWithBorder(
      GuiGraphics guiGraphics,
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
      int rightBorder,
      float zLevel
   ) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int fillerWidth = textureWidth - leftBorder - rightBorder;
      int fillerHeight = textureHeight - topBorder - bottomBorder;
      int canvasWidth = width - leftBorder - rightBorder;
      int canvasHeight = height - topBorder - bottomBorder;
      int xPasses = canvasWidth / fillerWidth;
      int remainderWidth = canvasWidth % fillerWidth;
      int yPasses = canvasHeight / fillerHeight;
      int remainderHeight = canvasHeight % fillerHeight;
      drawTexturedModalRect(guiGraphics, x, y, u, v, leftBorder, topBorder, zLevel);
      drawTexturedModalRect(guiGraphics, x + leftBorder + canvasWidth, y, u + leftBorder + fillerWidth, v, rightBorder, topBorder, zLevel);
      drawTexturedModalRect(guiGraphics, x, y + topBorder + canvasHeight, u, v + topBorder + fillerHeight, leftBorder, bottomBorder, zLevel);
      drawTexturedModalRect(
         guiGraphics,
         x + leftBorder + canvasWidth,
         y + topBorder + canvasHeight,
         u + leftBorder + fillerWidth,
         v + topBorder + fillerHeight,
         rightBorder,
         bottomBorder,
         zLevel
      );

      for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
         drawTexturedModalRect(
            guiGraphics, x + leftBorder + i * fillerWidth, y, u + leftBorder, v, i == xPasses ? remainderWidth : fillerWidth, topBorder, zLevel
         );
         drawTexturedModalRect(
            guiGraphics,
            x + leftBorder + i * fillerWidth,
            y + topBorder + canvasHeight,
            u + leftBorder,
            v + topBorder + fillerHeight,
            i == xPasses ? remainderWidth : fillerWidth,
            bottomBorder,
            zLevel
         );

         for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            drawTexturedModalRect(
               guiGraphics,
               x + leftBorder + i * fillerWidth,
               y + topBorder + j * fillerHeight,
               u + leftBorder,
               v + topBorder,
               i == xPasses ? remainderWidth : fillerWidth,
               j == yPasses ? remainderHeight : fillerHeight,
               zLevel
            );
         }
      }

      for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
         drawTexturedModalRect(
            guiGraphics, x, y + topBorder + j * fillerHeight, u, v + topBorder, leftBorder, j == yPasses ? remainderHeight : fillerHeight, zLevel
         );
         drawTexturedModalRect(
            guiGraphics,
            x + leftBorder + canvasWidth,
            y + topBorder + j * fillerHeight,
            u + leftBorder + fillerWidth,
            v + topBorder,
            rightBorder,
            j == yPasses ? remainderHeight : fillerHeight,
            zLevel
         );
      }
   }

   @Deprecated(
      forRemoval = true
   )
   public static void drawTexturedModalRect(GuiGraphics guiGraphics, int x, int y, int u, int v, int width, int height, float zLevel) {
      float uScale = 0.00390625F;
      float vScale = 0.00390625F;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder wr = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      Matrix4f matrix = guiGraphics.pose().last().pose();
      wr.addVertex(matrix, x, y + height, zLevel).setUv(u * 0.00390625F, (v + height) * 0.00390625F);
      wr.addVertex(matrix, x + width, y + height, zLevel).setUv((u + width) * 0.00390625F, (v + height) * 0.00390625F);
      wr.addVertex(matrix, x + width, y, zLevel).setUv((u + width) * 0.00390625F, v * 0.00390625F);
      wr.addVertex(matrix, x, y, zLevel).setUv(u * 0.00390625F, v * 0.00390625F);
      BufferUploader.drawWithShader(wr.buildOrThrow());
   }

   @Deprecated(
      forRemoval = true
   )
   public static void drawGradientRect(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
      float startAlpha = (startColor >> 24 & 0xFF) / 255.0F;
      float startRed = (startColor >> 16 & 0xFF) / 255.0F;
      float startGreen = (startColor >> 8 & 0xFF) / 255.0F;
      float startBlue = (startColor & 0xFF) / 255.0F;
      float endAlpha = (endColor >> 24 & 0xFF) / 255.0F;
      float endRed = (endColor >> 16 & 0xFF) / 255.0F;
      float endGreen = (endColor >> 8 & 0xFF) / 255.0F;
      float endBlue = (endColor & 0xFF) / 255.0F;
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.addVertex(mat, right, top, zLevel).setColor(startRed, startGreen, startBlue, startAlpha);
      buffer.addVertex(mat, left, top, zLevel).setColor(startRed, startGreen, startBlue, startAlpha);
      buffer.addVertex(mat, left, bottom, zLevel).setColor(endRed, endGreen, endBlue, endAlpha);
      buffer.addVertex(mat, right, bottom, zLevel).setColor(endRed, endGreen, endBlue, endAlpha);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void blitInscribed(
      GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int boundsWidth, int boundsHeight, int rectWidth, int rectHeight
   ) {
      blitInscribed(guiGraphics, texture, x, y, boundsWidth, boundsHeight, rectWidth, rectHeight, true, true);
   }

   public static void blitInscribed(
      GuiGraphics guiGraphics,
      ResourceLocation texture,
      int x,
      int y,
      int boundsWidth,
      int boundsHeight,
      int rectWidth,
      int rectHeight,
      boolean centerX,
      boolean centerY
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

      guiGraphics.blit(texture, x, y, boundsWidth, boundsHeight, 0.0F, 0.0F, rectWidth, rectHeight, rectWidth, rectHeight);
   }
}
