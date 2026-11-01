package net.neoforged.neoforge.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;

public abstract class ScrollPanel extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
   private final Minecraft client;
   protected final int width;
   protected final int height;
   protected final int top;
   protected final int bottom;
   protected final int right;
   protected final int left;
   private boolean scrolling;
   protected float scrollDistance;
   protected boolean captureMouse = true;
   protected final int border;
   private final int barWidth;
   private final int barLeft;
   private final int barBgColor;
   private final int barColor;
   private final int barBorderColor;

   public ScrollPanel(Minecraft client, int width, int height, int top, int left) {
      this(client, width, height, top, left, 4);
   }

   public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border) {
      this(client, width, height, top, left, border, 6);
   }

   public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth) {
      this(client, width, height, top, left, border, barWidth, -16777216, -8355712, -4144960);
   }

   public ScrollPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int barBgColor, int barColor, int barBorderColor) {
      this.client = client;
      this.width = width;
      this.height = height;
      this.top = top;
      this.left = left;
      this.bottom = height + this.top;
      this.right = width + this.left;
      this.barLeft = this.left + this.width - barWidth;
      this.border = border;
      this.barWidth = barWidth;
      this.barBgColor = barBgColor;
      this.barColor = barColor;
      this.barBorderColor = barBorderColor;
   }

   protected abstract int getContentHeight();

   protected void drawBackground(GuiGraphics guiGraphics, Tesselator tess, float partialTick) {
      Screen.renderMenuBackgroundTexture(guiGraphics, Screen.MENU_BACKGROUND, this.left, this.top, 0.0F, 0.0F, this.width, this.height);
   }

   protected abstract void drawPanel(GuiGraphics var1, int var2, int var3, Tesselator var4, int var5, int var6);

   protected boolean clickPanel(double mouseX, double mouseY, int button) {
      return false;
   }

   private int getMaxScroll() {
      return this.getContentHeight() - (this.height - this.border);
   }

   private void applyScrollLimits() {
      int max = this.getMaxScroll();
      if (max < 0) {
         max /= 2;
      }

      if (this.scrollDistance < 0.0F) {
         this.scrollDistance = 0.0F;
      }

      if (this.scrollDistance > max) {
         this.scrollDistance = max;
      }
   }

   @Override
   public boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_, double p_294830_) {
      if (p_294830_ != 0.0) {
         this.scrollDistance = this.scrollDistance + (float)(-p_294830_ * this.getScrollAmount());
         this.applyScrollLimits();
         return true;
      } else {
         return false;
      }
   }

   protected int getScrollAmount() {
      return 20;
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return mouseX >= this.left && mouseX < this.right && mouseY >= this.top && mouseY < this.bottom;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else {
         this.scrolling = button == 0 && mouseX >= this.barLeft && mouseX < this.right && mouseY >= this.top && mouseY < this.bottom;
         if (this.scrolling) {
            return true;
         } else {
            int mouseListY = (int)mouseY - this.top - this.getContentHeight() + (int)this.scrollDistance - this.border;
            return mouseX >= this.left && mouseX < this.right && mouseListY < 0
               ? this.clickPanel(mouseX - this.left, mouseY - this.top + (int)this.scrollDistance - this.border, button)
               : false;
         }
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (super.mouseReleased(mouseX, mouseY, button)) {
         return true;
      } else {
         boolean ret = this.scrolling;
         this.scrolling = false;
         return ret;
      }
   }

   private int getBarHeight() {
      int barHeight = this.height * this.height / this.getContentHeight();
      if (barHeight < 32) {
         barHeight = 32;
      }

      if (barHeight > this.height - this.border * 2) {
         barHeight = this.height - this.border * 2;
      }

      return barHeight;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.scrolling) {
         int maxScroll = this.height - this.getBarHeight();
         double moved = deltaY / maxScroll;
         this.scrollDistance = (float)(this.scrollDistance + this.getMaxScroll() * moved);
         this.applyScrollLimits();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Tesselator tess = Tesselator.getInstance();
      double scale = this.client.getWindow().getGuiScale();
      RenderSystem.enableScissor(
         (int)(this.left * scale), (int)(this.client.getWindow().getHeight() - this.bottom * scale), (int)(this.width * scale), (int)(this.height * scale)
      );
      this.drawBackground(guiGraphics, tess, partialTick);
      int baseY = this.top + this.border - (int)this.scrollDistance;
      this.drawPanel(guiGraphics, this.right, baseY, tess, mouseX, mouseY);
      RenderSystem.disableDepthTest();
      int extraHeight = this.getContentHeight() + this.border - this.height;
      if (extraHeight > 0) {
         int barHeight = this.getBarHeight();
         int barTop = (int)this.scrollDistance * (this.height - barHeight) / extraHeight + this.top;
         if (barTop < this.top) {
            barTop = this.top;
         }

         int barBgAlpha = this.barBgColor >> 24 & 0xFF;
         int barBgRed = this.barBgColor >> 16 & 0xFF;
         int barBgGreen = this.barBgColor >> 8 & 0xFF;
         int barBgBlue = this.barBgColor & 0xFF;
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         BufferBuilder worldr = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         worldr.addVertex(this.barLeft, this.bottom, 0.0F).setColor(barBgRed, barBgGreen, barBgBlue, barBgAlpha);
         worldr.addVertex(this.barLeft + this.barWidth, this.bottom, 0.0F).setColor(barBgRed, barBgGreen, barBgBlue, barBgAlpha);
         worldr.addVertex(this.barLeft + this.barWidth, this.top, 0.0F).setColor(barBgRed, barBgGreen, barBgBlue, barBgAlpha);
         worldr.addVertex(this.barLeft, this.top, 0.0F).setColor(barBgRed, barBgGreen, barBgBlue, barBgAlpha);
         BufferUploader.drawWithShader(worldr.buildOrThrow());
         int barAlpha = this.barColor >> 24 & 0xFF;
         int barRed = this.barColor >> 16 & 0xFF;
         int barGreen = this.barColor >> 8 & 0xFF;
         int barBlue = this.barColor & 0xFF;
         worldr = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         worldr.addVertex(this.barLeft, barTop + barHeight, 0.0F).setColor(barRed, barGreen, barBlue, barAlpha);
         worldr.addVertex(this.barLeft + this.barWidth, barTop + barHeight, 0.0F).setColor(barRed, barGreen, barBlue, barAlpha);
         worldr.addVertex(this.barLeft + this.barWidth, barTop, 0.0F).setColor(barRed, barGreen, barBlue, barAlpha);
         worldr.addVertex(this.barLeft, barTop, 0.0F).setColor(barRed, barGreen, barBlue, barAlpha);
         BufferUploader.drawWithShader(worldr.buildOrThrow());
         int barBorderAlpha = this.barBorderColor >> 24 & 0xFF;
         int barBorderRed = this.barBorderColor >> 16 & 0xFF;
         int barBorderGreen = this.barBorderColor >> 8 & 0xFF;
         int barBorderBlue = this.barBorderColor & 0xFF;
         worldr = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         worldr.addVertex(this.barLeft, barTop + barHeight - 1, 0.0F).setColor(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha);
         worldr.addVertex(this.barLeft + this.barWidth - 1, barTop + barHeight - 1, 0.0F).setColor(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha);
         worldr.addVertex(this.barLeft + this.barWidth - 1, barTop, 0.0F).setColor(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha);
         worldr.addVertex(this.barLeft, barTop, 0.0F).setColor(barBorderRed, barBorderGreen, barBorderBlue, barBorderAlpha);
         BufferUploader.drawWithShader(worldr.buildOrThrow());
      }

      RenderSystem.disableBlend();
      RenderSystem.disableScissor();
   }

   protected void drawGradientRect(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color1, int color2) {
      guiGraphics.fillGradient(left, top, right, bottom, color1, color2);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return Collections.emptyList();
   }
}
