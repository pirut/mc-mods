package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipeHelper {
   static <T> void placeRecipe(int width, int height, Recipe<?> recipe, Iterable<T> ingredients, PlaceRecipeHelper.Output<T> output) {
      if (recipe instanceof ShapedRecipe shapedRecipe) {
         placeRecipe(width, height, shapedRecipe.getWidth(), shapedRecipe.getHeight(), ingredients, output);
      } else {
         placeRecipe(width, height, width, height, ingredients, output);
      }
   }

   static <T> void placeRecipe(int gridWidth, int gridHeight, int width, int height, Iterable<T> ingredients, PlaceRecipeHelper.Output<T> output) {
      Iterator<T> iterator = ingredients.iterator();
      int i = 0;

      for (int j = 0; j < gridHeight; j++) {
         boolean bl = height < gridHeight / 2.0F;
         int k = Mth.floor(gridHeight / 2.0F - height / 2.0F);
         if (bl && k > j) {
            i += gridWidth;
            j++;
         }

         for (int l = 0; l < gridWidth; l++) {
            if (!iterator.hasNext()) {
               return;
            }

            bl = width < gridWidth / 2.0F;
            k = Mth.floor(gridWidth / 2.0F - width / 2.0F);
            int m = width;
            boolean bl2 = l < width;
            if (bl) {
               m = k + width;
               bl2 = k <= l && l < k + width;
            }

            if (bl2) {
               output.addItemToSlot(iterator.next(), i, l, j);
            } else if (m == l) {
               i += gridWidth - l;
               break;
            }

            i++;
         }
      }
   }

   @FunctionalInterface
   public interface Output<T> {
      void addItemToSlot(T object, int i, int j, int k);
   }
}
