package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RecipeMap {
   public static final RecipeMap EMPTY = new RecipeMap(ImmutableMultimap.of(), Map.of());
   private Multimap<RecipeType<?>, RecipeHolder<?>> byType;
   private final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

   private RecipeMap(Multimap<RecipeType<?>, RecipeHolder<?>> p_379497_, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> p_380280_) {
      this.byType = p_379497_;
      this.byKey = p_380280_;
   }

   public static RecipeMap create(Iterable<RecipeHolder<?>> p_379481_) {
      Builder<RecipeType<?>, RecipeHolder<?>> builder = ImmutableMultimap.builder();
      com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> builder1 = ImmutableMap.builder();

      for (RecipeHolder<?> recipeholder : p_379481_) {
         builder.put(recipeholder.value().getType(), recipeholder);
         builder1.put(recipeholder.id(), recipeholder);
      }

      return new RecipeMap(builder.build(), builder1.build());
   }

   public void order(Object2IntMap<ResourceKey<Recipe<?>>> recipePriorities) {
      Int2ObjectRBTreeMap<List<RecipeHolder<?>>> priorityBuilder = new Int2ObjectRBTreeMap();
      LinkedListMultimap<RecipeType<?>, RecipeHolder<?>> finalBuilder = LinkedListMultimap.create();

      for (RecipeHolder<?> recipeholder : this.byKey.values()) {
         int priority = recipePriorities.getOrDefault(recipeholder.id(), 0);
         ((List)priorityBuilder.computeIfAbsent(priority, p -> new ArrayList())).add(recipeholder);
      }

      for (List<RecipeHolder<?>> list : priorityBuilder.reversed().values()) {
         for (RecipeHolder<?> recipeHolder : list) {
            finalBuilder.put(recipeHolder.value().getType(), recipeHolder);
         }
      }

      this.byType = ImmutableMultimap.copyOf(finalBuilder);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> p_379772_) {
      return this.byType.get(p_379772_);
   }

   public Collection<RecipeHolder<?>> values() {
      return this.byKey.values();
   }

   @Nullable
   public RecipeHolder<?> byKey(ResourceKey<Recipe<?>> p_380265_) {
      return this.byKey.get(p_380265_);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getRecipesFor(RecipeType<T> p_380402_, I p_379312_, Level p_379663_) {
      return p_379312_.isEmpty() ? Stream.empty() : this.byType(p_380402_).stream().filter(p_380352_ -> p_380352_.value().matches(p_379312_, p_379663_));
   }
}
