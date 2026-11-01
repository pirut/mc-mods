package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableList.Builder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

@Environment(EnvType.CLIENT)
public class ClientRecipeBook extends RecipeBook {
   private final Map<RecipeDisplayId, RecipeDisplayEntry> known = new HashMap<>();
   private final Set<RecipeDisplayId> highlight = new HashSet<>();
   private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab = Map.of();
   private List<RecipeCollection> allCollections = List.of();

   public void add(RecipeDisplayEntry recipe) {
      this.known.put(recipe.id(), recipe);
   }

   public void remove(RecipeDisplayId recipe) {
      this.known.remove(recipe);
      this.highlight.remove(recipe);
   }

   public void clear() {
      this.known.clear();
      this.highlight.clear();
   }

   public boolean willHighlight(RecipeDisplayId recipe) {
      return this.highlight.contains(recipe);
   }

   public void removeHighlight(RecipeDisplayId recipe) {
      this.highlight.remove(recipe);
   }

   public void addHighlight(RecipeDisplayId recipe) {
      this.highlight.add(recipe);
   }

   public void rebuildCollections() {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = categorizeAndGroupRecipes(this.known.values());
      Map<ExtendedRecipeBookCategory, List<RecipeCollection>> map2 = new HashMap<>();
      Builder<RecipeCollection> builder = ImmutableList.builder();
      map.forEach(
         (recipeBookCategory, list) -> map2.put(
            recipeBookCategory, (List)list.stream().map(RecipeCollection::new).peek(builder::add).collect(ImmutableList.toImmutableList())
         )
      );

      for (SearchRecipeBookCategory searchRecipeBookCategory : SearchRecipeBookCategory.values()) {
         map2.put(
            searchRecipeBookCategory,
            searchRecipeBookCategory.includedCategories()
               .stream()
               .flatMap(recipeBookCategory -> map2.getOrDefault(recipeBookCategory, List.of()).stream())
               .collect(ImmutableList.toImmutableList())
         );
      }

      this.collectionsByTab = Map.copyOf(map2);
      this.allCollections = builder.build();
   }

   private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> categorizeAndGroupRecipes(Iterable<RecipeDisplayEntry> recipes) {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = new HashMap<>();
      Table<RecipeBookCategory, Integer, List<RecipeDisplayEntry>> table = HashBasedTable.create();

      for (RecipeDisplayEntry recipeDisplayEntry : recipes) {
         RecipeBookCategory recipeBookCategory = recipeDisplayEntry.category();
         OptionalInt optionalInt = recipeDisplayEntry.group();
         if (optionalInt.isEmpty()) {
            map.computeIfAbsent(recipeBookCategory, recipeBookCategoryx -> new ArrayList<>()).add(List.of(recipeDisplayEntry));
         } else {
            List<RecipeDisplayEntry> list = (List<RecipeDisplayEntry>)table.get(recipeBookCategory, optionalInt.getAsInt());
            if (list == null) {
               list = new ArrayList<>();
               table.put(recipeBookCategory, optionalInt.getAsInt(), list);
               map.computeIfAbsent(recipeBookCategory, recipeBookCategoryx -> new ArrayList<>()).add(list);
            }

            list.add(recipeDisplayEntry);
         }
      }

      return map;
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(ExtendedRecipeBookCategory category) {
      return this.collectionsByTab.getOrDefault(category, Collections.emptyList());
   }
}
