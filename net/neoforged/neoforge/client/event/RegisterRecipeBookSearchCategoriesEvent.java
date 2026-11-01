package net.neoforged.neoforge.client.event;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterRecipeBookSearchCategoriesEvent extends Event implements IModBusEvent {
   private final Map<ExtendedRecipeBookCategory, List<RecipeBookCategory>> categories;

   @Internal
   public RegisterRecipeBookSearchCategoriesEvent(Map<ExtendedRecipeBookCategory, List<RecipeBookCategory>> categories) {
      this.categories = categories;
   }

   public void register(ExtendedRecipeBookCategory searchCategory, RecipeBookCategory... includedCategories) {
      if (includedCategories.length == 0) {
         throw new IllegalArgumentException("Forgot to register included categories.");
      } else if (this.categories.containsKey(searchCategory)) {
         throw new IllegalArgumentException("Duplicate registration of search category " + searchCategory);
      } else {
         this.categories.put(searchCategory, List.of(includedCategories));
      }
   }
}
