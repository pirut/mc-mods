package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapCloningRecipe extends CustomRecipe {
   public MapCloningRecipe(CraftingBookCategory p_251985_) {
      super(p_251985_);
   }

   public boolean matches(CraftingInput p_345914_, Level p_43981_) {
      int $$2 = 0;
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < p_345914_.size(); $$4++) {
         ItemStack $$5 = p_345914_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(Items.FILLED_MAP)) {
               if (!$$3.isEmpty()) {
                  return false;
               }

               $$3 = $$5;
            } else {
               if (!$$5.is(Items.MAP)) {
                  return false;
               }

               $$2++;
            }
         }
      }

      return !$$3.isEmpty() && $$2 > 0;
   }

   public ItemStack assemble(CraftingInput p_344908_, HolderLookup.Provider p_335660_) {
      int $$2 = 0;
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < p_344908_.size(); $$4++) {
         ItemStack $$5 = p_344908_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(Items.FILLED_MAP)) {
               if (!$$3.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               $$3 = $$5;
            } else {
               if (!$$5.is(Items.MAP)) {
                  return ItemStack.EMPTY;
               }

               $$2++;
            }
         }
      }

      return !$$3.isEmpty() && $$2 >= 1 ? $$3.copyWithCount($$2 + 1) : ItemStack.EMPTY;
   }

   @Override
   public boolean canCraftInDimensions(int p_43970_, int p_43971_) {
      return p_43970_ >= 3 && p_43971_ >= 3;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.MAP_CLONING;
   }
}
