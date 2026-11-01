package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipe<T> {
   default void placeRecipe(int p_135409_, int p_135410_, int p_135411_, RecipeHolder<?> p_301225_, Iterator<T> p_135413_, int p_135414_) {
      int $$6 = p_135409_;
      int $$7 = p_135410_;
      if (p_301225_.value() instanceof ShapedRecipe $$9) {
         $$6 = $$9.getWidth();
         $$7 = $$9.getHeight();
      }

      int $$10 = 0;

      for (int $$11 = 0; $$11 < p_135410_; $$11++) {
         if ($$10 == p_135411_) {
            $$10++;
         }

         boolean $$12 = $$7 < p_135410_ / 2.0F;
         int $$13 = Mth.floor(p_135410_ / 2.0F - $$7 / 2.0F);
         if ($$12 && $$13 > $$11) {
            $$10 += p_135409_;
            $$11++;
         }

         for (int $$14 = 0; $$14 < p_135409_; $$14++) {
            if (!p_135413_.hasNext()) {
               return;
            }

            $$12 = $$6 < p_135409_ / 2.0F;
            $$13 = Mth.floor(p_135409_ / 2.0F - $$6 / 2.0F);
            int $$15 = $$6;
            boolean $$16 = $$14 < $$6;
            if ($$12) {
               $$15 = $$13 + $$6;
               $$16 = $$13 <= $$14 && $$14 < $$13 + $$6;
            }

            if ($$16) {
               this.addItemToSlot(p_135413_.next(), $$10, p_135414_, $$14, $$11);
            } else if ($$15 == $$14) {
               $$10 += p_135409_ - $$14;
               break;
            }

            $$10++;
         }
      }
   }

   void addItemToSlot(T var1, int var2, int var3, int var4, int var5);
}
