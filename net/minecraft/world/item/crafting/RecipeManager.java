package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
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
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceKey<RecipePropertySet>, RecipeManager.IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(
      RecipePropertySet.SMITHING_ADDITION,
      p_380841_ -> p_380841_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.additionIngredient() : Optional.empty(),
      RecipePropertySet.SMITHING_BASE,
      p_399417_ -> p_399417_ instanceof SmithingRecipe smithingrecipe ? Optional.of(smithingrecipe.baseIngredient()) : Optional.empty(),
      RecipePropertySet.SMITHING_TEMPLATE,
      p_380847_ -> p_380847_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.templateIngredient() : Optional.empty(),
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
   private Object2IntMap<ResourceKey<Recipe<?>>> recipePriorities = Object2IntMaps.emptyMap();

   public RecipeManager(HolderLookup.Provider p_324137_) {
      this.registries = p_324137_;
   }

   protected RecipeMap prepare(ResourceManager p_379845_, ProfilerFiller p_380058_) {
      SortedMap<Identifier, Recipe<?>> sortedmap = new TreeMap<>();
      SimpleJsonResourceReloadListener.scanDirectory(
         p_379845_,
         RECIPE_LISTER,
         new ConditionalOps<>(this.registries.createSerializationContext(JsonOps.INSTANCE), this.getContext()),
         Recipe.CODEC,
         sortedmap
      );
      List<RecipeHolder<?>> list = new ArrayList<>(sortedmap.size());
      sortedmap.forEach((p_466665_, p_466666_) -> {
         ResourceKey<Recipe<?>> resourcekey = ResourceKey.create(Registries.RECIPE, p_466665_);
         RecipeHolder<?> recipeholder = new RecipeHolder<>(resourcekey, p_466666_);
         list.add(recipeholder);
      });
      return RecipeMap.create(list);
   }

   protected void apply(RecipeMap p_380231_, ResourceManager p_44038_, ProfilerFiller p_44039_) {
      this.recipes = p_380231_;
      LOGGER.info("Loaded {} recipes", p_380231_.values().size());
   }

   public void finalizeRecipeLoading(FeatureFlagSet p_379660_) {
      this.recipes.order(this.recipePriorities);
      List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> list = new ArrayList<>();
      List<RecipeManager.IngredientCollector> list1 = RECIPE_PROPERTY_SETS.entrySet()
         .stream()
         .map(p_380840_ -> new RecipeManager.IngredientCollector(p_380840_.getKey(), p_380840_.getValue()))
         .toList();
      this.recipes
         .values()
         .forEach(
            p_466663_ -> {
               Recipe<?> recipe = p_466663_.value();
               if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                  LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", p_466663_.id().identifier());
               } else {
                  list1.forEach(p_380839_ -> p_380839_.accept(recipe));
                  if (recipe instanceof StonecutterRecipe stonecutterrecipe
                     && isIngredientEnabled(p_379660_, stonecutterrecipe.input())
                     && stonecutterrecipe.resultDisplay().isEnabled(p_379660_)) {
                     list.add(
                        new SelectableRecipe.SingleInputEntry<>(
                           stonecutterrecipe.input(),
                           new SelectableRecipe<>(stonecutterrecipe.resultDisplay(), Optional.of((RecipeHolder<StonecutterRecipe>)p_466663_))
                        )
                     );
                  }
               }
            }
         );
      this.propertySets = list1.stream().collect(Collectors.toUnmodifiableMap(p_380846_ -> p_380846_.key, p_380852_ -> p_380852_.asPropertySet(p_379660_)));
      this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(list);
      this.allDisplays = unpackRecipeInfo(this.recipes.values(), p_379660_);
      this.recipeToDisplay = this.allDisplays
         .stream()
         .collect(Collectors.groupingBy(p_379212_ -> p_379212_.parent.id(), IdentityHashMap::new, Collectors.toList()));
   }

   static List<Ingredient> filterDisabled(FeatureFlagSet p_379504_, List<Ingredient> p_379890_) {
      p_379890_.removeIf(p_379230_ -> !isIngredientEnabled(p_379504_, p_379230_));
      return p_379890_;
   }

   private static boolean isIngredientEnabled(FeatureFlagSet p_379722_, Ingredient p_380390_) {
      return p_380390_.items().allMatch(p_379224_ -> p_379224_.value().isEnabled(p_379722_));
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
      RecipeType<T> p_345895_, I p_345268_, Level p_346336_, @Nullable ResourceKey<Recipe<?>> p_380176_
   ) {
      RecipeHolder<T> recipeholder = p_380176_ != null ? this.byKeyTyped(p_345895_, p_380176_) : null;
      return this.getRecipeFor(p_345895_, p_345268_, p_346336_, recipeholder);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
      RecipeType<T> p_220249_, I p_345826_, Level p_220251_, @Nullable RecipeHolder<T> p_346407_
   ) {
      return p_346407_ != null && p_346407_.value().matches(p_345826_, p_220251_) ? Optional.of(p_346407_) : this.getRecipeFor(p_220249_, p_345826_, p_220251_);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> p_44016_, I p_345492_, Level p_44018_) {
      return this.recipes.getRecipesFor(p_44016_, p_345492_, p_44018_).findFirst();
   }

   public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> p_379753_) {
      return Optional.ofNullable(this.recipes.byKey(p_379753_));
   }

   @Nullable
   private <T extends Recipe<?>> RecipeHolder<T> byKeyTyped(RecipeType<T> p_341695_, ResourceKey<Recipe<?>> p_379841_) {
      RecipeHolder<?> recipeholder = this.recipes.byKey(p_379841_);
      return (RecipeHolder<T>)(recipeholder != null && recipeholder.value().getType().equals(p_341695_) ? recipeholder : null);
   }

   public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
      return this.propertySets;
   }

   public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
      return this.stonecutterRecipes;
   }

   @Override
   public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> p_379366_) {
      return this.propertySets.getOrDefault(p_379366_, RecipePropertySet.EMPTY);
   }

   @Override
   public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
      return this.stonecutterRecipes;
   }

   public Collection<RecipeHolder<?>> getRecipes() {
      return this.recipes.values();
   }

   @Nullable
   public RecipeManager.ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId p_379566_) {
      int i = p_379566_.index();
      return i >= 0 && i < this.allDisplays.size() ? this.allDisplays.get(i) : null;
   }

   public void listDisplaysForRecipe(ResourceKey<Recipe<?>> p_379964_, Consumer<RecipeDisplayEntry> p_379437_) {
      List<RecipeManager.ServerDisplayInfo> list = this.recipeToDisplay.get(p_379964_);
      if (list != null) {
         list.forEach(p_379228_ -> p_379437_.accept(p_379228_.display));
      }
   }

   @VisibleForTesting
   protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> p_380059_, JsonObject p_44047_, HolderLookup.Provider p_323755_) {
      Recipe<?> recipe = (Recipe<?>)Recipe.CODEC.parse(p_323755_.createSerializationContext(JsonOps.INSTANCE), p_44047_).getOrThrow(JsonParseException::new);
      return new RecipeHolder<>(p_380059_, recipe);
   }

   public static <I extends RecipeInput, T extends Recipe<I>> RecipeManager.CachedCheck<I, T> createCheck(final RecipeType<T> p_220268_) {
      return new RecipeManager.CachedCheck<I, T>() {
         @Nullable
         private ResourceKey<Recipe<?>> lastRecipe;

         @Override
         public Optional<RecipeHolder<T>> getRecipeFor(I p_344742_, ServerLevel p_379891_) {
            RecipeManager recipemanager = p_379891_.recipeAccess();
            Optional<RecipeHolder<T>> optional = recipemanager.getRecipeFor(p_220268_, p_344742_, p_379891_, this.lastRecipe);
            if (optional.isPresent()) {
               RecipeHolder<T> recipeholder = optional.get();
               this.lastRecipe = recipeholder.id();
               return Optional.of(recipeholder);
            } else {
               return Optional.empty();
            }
         }
      };
   }

   private static List<RecipeManager.ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> p_379538_, FeatureFlagSet p_379868_) {
      List<RecipeManager.ServerDisplayInfo> list = new ArrayList<>();
      Object2IntMap<String> object2intmap = new Object2IntOpenHashMap();

      for (RecipeHolder<?> recipeholder : p_379538_) {
         Recipe<?> recipe = recipeholder.value();
         OptionalInt optionalint;
         if (recipe.group().isEmpty()) {
            optionalint = OptionalInt.empty();
         } else {
            optionalint = OptionalInt.of(object2intmap.computeIfAbsent(recipe.group(), p_379226_ -> object2intmap.size()));
         }

         Optional<List<Ingredient>> optional;
         if (recipe.isSpecial()) {
            optional = Optional.empty();
         } else {
            optional = Optional.of(recipe.placementInfo().ingredients());
         }

         for (RecipeDisplay recipedisplay : recipe.display()) {
            if (recipedisplay.isEnabled(p_379868_)) {
               int i = list.size();
               RecipeDisplayId recipedisplayid = new RecipeDisplayId(i);
               RecipeDisplayEntry recipedisplayentry = new RecipeDisplayEntry(
                  recipedisplayid, recipedisplay, optionalint, recipe.recipeBookCategory(), optional
               );
               list.add(new RecipeManager.ServerDisplayInfo(recipedisplayentry, recipeholder));
            }
         }
      }

      return list;
   }

   private static RecipeManager.IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> p_381108_) {
      return p_380850_ -> p_380850_.getType() == p_381108_ && p_380850_ instanceof SingleItemRecipe singleitemrecipe
         ? Optional.of(singleitemrecipe.input())
         : Optional.empty();
   }

   public RecipeMap recipeMap() {
      return this.recipes;
   }

   public void setPriorityMap(Object2IntMap<ResourceKey<Recipe<?>>> recipePriorities) {
      this.recipePriorities = recipePriorities;
   }

   public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
      Optional<RecipeHolder<T>> getRecipeFor(I var1, ServerLevel var2);
   }

   public static class IngredientCollector implements Consumer<Recipe<?>> {
      final ResourceKey<RecipePropertySet> key;
      private final RecipeManager.IngredientExtractor extractor;
      private final List<Ingredient> ingredients = new ArrayList<>();

      protected IngredientCollector(ResourceKey<RecipePropertySet> p_380992_, RecipeManager.IngredientExtractor p_380987_) {
         this.key = p_380992_;
         this.extractor = p_380987_;
      }

      public void accept(Recipe<?> p_380946_) {
         this.extractor.apply(p_380946_).ifPresent(this.ingredients::add);
      }

      public RecipePropertySet asPropertySet(FeatureFlagSet p_380941_) {
         return RecipePropertySet.create(RecipeManager.filterDisabled(p_380941_, this.ingredients));
      }
   }

   @FunctionalInterface
   public interface IngredientExtractor {
      Optional<Ingredient> apply(Recipe<?> var1);
   }

   public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
   }
}
