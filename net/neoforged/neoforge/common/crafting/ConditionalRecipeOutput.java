package net.neoforged.neoforge.common.crafting;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class ConditionalRecipeOutput implements RecipeOutput {
   private final RecipeOutput inner;
   private final ICondition[] conditions;

   public ConditionalRecipeOutput(RecipeOutput inner, ICondition[] conditions) {
      this.inner = inner;
      this.conditions = conditions;
   }

   @Override
   public Advancement.Builder advancement() {
      return this.inner.advancement();
   }

   @Override
   public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
      ICondition[] innerConditions;
      if (conditions.length == 0) {
         innerConditions = this.conditions;
      } else if (this.conditions.length == 0) {
         innerConditions = conditions;
      } else {
         innerConditions = (ICondition[])ArrayUtils.addAll(this.conditions, conditions);
      }

      this.inner.accept(id, recipe, advancement, innerConditions);
   }

   @Override
   public void includeRootAdvancement() {
      this.inner.includeRootAdvancement();
   }
}
