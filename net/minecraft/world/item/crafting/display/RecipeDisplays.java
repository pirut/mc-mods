package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class RecipeDisplays {
   public static RecipeDisplay.Type<?> bootstrap(Registry<RecipeDisplay.Type<?>> p_379826_) {
      Registry.register(p_379826_, "crafting_shapeless", ShapelessCraftingRecipeDisplay.TYPE);
      Registry.register(p_379826_, "crafting_shaped", ShapedCraftingRecipeDisplay.TYPE);
      Registry.register(p_379826_, "furnace", FurnaceRecipeDisplay.TYPE);
      Registry.register(p_379826_, "stonecutter", StonecutterRecipeDisplay.TYPE);
      return Registry.register(p_379826_, "smithing", SmithingRecipeDisplay.TYPE);
   }
}
