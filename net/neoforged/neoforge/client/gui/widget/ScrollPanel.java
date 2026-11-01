package net.neoforged.neoforge.client.gui.widget;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

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

   protected void drawBackground(GuiGraphics guiGraphics, float partialTick) {
      Screen.renderMenuBackgroundTexture(guiGraphics, Screen.MENU_BACKGROUND, this.left, this.top, 0.0F, 0.0F, this.width, this.height);
   }

   protected abstract void drawPanel(GuiGraphics var1, int var2, int var3, int var4, int var5);

   protected boolean clickPanel(double mouseX, double mouseY, MouseButtonEvent event) {
      return false;
   }

   private int getMaxScroll() {
      return this.getContentHeight() - (this.height - this.border);
   }

   private void applyScrollLimits() {
      int max = this.getMaxScroll();
      if (max < 0) {
         max = 0;
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
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (super.mouseClicked(event, doubleClick)) {
         return true;
      } else {
         this.scrolling = event.button() == 0 && event.x() >= this.barLeft && event.x() < this.right && event.y() >= this.top && event.y() < this.bottom;
         if (this.scrolling) {
            return true;
         } else {
            int mouseListY = (int)event.y() - this.top - this.getContentHeight() + (int)this.scrollDistance - this.border;
            return event.x() >= this.left && event.x() < this.right && mouseListY < 0
               ? this.clickPanel(event.x() - this.left, event.y() - this.top + (int)this.scrollDistance - this.border, event)
               : false;
         }
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (super.mouseReleased(event)) {
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
   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
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
      guiGraphics.enableScissor(this.left, this.top, this.right, this.bottom);
      this.drawBackground(guiGraphics, partialTick);
      int baseY = this.top + this.border - (int)this.scrollDistance;
      this.drawPanel(guiGraphics, this.right, baseY, mouseX, mouseY);
      int extraHeight = this.getContentHeight() + this.border - this.height;
      if (extraHeight > 0) {
         int barHeight = this.getBarHeight();
         int barTop = (int)this.scrollDistance * (this.height - barHeight) / extraHeight + this.top;
         if (barTop < this.top) {
            barTop = this.top;
         }

         guiGraphics.fill(this.barLeft, this.top, this.barLeft + this.barWidth, this.bottom, this.barBgColor);
         guiGraphics.fill(this.barLeft, barTop, this.barLeft + this.barWidth, barTop + barHeight, this.barColor);
         guiGraphics.fill(this.barLeft, barTop, this.barLeft + this.barWidth - 1, barTop + barHeight - 1, this.barBorderColor);
      }

      guiGraphics.disableScissor();
   }

   protected void drawGradientRect(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color1, int color2) {
      guiGraphics.fillGradient(left, top, right, bottom, color1, color2);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return Collections.emptyList();
   }
}
