package net.neoforged.neoforge.client.event;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterRecipeBookCategoriesEvent extends Event implements IModBusEvent {
   private final Map<RecipeBookCategories, ImmutableList<RecipeBookCategories>> aggregateCategories;
   private final Map<RecipeBookType, ImmutableList<RecipeBookCategories>> typeCategories;
   private final Map<RecipeType<?>, Function<RecipeHolder<?>, RecipeBookCategories>> recipeCategoryLookups;

   @Internal
   public RegisterRecipeBookCategoriesEvent(
      Map<RecipeBookCategories, ImmutableList<RecipeBookCategories>> aggregateCategories,
      Map<RecipeBookType, ImmutableList<RecipeBookCategories>> typeCategories,
      Map<RecipeType<?>, Function<RecipeHolder<?>, RecipeBookCategories>> recipeCategoryLookups
   ) {
      this.aggregateCategories = aggregateCategories;
      this.typeCategories = typeCategories;
      this.recipeCategoryLookups = recipeCategoryLookups;
   }

   public void registerAggregateCategory(RecipeBookCategories category, List<RecipeBookCategories> others) {
      this.aggregateCategories.put(category, ImmutableList.copyOf(others));
   }

   public void registerBookCategories(RecipeBookType type, List<RecipeBookCategories> categories) {
      this.typeCategories.put(type, ImmutableList.copyOf(categories));
   }

   public void registerRecipeCategoryFinder(RecipeType<?> type, Function<RecipeHolder<?>, RecipeBookCategories> lookup) {
      this.recipeCategoryLookups.put(type, lookup);
   }
}
