package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IRecipeOutputExtension;

public interface RecipeOutput extends IRecipeOutputExtension {
   default void accept(ResourceLocation p_312249_, Recipe<?> p_312328_, @Nullable AdvancementHolder p_312176_) {
      this.accept(p_312249_, p_312328_, p_312176_, new ICondition[0]);
   }

   Advancement.Builder advancement();
}
