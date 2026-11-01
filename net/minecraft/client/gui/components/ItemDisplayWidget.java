package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemDisplayWidget extends AbstractWidget {
   private final Minecraft minecraft;
   private final int offsetX;
   private final int offsetY;
   private final ItemStack itemStack;
   private final boolean decorations;
   private final boolean tooltip;

   public ItemDisplayWidget(
      Minecraft p_425711_,
      int p_425622_,
      int p_425765_,
      int p_425717_,
      int p_426221_,
      Component p_426182_,
      ItemStack p_425566_,
      boolean p_425684_,
      boolean p_425583_
   ) {
      super(0, 0, p_425717_, p_426221_, p_426182_);
      this.minecraft = p_425711_;
      this.offsetX = p_425622_;
      this.offsetY = p_425765_;
      this.itemStack = p_425566_;
      this.decorations = p_425684_;
      this.tooltip = p_425583_;
   }

   @Override
   protected void renderWidget(GuiGraphics p_425745_, int p_425710_, int p_425986_, float p_425651_) {
      p_425745_.renderItem(this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, 0);
      if (this.decorations) {
         p_425745_.renderItemDecorations(this.minecraft.font, this.itemStack, this.getX() + this.offsetX, this.getY() + this.offsetY, null);
      }

      if (this.isFocused()) {
         p_425745_.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), -1);
      }

      if (this.tooltip && this.isHoveredOrFocused()) {
         this.renderTooltip(p_425745_, p_425710_, p_425986_);
      }
   }

   protected void renderTooltip(GuiGraphics p_440462_, int p_439191_, int p_439103_) {
      p_440462_.setTooltipForNextFrame(this.minecraft.font, this.itemStack, p_439191_, p_439103_);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_426258_) {
      p_426258_.add(NarratedElementType.TITLE, Component.translatable("narration.item", this.itemStack.getHoverName()));
   }
}
