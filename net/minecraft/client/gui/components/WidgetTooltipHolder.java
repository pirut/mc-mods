package net.minecraft.client.gui.components;

import java.time.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WidgetTooltipHolder {
   @Nullable
   private Tooltip tooltip;
   private Duration delay = Duration.ZERO;
   private long displayStartTime;
   private boolean wasDisplayed;

   public void setDelay(Duration p_320662_) {
      this.delay = p_320662_;
   }

   public void set(@Nullable Tooltip p_320861_) {
      this.tooltip = p_320861_;
   }

   @Nullable
   public Tooltip get() {
      return this.tooltip;
   }

   public void refreshTooltipForNextRenderPass(
      GuiGraphics p_419612_, int p_420046_, int p_419857_, boolean p_319807_, boolean p_320768_, ScreenRectangle p_320963_
   ) {
      if (this.tooltip == null) {
         this.wasDisplayed = false;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         boolean flag = p_319807_ || p_320768_ && minecraft.getLastInputType().isKeyboard();
         if (flag != this.wasDisplayed) {
            if (flag) {
               this.displayStartTime = Util.getMillis();
            }

            this.wasDisplayed = flag;
         }

         if (flag && Util.getMillis() - this.displayStartTime > this.delay.toMillis()) {
            p_419612_.setTooltipForNextFrame(
               minecraft.font,
               this.tooltip.toCharSequence(minecraft),
               this.createTooltipPositioner(p_320963_, p_319807_, p_320768_),
               p_420046_,
               p_419857_,
               p_320768_
            );
         }
      }
   }

   private ClientTooltipPositioner createTooltipPositioner(ScreenRectangle p_320471_, boolean p_320558_, boolean p_320200_) {
      return (ClientTooltipPositioner)(!p_320558_ && p_320200_ && Minecraft.getInstance().getLastInputType().isKeyboard()
         ? new BelowOrAboveWidgetTooltipPositioner(p_320471_)
         : new MenuTooltipPositioner(p_320471_));
   }

   public void updateNarration(NarrationElementOutput p_319926_) {
      if (this.tooltip != null) {
         this.tooltip.updateNarration(p_319926_);
      }
   }
}
