package net.neoforged.neoforge.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterRecipeBookSearchCategoriesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class RecipeBookManager {
   private static Map<ExtendedRecipeBookCategory, List<RecipeBookCategory>> searchCategories = Map.of();

   public static Map<ExtendedRecipeBookCategory, List<RecipeBookCategory>> getSearchCategories() {
      return searchCategories;
   }

   @Internal
   public static void init() {
      IdentityHashMap<ExtendedRecipeBookCategory, List<RecipeBookCategory>> searchCategories = new IdentityHashMap<>();
      RegisterRecipeBookSearchCategoriesEvent event = new RegisterRecipeBookSearchCategoriesEvent(searchCategories);
      ModLoader.postEvent(event);
      RecipeBookManager.searchCategories = Collections.unmodifiableMap(searchCategories);
   }
}
