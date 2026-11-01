package net.minecraft.data.recipes;

import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

public class SpecialRecipeBuilder {
   private final Function<CraftingBookCategory, Recipe<?>> factory;

   public SpecialRecipeBuilder(Function<CraftingBookCategory, Recipe<?>> p_312708_) {
      this.factory = p_312708_;
   }

   public static SpecialRecipeBuilder special(Function<CraftingBookCategory, Recipe<?>> p_312084_) {
      return new SpecialRecipeBuilder(p_312084_);
   }

   public void save(RecipeOutput p_301307_, String p_126361_) {
      this.save(p_301307_, ResourceKey.create(Registries.RECIPE, Identifier.parse(p_126361_)));
   }

   public void save(RecipeOutput p_301123_, ResourceKey<Recipe<?>> p_379910_) {
      p_301123_.accept(p_379910_, this.factory.apply(CraftingBookCategory.MISC), null);
   }
}
