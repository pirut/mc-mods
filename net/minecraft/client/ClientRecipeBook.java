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
import java.util.Map.Entry;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.RecipeBookManager;

@OnlyIn(Dist.CLIENT)
public class ClientRecipeBook extends RecipeBook {
   private final Map<RecipeDisplayId, RecipeDisplayEntry> known = new HashMap<>();
   private final Set<RecipeDisplayId> highlight = new HashSet<>();
   private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab = Map.of();
   private List<RecipeCollection> allCollections = List.of();

   public void add(RecipeDisplayEntry p_380259_) {
      this.known.put(p_380259_.id(), p_380259_);
   }

   public void remove(RecipeDisplayId p_379557_) {
      this.known.remove(p_379557_);
      this.highlight.remove(p_379557_);
   }

   public void clear() {
      this.known.clear();
      this.highlight.clear();
   }

   public boolean willHighlight(RecipeDisplayId p_379469_) {
      return this.highlight.contains(p_379469_);
   }

   public void removeHighlight(RecipeDisplayId p_379652_) {
      this.highlight.remove(p_379652_);
   }

   public void addHighlight(RecipeDisplayId p_380362_) {
      this.highlight.add(p_380362_);
   }

   public void rebuildCollections() {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = categorizeAndGroupRecipes(this.known.values());
      Map<ExtendedRecipeBookCategory, List<RecipeCollection>> map1 = new HashMap<>();
      Builder<RecipeCollection> builder = ImmutableList.builder();
      map.forEach(
         (p_380933_, p_378767_) -> map1.put(
            p_380933_, (List)p_378767_.stream().map(RecipeCollection::new).peek(builder::add).collect(ImmutableList.toImmutableList())
         )
      );

      for (SearchRecipeBookCategory searchrecipebookcategory : SearchRecipeBookCategory.values()) {
         map1.put(
            searchrecipebookcategory,
            searchrecipebookcategory.includedCategories()
               .stream()
               .flatMap(p_380954_ -> map1.getOrDefault(p_380954_, List.of()).stream())
               .collect(ImmutableList.toImmutableList())
         );
      }

      for (Entry<ExtendedRecipeBookCategory, List<RecipeBookCategory>> entry : RecipeBookManager.getSearchCategories().entrySet()) {
         map1.put(
            entry.getKey(),
            entry.getValue().stream().flatMap(category -> map1.getOrDefault(category, List.of()).stream()).collect(ImmutableList.toImmutableList())
         );
      }

      this.collectionsByTab = Map.copyOf(map1);
      this.allCollections = builder.build();
   }

   private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> categorizeAndGroupRecipes(Iterable<RecipeDisplayEntry> p_90643_) {
      Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = new HashMap<>();
      Table<RecipeBookCategory, Integer, List<RecipeDisplayEntry>> table = HashBasedTable.create();

      for (RecipeDisplayEntry recipedisplayentry : p_90643_) {
         RecipeBookCategory recipebookcategory = recipedisplayentry.category();
         OptionalInt optionalint = recipedisplayentry.group();
         if (optionalint.isEmpty()) {
            map.computeIfAbsent(recipebookcategory, p_381000_ -> new ArrayList<>()).add(List.of(recipedisplayentry));
         } else {
            List<RecipeDisplayEntry> list = (List<RecipeDisplayEntry>)table.get(recipebookcategory, optionalint.getAsInt());
            if (list == null) {
               list = new ArrayList<>();
               table.put(recipebookcategory, optionalint.getAsInt(), list);
               map.computeIfAbsent(recipebookcategory, p_380998_ -> new ArrayList<>()).add(list);
            }

            list.add(recipedisplayentry);
         }
      }

      return map;
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(ExtendedRecipeBookCategory p_380958_) {
      return this.collectionsByTab.getOrDefault(p_380958_, Collections.emptyList());
   }
}
