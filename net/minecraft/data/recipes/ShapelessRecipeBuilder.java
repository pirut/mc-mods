package net.minecraft.data.recipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class ShapelessRecipeBuilder implements RecipeBuilder {
   private final HolderGetter<Item> items;
   private final RecipeCategory category;
   private final ItemStack result;
   private final List<Ingredient> ingredients = new ArrayList<>();
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   @Nullable
   private String group;

   private ShapelessRecipeBuilder(HolderGetter<Item> p_363417_, RecipeCategory p_250837_, ItemStack p_363612_) {
      this.items = p_363417_;
      this.category = p_250837_;
      this.result = p_363612_;
   }

   public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> p_364294_, RecipeCategory p_361887_, ItemStack p_364359_) {
      return new ShapelessRecipeBuilder(p_364294_, p_361887_, p_364359_);
   }

   public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> p_362315_, RecipeCategory p_250714_, ItemLike p_249659_) {
      return shapeless(p_362315_, p_250714_, p_249659_, 1);
   }

   public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> p_360448_, RecipeCategory p_252339_, ItemLike p_250836_, int p_249928_) {
      return new ShapelessRecipeBuilder(p_360448_, p_252339_, p_250836_.asItem().getDefaultInstance().copyWithCount(p_249928_));
   }

   public ShapelessRecipeBuilder requires(TagKey<Item> p_206420_) {
      return this.requires(Ingredient.of(this.items.getOrThrow(p_206420_)));
   }

   public ShapelessRecipeBuilder requires(ItemLike p_126210_) {
      return this.requires(p_126210_, 1);
   }

   public ShapelessRecipeBuilder requires(ItemLike p_126212_, int p_126213_) {
      for (int i = 0; i < p_126213_; i++) {
         this.requires(Ingredient.of(p_126212_));
      }

      return this;
   }

   public ShapelessRecipeBuilder requires(Ingredient p_126185_) {
      return this.requires(p_126185_, 1);
   }

   public ShapelessRecipeBuilder requires(Ingredient p_126187_, int p_126188_) {
      for (int i = 0; i < p_126188_; i++) {
         this.ingredients.add(p_126187_);
      }

      return this;
   }

   public ShapelessRecipeBuilder unlockedBy(String p_176781_, Criterion<?> p_300897_) {
      this.criteria.put(p_176781_, p_300897_);
      return this;
   }

   public ShapelessRecipeBuilder group(@Nullable String p_126195_) {
      this.group = p_126195_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result.getItem();
   }

   @Override
   public void save(RecipeOutput p_301215_, ResourceKey<Recipe<?>> p_379987_) {
      this.ensureValid(p_379987_);
      Advancement.Builder advancement$builder = p_301215_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_379987_))
         .rewards(AdvancementRewards.Builder.recipe(p_379987_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      ShapelessRecipe shapelessrecipe = new ShapelessRecipe(
         Objects.requireNonNullElse(this.group, ""), RecipeBuilder.determineBookCategory(this.category), this.result, this.ingredients
      );
      p_301215_.accept(
         p_379987_, shapelessrecipe, advancement$builder.build(p_379987_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_379745_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_379745_.identifier());
      }
   }
}
