package net.neoforged.neoforge.common.brewing;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record BrewingRecipeRegistry(List<IBrewingRecipe> recipes) {
   public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
      if (!input.isEmpty() && input.getCount() == 1) {
         if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            for (IBrewingRecipe recipe : this.recipes) {
               ItemStack output = recipe.getOutput(input, ingredient);
               if (!output.isEmpty()) {
                  return output;
               }
            }

            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean hasOutput(ItemStack input, ItemStack ingredient) {
      return !this.getOutput(input, ingredient).isEmpty();
   }

   public boolean isValidIngredient(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         for (IBrewingRecipe recipe : this.recipes) {
            if (recipe.isIngredient(stack)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isValidInput(ItemStack stack) {
      for (IBrewingRecipe recipe : this.recipes) {
         if (recipe.isInput(stack)) {
            return true;
         }
      }

      return false;
   }
}
