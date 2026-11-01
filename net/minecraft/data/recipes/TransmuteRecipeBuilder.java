package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.jspecify.annotations.Nullable;

public class TransmuteRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Holder<Item> result;
   private final Ingredient input;
   private final Ingredient material;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   @Nullable
   private String group;

   private TransmuteRecipeBuilder(RecipeCategory p_374344_, Holder<Item> p_374428_, Ingredient p_374309_, Ingredient p_374316_) {
      this.category = p_374344_;
      this.result = p_374428_;
      this.input = p_374309_;
      this.material = p_374316_;
   }

   public static TransmuteRecipeBuilder transmute(RecipeCategory p_374068_, Ingredient p_374585_, Ingredient p_374268_, Item p_374030_) {
      return new TransmuteRecipeBuilder(p_374068_, p_374030_.builtInRegistryHolder(), p_374585_, p_374268_);
   }

   public TransmuteRecipeBuilder unlockedBy(String p_374276_, Criterion<?> p_374559_) {
      this.criteria.put(p_374276_, p_374559_);
      return this;
   }

   public TransmuteRecipeBuilder group(@Nullable String p_374475_) {
      this.group = p_374475_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result.value();
   }

   @Override
   public void save(RecipeOutput p_374401_, ResourceKey<Recipe<?>> p_379646_) {
      this.ensureValid(p_379646_);
      Advancement.Builder advancement$builder = p_374401_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_379646_))
         .rewards(AdvancementRewards.Builder.recipe(p_379646_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      TransmuteRecipe transmuterecipe = new TransmuteRecipe(
         Objects.requireNonNullElse(this.group, ""),
         RecipeBuilder.determineBookCategory(this.category),
         this.input,
         this.material,
         new TransmuteResult(this.result.value())
      );
      p_374401_.accept(
         p_379646_, transmuterecipe, advancement$builder.build(p_379646_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_379810_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_379810_.identifier());
      }
   }
}
