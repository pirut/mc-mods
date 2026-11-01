package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlacementInfo {
   public static final int EMPTY_SLOT = -1;
   public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), IntList.of());
   private final List<Ingredient> ingredients;
   private final IntList slotsToIngredientIndex;

   private PlacementInfo(List<Ingredient> p_364604_, IntList p_389415_) {
      this.ingredients = p_364604_;
      this.slotsToIngredientIndex = p_389415_;
   }

   public static PlacementInfo create(Ingredient p_363622_) {
      return p_363622_.isEmpty() ? NOT_PLACEABLE : new PlacementInfo(List.of(p_363622_), IntList.of(0));
   }

   public static PlacementInfo createFromOptionals(List<Optional<Ingredient>> p_361135_) {
      int i = p_361135_.size();
      List<Ingredient> list = new ArrayList<>(i);
      IntList intlist = new IntArrayList(i);
      int j = 0;

      for (Optional<Ingredient> optional : p_361135_) {
         if (optional.isPresent()) {
            Ingredient ingredient = optional.get();
            if (ingredient.isEmpty()) {
               return NOT_PLACEABLE;
            }

            list.add(ingredient);
            intlist.add(j++);
         } else {
            intlist.add(-1);
         }
      }

      return new PlacementInfo(list, intlist);
   }

   public static PlacementInfo create(List<Ingredient> p_364524_) {
      int i = p_364524_.size();
      IntList intlist = new IntArrayList(i);

      for (int j = 0; j < i; j++) {
         Ingredient ingredient = p_364524_.get(j);
         if (ingredient.isEmpty()) {
            return NOT_PLACEABLE;
         }

         intlist.add(j);
      }

      return new PlacementInfo(p_364524_, intlist);
   }

   public IntList slotsToIngredientIndex() {
      return this.slotsToIngredientIndex;
   }

   public List<Ingredient> ingredients() {
      return this.ingredients;
   }

   public boolean isImpossibleToPlace() {
      return this.slotsToIngredientIndex.isEmpty();
   }
}
