package com.mojang.realmsclient.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RowButton {
   public final int width;
   public final int height;
   public final int xOffset;
   public final int yOffset;

   public RowButton(int p_88012_, int p_88013_, int p_88014_, int p_88015_) {
      this.width = p_88012_;
      this.height = p_88013_;
      this.xOffset = p_88014_;
      this.yOffset = p_88015_;
   }

   public void drawForRowAt(GuiGraphics p_281584_, int p_88020_, int p_88021_, int p_88022_, int p_88023_) {
      int $$5 = p_88020_ + this.xOffset;
      int $$6 = p_88021_ + this.yOffset;
      boolean $$7 = p_88022_ >= $$5 && p_88022_ <= $$5 + this.width && p_88023_ >= $$6 && p_88023_ <= $$6 + this.height;
      this.draw(p_281584_, $$5, $$6, $$7);
   }

   protected abstract void draw(GuiGraphics var1, int var2, int var3, boolean var4);

   public int getRight() {
      return this.xOffset + this.width;
   }

   public int getBottom() {
      return this.yOffset + this.height;
   }

   public abstract void onClick(int var1);

   public static void drawButtonsInRow(
      GuiGraphics p_281401_, List<RowButton> p_283164_, RealmsObjectSelectionList<?> p_282348_, int p_282527_, int p_281326_, int p_281575_, int p_282538_
   ) {
      for (RowButton $$7 : p_283164_) {
         if (p_282348_.getRowWidth() > $$7.getRight()) {
            $$7.drawForRowAt(p_281401_, p_282527_, p_281326_, p_281575_, p_282538_);
         }
      }
   }

   public static void rowButtonMouseClicked(
      RealmsObjectSelectionList<?> p_88037_, ObjectSelectionList.Entry<?> p_88038_, List<RowButton> p_88039_, int p_88040_, double p_88041_, double p_88042_
   ) {
      int $$6 = p_88037_.children().indexOf(p_88038_);
      if ($$6 > -1) {
         p_88037_.selectItem($$6);
         int $$7 = p_88037_.getRowLeft();
         int $$8 = p_88037_.getRowTop($$6);
         int $$9 = (int)(p_88041_ - $$7);
         int $$10 = (int)(p_88042_ - $$8);

         for (RowButton $$11 : p_88039_) {
            if ($$9 >= $$11.xOffset && $$9 <= $$11.getRight() && $$10 >= $$11.yOffset && $$10 <= $$11.getBottom()) {
               $$11.onClick($$6);
            }
         }
      }
   }
}
