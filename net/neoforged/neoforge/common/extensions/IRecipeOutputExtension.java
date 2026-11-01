package net.neoforged.neoforge.common.extensions;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.ConditionalRecipeOutput;
import org.jetbrains.annotations.Nullable;

public interface IRecipeOutputExtension {
   private RecipeOutput self() {
      return (RecipeOutput)this;
   }

   void accept(ResourceLocation var1, Recipe<?> var2, @Nullable AdvancementHolder var3, ICondition... var4);

   default RecipeOutput withConditions(ICondition... conditions) {
      return new ConditionalRecipeOutput(this.self(), conditions);
   }
}
