package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TippedArrowRecipe extends CustomRecipe {
   public TippedArrowRecipe(CraftingBookCategory p_252163_) {
      super(p_252163_);
   }

   public boolean matches(CraftingInput p_345143_, Level p_44516_) {
      if (p_345143_.width() == 3 && p_345143_.height() == 3) {
         for (int $$2 = 0; $$2 < p_345143_.height(); $$2++) {
            for (int $$3 = 0; $$3 < p_345143_.width(); $$3++) {
               ItemStack $$4 = p_345143_.getItem($$3, $$2);
               if ($$4.isEmpty()) {
                  return false;
               }

               if ($$3 == 1 && $$2 == 1) {
                  if (!$$4.is(Items.LINGERING_POTION)) {
                     return false;
                  }
               } else if (!$$4.is(Items.ARROW)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public ItemStack assemble(CraftingInput p_345174_, HolderLookup.Provider p_335792_) {
      ItemStack $$2 = p_345174_.getItem(1, 1);
      if (!$$2.is(Items.LINGERING_POTION)) {
         return ItemStack.EMPTY;
      } else {
         ItemStack $$3 = new ItemStack(Items.TIPPED_ARROW, 8);
         $$3.set(DataComponents.POTION_CONTENTS, $$2.get(DataComponents.POTION_CONTENTS));
         return $$3;
      }
   }

   @Override
   public boolean canCraftInDimensions(int p_44505_, int p_44506_) {
      return p_44505_ >= 3 && p_44506_ >= 3;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.TIPPED_ARROW;
   }
}
