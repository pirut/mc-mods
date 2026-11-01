package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class ShieldDecorationRecipe extends CustomRecipe {
   public ShieldDecorationRecipe(CraftingBookCategory p_251065_) {
      super(p_251065_);
   }

   public boolean matches(CraftingInput p_344969_, Level p_44309_) {
      ItemStack $$2 = ItemStack.EMPTY;
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < p_344969_.size(); $$4++) {
         ItemStack $$5 = p_344969_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.getItem() instanceof BannerItem) {
               if (!$$3.isEmpty()) {
                  return false;
               }

               $$3 = $$5;
            } else {
               if (!$$5.is(Items.SHIELD)) {
                  return false;
               }

               if (!$$2.isEmpty()) {
                  return false;
               }

               BannerPatternLayers $$6 = $$5.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
               if (!$$6.layers().isEmpty()) {
                  return false;
               }

               $$2 = $$5;
            }
         }
      }

      return !$$2.isEmpty() && !$$3.isEmpty();
   }

   public ItemStack assemble(CraftingInput p_345351_, HolderLookup.Provider p_335895_) {
      ItemStack $$2 = ItemStack.EMPTY;
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < p_345351_.size(); $$4++) {
         ItemStack $$5 = p_345351_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.getItem() instanceof BannerItem) {
               $$2 = $$5;
            } else if ($$5.is(Items.SHIELD)) {
               $$3 = $$5.copy();
            }
         }
      }

      if ($$3.isEmpty()) {
         return $$3;
      } else {
         $$3.set(DataComponents.BANNER_PATTERNS, $$2.get(DataComponents.BANNER_PATTERNS));
         $$3.set(DataComponents.BASE_COLOR, ((BannerItem)$$2.getItem()).getColor());
         return $$3;
      }
   }

   @Override
   public boolean canCraftInDimensions(int p_44298_, int p_44299_) {
      return p_44298_ * p_44299_ >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHIELD_DECORATION;
   }
}
