package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

public class ArmorDyeRecipe extends CustomRecipe {
   public ArmorDyeRecipe(CraftingBookCategory p_251949_) {
      super(p_251949_);
   }

   public boolean matches(CraftingInput p_344736_, Level p_43770_) {
      ItemStack $$2 = ItemStack.EMPTY;
      List<ItemStack> $$3 = Lists.newArrayList();

      for (int $$4 = 0; $$4 < p_344736_.size(); $$4++) {
         ItemStack $$5 = p_344736_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(ItemTags.DYEABLE)) {
               if (!$$2.isEmpty()) {
                  return false;
               }

               $$2 = $$5;
            } else {
               if (!($$5.getItem() instanceof DyeItem)) {
                  return false;
               }

               $$3.add($$5);
            }
         }
      }

      return !$$2.isEmpty() && !$$3.isEmpty();
   }

   public ItemStack assemble(CraftingInput p_344909_, HolderLookup.Provider p_335722_) {
      List<DyeItem> $$2 = Lists.newArrayList();
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < p_344909_.size(); $$4++) {
         ItemStack $$5 = p_344909_.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(ItemTags.DYEABLE)) {
               if (!$$3.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               $$3 = $$5.copy();
            } else {
               if (!($$5.getItem() instanceof DyeItem $$6)) {
                  return ItemStack.EMPTY;
               }

               $$2.add($$6);
            }
         }
      }

      return !$$3.isEmpty() && !$$2.isEmpty() ? DyedItemColor.applyDyes($$3, $$2) : ItemStack.EMPTY;
   }

   @Override
   public boolean canCraftInDimensions(int p_43759_, int p_43760_) {
      return p_43759_ * p_43760_ >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.ARMOR_DYE;
   }
}
