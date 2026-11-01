package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
   private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

   public FireworkStarFadeRecipe(CraftingBookCategory p_251846_) {
      super(p_251846_);
   }

   public boolean matches(CraftingInput p_346021_, Level p_43874_) {
      boolean $$2 = false;
      boolean $$3 = false;

      for (int $$4 = 0; $$4 < p_346021_.size(); $$4++) {
         ItemStack $$5 = p_346021_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.getItem() instanceof DyeItem) {
               $$2 = true;
            } else {
               if (!STAR_INGREDIENT.test($$5)) {
                  return false;
               }

               if ($$3) {
                  return false;
               }

               $$3 = true;
            }
         }
      }

      return $$3 && $$2;
   }

   public ItemStack assemble(CraftingInput p_346017_, HolderLookup.Provider p_335705_) {
      IntList $$2 = new IntArrayList();
      ItemStack $$3 = null;

      for (int $$4 = 0; $$4 < p_346017_.size(); $$4++) {
         ItemStack $$5 = p_346017_.getItem($$4);
         Item $$6 = $$5.getItem();
         if ($$6 instanceof DyeItem) {
            $$2.add(((DyeItem)$$6).getDyeColor().getFireworkColor());
         } else if (STAR_INGREDIENT.test($$5)) {
            $$3 = $$5.copyWithCount(1);
         }
      }

      if ($$3 != null && !$$2.isEmpty()) {
         $$3.update(DataComponents.FIREWORK_EXPLOSION, FireworkExplosion.DEFAULT, $$2, FireworkExplosion::withFadeColors);
         return $$3;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public boolean canCraftInDimensions(int p_43863_, int p_43864_) {
      return p_43863_ * p_43864_ >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR_FADE;
   }
}
