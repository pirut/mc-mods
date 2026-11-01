package net.neoforged.neoforge.client.event;

import java.util.Set;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RecipesReceivedEvent extends Event {
   private final Set<RecipeType<?>> recipeTypes;
   private final RecipeMap recipeMap;

   @Internal
   public RecipesReceivedEvent(Set<RecipeType<?>> recipeTypes, RecipeMap recipeMap) {
      this.recipeTypes = Set.copyOf(recipeTypes);
      this.recipeMap = recipeMap;
   }

   public Set<RecipeType<?>> getRecipeTypes() {
      return this.recipeTypes;
   }

   public RecipeMap getRecipeMap() {
      return this.recipeMap;
   }
}
