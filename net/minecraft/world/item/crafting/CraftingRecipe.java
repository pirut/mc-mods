package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface CraftingRecipe extends Recipe<CraftingInput> {
   @Override
   default RecipeType<CraftingRecipe> getType() {
      return RecipeType.CRAFTING;
   }

   @Override
   RecipeSerializer<? extends CraftingRecipe> getSerializer();

   CraftingBookCategory category();

   default NonNullList<ItemStack> getRemainingItems(CraftingInput p_380110_) {
      return defaultCraftingReminder(p_380110_);
   }

   static NonNullList<ItemStack> defaultCraftingReminder(CraftingInput p_380223_) {
      NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_380223_.size(), ItemStack.EMPTY);

      for (int i = 0; i < nonnulllist.size(); i++) {
         ItemStack item = p_380223_.getItem(i);
         nonnulllist.set(i, item.getCraftingRemainder());
      }

      return nonnulllist;
   }

   @Override
   default RecipeBookCategory recipeBookCategory() {
      return switch (this.category()) {
         case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
         case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
         case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
         case MISC -> RecipeBookCategories.CRAFTING_MISC;
      };
   }
}
