package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceKey<RecipePropertySet>, RecipeManager.IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(
      RecipePropertySet.SMITHING_ADDITION,
      recipe -> recipe instanceof SmithingRecipe smithingRecipe ? smithingRecipe.additionIngredient() : Optional.empty(),
      RecipePropertySet.SMITHING_BASE,
      recipe -> recipe instanceof SmithingRecipe smithingRecipe ? Optional.of(smithingRecipe.baseIngredient()) : Optional.empty(),
      RecipePropertySet.SMITHING_TEMPLATE,
      recipe -> recipe instanceof SmithingRecipe smithingRecipe ? smithingRecipe.templateIngredient() : Optional.empty(),
      RecipePropertySet.FURNACE_INPUT,
      forSingleInput(RecipeType.SMELTING),
      RecipePropertySet.BLAST_FURNACE_INPUT,
      forSingleInput(RecipeType.BLASTING),
      RecipePropertySet.SMOKER_INPUT,
      forSingleInput(RecipeType.SMOKING),
      RecipePropertySet.CAMPFIRE_INPUT,
      forSingleInput(RecipeType.CAMPFIRE_COOKING)
   );
   private static final FileToIdConverter RECIPE_LISTER = FileToIdConverter.registry(Registries.RECIPE);
   private final HolderLookup.Provider registries;
   private RecipeMap recipes = RecipeMap.EMPTY;
   private Map<ResourceKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
   private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes = SelectableRecipe.SingleInputSet.empty();
   private List<RecipeManager.ServerDisplayInfo> allDisplays = List.of();
   private Map<ResourceKey<Recipe<?>>, List<RecipeManager.ServerDisplayInfo>> recipeToDisplay = Map.of();

   public RecipeManager(HolderLookup.Provider registries) {
      this.registries = registries;
   }

   protected RecipeMap prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      SortedMap<Identifier, Recipe<?>> sortedMap = new TreeMap<>();
      SimpleJsonResourceReloadListener.scanDirectory(
         resourceManager, RECIPE_LISTER, this.registries.createSerializationContext(JsonOps.INSTANCE), Recipe.CODEC, sortedMap
      );
      List<RecipeHolder<?>> list = new ArrayList<>(sortedMap.size());
      sortedMap.forEach((identifier, recipe) -> {
         ResourceKey<Recipe<?>> resourceKey = ResourceKey.create(Registries.RECIPE, identifier);
         RecipeHolder<?> recipeHolder = new RecipeHolder<>(resourceKey, recipe);
         list.add(recipeHolder);
      });
      return RecipeMap.create(list);
   }

   protected void apply(RecipeMap recipeMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      this.recipes = recipeMap;
      LOGGER.info("Loaded {} recipes", recipeMap.values().size());
   }

   public void finalizeRecipeLoading(FeatureFlagSet enabledFeatures) {
      List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> list = new ArrayList<>();
      List<RecipeManager.IngredientCollector> list2 = RECIPE_PROPERTY_SETS.entrySet()
         .stream()
         .map(entry -> new RecipeManager.IngredientCollector(entry.getKey(), entry.getValue()))
         .toList();
      this.recipes
         .values()
         .forEach(
            recipeHolder -> {
               Recipe<?> recipe = recipeHolder.value();
               if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                  LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", recipeHolder.id().identifier());
               } else {
                  list2.forEach(ingredientCollector -> ingredientCollector.accept(recipe));
                  if (recipe instanceof StonecutterRecipe stonecutterRecipe
                     && isIngredientEnabled(enabledFeatures, stonecutterRecipe.input())
                     && stonecutterRecipe.resultDisplay().isEnabled(enabledFeatures)) {
                     list.add(
                        new SelectableRecipe.SingleInputEntry<>(
                           stonecutterRecipe.input(),
                           new SelectableRecipe<>(stonecutterRecipe.resultDisplay(), Optional.of((RecipeHolder<StonecutterRecipe>)recipeHolder))
                        )
                     );
                  }
               }
            }
         );
      this.propertySets = list2.stream()
         .collect(
            Collectors.toUnmodifiableMap(
               ingredientCollector -> ingredientCollector.key, ingredientCollector -> ingredientCollector.asPropertySet(enabledFeatures)
            )
         );
      this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(list);
      this.allDisplays = unpackRecipeInfo(this.recipes.values(), enabledFeatures);
      this.recipeToDisplay = this.allDisplays
         .stream()
         .collect(Collectors.groupingBy(serverDisplayInfo -> serverDisplayInfo.parent.id(), IdentityHashMap::new, Collectors.toList()));
   }

   static List<Ingredient> filterDisabled(FeatureFlagSet enabledFeatures, List<Ingredient> ingredients) {
      ingredients.removeIf(ingredient -> !isIngredientEnabled(enabledFeatures, ingredient));
      return ingredients;
   }

   private static boolean isIngredientEnabled(FeatureFlagSet enabledFeatures, Ingredient ingredient) {
      return ingredient.items().allMatch(holder -> holder.value().isEnabled(enabledFeatures));
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
      RecipeType<T> recipeType, I input, Level level, @Nullable ResourceKey<Recipe<?>> recipe
   ) {
      RecipeHolder<T> recipeHolder = recipe != null ? this.byKeyTyped(recipeType, recipe) : null;
      return this.getRecipeFor(recipeType, input, level, recipeHolder);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
      RecipeType<T> recipeType, I input, Level level, @Nullable RecipeHolder<T> lastRecipe
   ) {
      return lastRecipe != null && lastRecipe.value().matches(input, level) ? Optional.of(lastRecipe) : this.getRecipeFor(recipeType, input, level);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> recipeType, I input, Level level) {
      return this.recipes.getRecipesFor(recipeType, input, level).findFirst();
   }

   public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> key) {
      return Optional.ofNullable(this.recipes.byKey(key));
   }

   @Nullable
   private <T extends Recipe<?>> RecipeHolder<T> byKeyTyped(RecipeType<T> type, ResourceKey<Recipe<?>> key) {
      RecipeHolder<?> recipeHolder = this.recipes.byKey(key);
      return (RecipeHolder<T>)(recipeHolder != null && recipeHolder.value().getType().equals(type) ? recipeHolder : null);
   }

   public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
      return this.propertySets;
   }

   public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
      return this.stonecutterRecipes;
   }

   @Override
   public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> propertySet) {
      return this.propertySets.getOrDefault(propertySet, RecipePropertySet.EMPTY);
   }

   @Override
   public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
      return this.stonecutterRecipes;
   }

   public Collection<RecipeHolder<?>> getRecipes() {
      return this.recipes.values();
   }

   @Nullable
   public RecipeManager.ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId display) {
      int i = display.index();
      return i >= 0 && i < this.allDisplays.size() ? this.allDisplays.get(i) : null;
   }

   public void listDisplaysForRecipe(ResourceKey<Recipe<?>> recipe, Consumer<RecipeDisplayEntry> output) {
      List<RecipeManager.ServerDisplayInfo> list = this.recipeToDisplay.get(recipe);
      if (list != null) {
         list.forEach(serverDisplayInfo -> output.accept(serverDisplayInfo.display));
      }
   }

   @VisibleForTesting
   protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> recipe, JsonObject json, HolderLookup.Provider registries) {
      Recipe<?> recipe2 = (Recipe<?>)Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new);
      return new RecipeHolder<>(recipe, recipe2);
   }

   public static <I extends RecipeInput, T extends Recipe<I>> RecipeManager.CachedCheck<I, T> createCheck(RecipeType<T> recipeType) {
      return new RecipeManager.CachedCheck<I, T>() {
         @Nullable
         private ResourceKey<Recipe<?>> lastRecipe;

         @Override
         public Optional<RecipeHolder<T>> getRecipeFor(I input, ServerLevel level) {
            RecipeManager recipeManager = level.recipeAccess();
            Optional<RecipeHolder<T>> optional = recipeManager.getRecipeFor(recipeType, input, level, this.lastRecipe);
            if (optional.isPresent()) {
               RecipeHolder<T> recipeHolder = optional.get();
               this.lastRecipe = recipeHolder.id();
               return Optional.of(recipeHolder);
            } else {
               return Optional.empty();
            }
         }
      };
   }

   private static List<RecipeManager.ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> recipes, FeatureFlagSet enabledFeatures) {
      List<RecipeManager.ServerDisplayInfo> list = new ArrayList<>();
      Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();

      for (RecipeHolder<?> recipeHolder : recipes) {
         Recipe<?> recipe = recipeHolder.value();
         OptionalInt optionalInt;
         if (recipe.group().isEmpty()) {
            optionalInt = OptionalInt.empty();
         } else {
            optionalInt = OptionalInt.of(object2IntMap.computeIfAbsent(recipe.group(), object -> object2IntMap.size()));
         }

         Optional<List<Ingredient>> optional;
         if (recipe.isSpecial()) {
            optional = Optional.empty();
         } else {
            optional = Optional.of(recipe.placementInfo().ingredients());
         }

         for (RecipeDisplay recipeDisplay : recipe.display()) {
            if (recipeDisplay.isEnabled(enabledFeatures)) {
               int i = list.size();
               RecipeDisplayId recipeDisplayId = new RecipeDisplayId(i);
               RecipeDisplayEntry recipeDisplayEntry = new RecipeDisplayEntry(
                  recipeDisplayId, recipeDisplay, optionalInt, recipe.recipeBookCategory(), optional
               );
               list.add(new RecipeManager.ServerDisplayInfo(recipeDisplayEntry, recipeHolder));
            }
         }
      }

      return list;
   }

   private static RecipeManager.IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> recipeType) {
      return recipe -> recipe.getType() == recipeType && recipe instanceof SingleItemRecipe singleItemRecipe
         ? Optional.of(singleItemRecipe.input())
         : Optional.empty();
   }

   public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
      Optional<RecipeHolder<T>> getRecipeFor(I input, ServerLevel level);
   }

   public static class IngredientCollector implements Consumer<Recipe<?>> {
      final ResourceKey<RecipePropertySet> key;
      private final RecipeManager.IngredientExtractor extractor;
      private final List<Ingredient> ingredients = new ArrayList<>();

      protected IngredientCollector(ResourceKey<RecipePropertySet> key, RecipeManager.IngredientExtractor extractor) {
         this.key = key;
         this.extractor = extractor;
      }

      public void accept(Recipe<?> recipe) {
         this.extractor.apply(recipe).ifPresent(this.ingredients::add);
      }

      public RecipePropertySet asPropertySet(FeatureFlagSet enabledFeatures) {
         return RecipePropertySet.create(RecipeManager.filterDisabled(enabledFeatures, this.ingredients));
      }
   }

   @FunctionalInterface
   public interface IngredientExtractor {
      Optional<Ingredient> apply(Recipe<?> recipe);
   }

   public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
   }
}
