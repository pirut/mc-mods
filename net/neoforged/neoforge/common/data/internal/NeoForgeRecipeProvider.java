package net.neoforged.neoforge.common.data.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import org.jspecify.annotations.Nullable;

public final class NeoForgeRecipeProvider extends VanillaRecipeProvider {
   private final NeoForgeRecipeProvider.InterceptingRecipeOutput output;

   private NeoForgeRecipeProvider(HolderLookup.Provider provider, NeoForgeRecipeProvider.InterceptingRecipeOutput output) {
      super(provider, output);
      this.output = output;
   }

   private void exclude(ItemLike item) {
      this.output.excludes.add(ResourceKey.create(Registries.RECIPE, BuiltInRegistries.ITEM.getKey(item.asItem())));
   }

   private void exclude(String name) {
      this.output.excludes.add(ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
   }

   private void replace(ItemLike item, TagKey<Item> tag) {
      this.output.replacements.put(item.asItem(), tag);
   }

   @Override
   protected void buildRecipes() {
      this.replace(Items.STICK, Tags.Items.RODS_WOODEN);
      this.replace(Items.GOLD_INGOT, Tags.Items.INGOTS_GOLD);
      this.replace(Items.GOLD_NUGGET, Tags.Items.NUGGETS_GOLD);
      this.replace(Items.IRON_INGOT, Tags.Items.INGOTS_IRON);
      this.replace(Items.IRON_NUGGET, Tags.Items.NUGGETS_IRON);
      this.replace(Items.NETHERITE_INGOT, Tags.Items.INGOTS_NETHERITE);
      this.replace(Items.COPPER_INGOT, Tags.Items.INGOTS_COPPER);
      this.replace(Items.AMETHYST_SHARD, Tags.Items.GEMS_AMETHYST);
      this.replace(Items.DIAMOND, Tags.Items.GEMS_DIAMOND);
      this.replace(Items.EMERALD, Tags.Items.GEMS_EMERALD);
      this.replace(Items.WHITE_DYE, Tags.Items.DYES_WHITE);
      this.replace(Items.ORANGE_DYE, Tags.Items.DYES_ORANGE);
      this.replace(Items.MAGENTA_DYE, Tags.Items.DYES_MAGENTA);
      this.replace(Items.LIGHT_BLUE_DYE, Tags.Items.DYES_LIGHT_BLUE);
      this.replace(Items.YELLOW_DYE, Tags.Items.DYES_YELLOW);
      this.replace(Items.LIME_DYE, Tags.Items.DYES_LIME);
      this.replace(Items.PINK_DYE, Tags.Items.DYES_PINK);
      this.replace(Items.GRAY_DYE, Tags.Items.DYES_GRAY);
      this.replace(Items.LIGHT_GRAY_DYE, Tags.Items.DYES_LIGHT_GRAY);
      this.replace(Items.CYAN_DYE, Tags.Items.DYES_CYAN);
      this.replace(Items.PURPLE_DYE, Tags.Items.DYES_PURPLE);
      this.replace(Items.BLUE_DYE, Tags.Items.DYES_BLUE);
      this.replace(Items.BROWN_DYE, Tags.Items.DYES_BROWN);
      this.replace(Items.GREEN_DYE, Tags.Items.DYES_GREEN);
      this.replace(Items.RED_DYE, Tags.Items.DYES_RED);
      this.replace(Items.BLACK_DYE, Tags.Items.DYES_BLACK);
      this.replace(Blocks.COBBLESTONE, Tags.Items.COBBLESTONES_NORMAL);
      this.replace(Blocks.COBBLED_DEEPSLATE, Tags.Items.COBBLESTONES_DEEPSLATE);
      this.replace(Items.EGG, Tags.Items.EGGS);
      this.replace(Items.STRING, Tags.Items.STRINGS);
      this.exclude(getConversionRecipeName(Blocks.WHITE_WOOL, Items.STRING));
      this.exclude(Blocks.GOLD_BLOCK);
      this.exclude(Items.GOLD_NUGGET);
      this.exclude(Blocks.IRON_BLOCK);
      this.exclude(Items.IRON_NUGGET);
      this.exclude(Blocks.DIAMOND_BLOCK);
      this.exclude(Blocks.EMERALD_BLOCK);
      this.exclude(Blocks.NETHERITE_BLOCK);
      this.exclude(Blocks.COPPER_BLOCK);
      this.exclude(Blocks.AMETHYST_BLOCK);
      this.exclude(Blocks.COBBLESTONE_STAIRS);
      this.exclude(Blocks.COBBLESTONE_SLAB);
      this.exclude(Blocks.COBBLESTONE_WALL);
      this.exclude(Blocks.COBBLED_DEEPSLATE_STAIRS);
      this.exclude(Blocks.COBBLED_DEEPSLATE_SLAB);
      this.exclude(Blocks.COBBLED_DEEPSLATE_WALL);
      this.output.specialReplacements.put(Items.CHEST, DifferenceIngredient.of(this.tag(Tags.Items.CHESTS_WOODEN), this.tag(Tags.Items.CHESTS_TRAPPED)));
      super.buildRecipes();
   }

   private static class InterceptingRecipeOutput implements RecipeOutput {
      private final HolderGetter<Item> items;
      private final RecipeOutput output;
      private final Map<Item, TagKey<Item>> replacements = new HashMap<>();
      private final Map<Item, Ingredient> specialReplacements = new HashMap<>();
      private final Set<ResourceKey<Recipe<?>>> excludes = new HashSet<>();

      private InterceptingRecipeOutput(HolderGetter<Item> items, RecipeOutput output) {
         this.items = items;
         this.output = output;
      }

      @Override
      public Advancement.Builder advancement() {
         return this.output.advancement();
      }

      @Override
      public void includeRootAdvancement() {
      }

      @Override
      public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
         Recipe<?> modified = this.enhance(id, recipe);
         if (modified != null) {
            this.output.accept(id, modified, null, conditions);
         }
      }

      @Nullable
      private Recipe<?> enhance(ResourceKey<Recipe<?>> id, Recipe<?> vanilla) {
         if (vanilla instanceof ShapelessRecipe shapeless) {
            return this.enhance(id, shapeless);
         } else {
            return vanilla instanceof ShapedRecipe shaped ? this.enhance(id, shaped) : null;
         }
      }

      @Nullable
      private ShapelessRecipe enhance(ResourceKey<Recipe<?>> id, ShapelessRecipe vanilla) {
         List<Ingredient> ingredients = (List<Ingredient>)ObfuscationReflectionHelper.getPrivateValue(ShapelessRecipe.class, vanilla, "ingredients");
         boolean modified = false;

         for (int x = 0; x < ingredients.size(); x++) {
            Ingredient ing = this.enhance(id, ingredients.get(x));
            if (ing != null) {
               ingredients.set(x, ing);
               modified = true;
            }
         }

         return modified ? vanilla : null;
      }

      @Nullable
      private ShapedRecipe enhance(ResourceKey<Recipe<?>> id, ShapedRecipe vanilla) {
         ShapedRecipePattern pattern = (ShapedRecipePattern)ObfuscationReflectionHelper.getPrivateValue(ShapedRecipe.class, vanilla, "pattern");
         if (pattern == null) {
            throw new IllegalStateException(ShapedRecipe.class.getName() + " has no field pattern");
         } else {
            ShapedRecipePattern.Data data = (ShapedRecipePattern.Data)((Optional)ObfuscationReflectionHelper.getPrivateValue(
                  ShapedRecipePattern.class, pattern, "data"
               ))
               .orElseThrow(() -> new IllegalArgumentException("recipe " + id + " does not have pattern data"));
            Map<Character, Ingredient> ingredients = data.key();
            boolean modified = false;

            for (Character x : ingredients.keySet()) {
               Ingredient ing = this.enhance(id, ingredients.get(x));
               if (ing != null) {
                  ingredients.put(x, ing);
                  modified = true;
               }
            }

            return modified ? vanilla : null;
         }
      }

      @Nullable
      private Ingredient enhance(ResourceKey<Recipe<?>> name, Ingredient vanilla) {
         return this.excludes.contains(name) ? null : (Ingredient)vanilla.getValues().unwrap().map(tagKey -> null, items -> {
            if (items.size() == 1) {
               Ingredient specialReplacement = this.specialReplacements.get(((Holder)items.getFirst()).value());
               if (specialReplacement != null) {
                  return specialReplacement;
               }

               TagKey<Item> replacement = this.replacements.get(((Holder)items.getFirst()).value());
               if (replacement != null) {
                  return Ingredient.of(this.items.getOrThrow(replacement));
               }
            }

            for (Holder<Item> holder : items) {
               if (this.replacements.containsKey(holder.value())) {
                  throw new IllegalArgumentException("Cannot replace '%s' which is part of a multi-item ingredient.".formatted(holder.value()));
               }
            }

            return null;
         });
      }
   }

   public static final class Runner extends RecipeProvider.Runner {
      public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
         super(output, lookupProvider);
      }

      @Override
      protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookupProvider, RecipeOutput output) {
         return new NeoForgeRecipeProvider(
            lookupProvider, new NeoForgeRecipeProvider.InterceptingRecipeOutput(lookupProvider.lookupOrThrow(Registries.ITEM), output)
         );
      }

      @Override
      public String getName() {
         return "NeoForge recipes";
      }
   }
}
