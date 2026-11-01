package net.neoforged.neoforge.common.data.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.Nullable;

public final class NeoForgeRecipeProvider extends VanillaRecipeProvider {
   private final Map<Item, TagKey<Item>> replacements = new HashMap<>();
   private final Map<Item, Ingredient> specialReplacements = new HashMap<>();
   private final Set<ResourceLocation> excludes = new HashSet<>();

   public NeoForgeRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
      super(packOutput, provider);
   }

   private void exclude(ItemLike item) {
      this.excludes.add(BuiltInRegistries.ITEM.getKey(item.asItem()));
   }

   private void exclude(String name) {
      this.excludes.add(ResourceLocation.parse(name));
   }

   private void replace(ItemLike item, TagKey<Item> tag) {
      this.replacements.put(item.asItem(), tag);
   }

   @Override
   protected void buildRecipes(final RecipeOutput recipeOutput) {
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
      this.specialReplacements.put(Items.CHEST, DifferenceIngredient.of(Ingredient.of(Tags.Items.CHESTS_WOODEN), Ingredient.of(Tags.Items.CHESTS_TRAPPED)));
      super.buildRecipes(new RecipeOutput() {
         @Override
         public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
            Recipe<?> modified = NeoForgeRecipeProvider.this.enhance(id, recipe);
            if (modified != null) {
               recipeOutput.accept(id, modified, null, conditions);
            }
         }

         @Override
         public Advancement.Builder advancement() {
            return recipeOutput.advancement();
         }
      });
   }

   @Nullable
   private Recipe<?> enhance(ResourceLocation id, Recipe<?> vanilla) {
      if (vanilla instanceof ShapelessRecipe shapeless) {
         return this.enhance(id, shapeless);
      } else {
         return vanilla instanceof ShapedRecipe shaped ? this.enhance(id, shaped) : null;
      }
   }

   @Nullable
   private ShapelessRecipe enhance(ResourceLocation id, ShapelessRecipe vanilla) {
      List<Ingredient> ingredients = vanilla.getIngredients();
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

   @Override
   protected CompletableFuture<?> buildAdvancement(CachedOutput p_253674_, HolderLookup.Provider p_323646_, AdvancementHolder p_301116_) {
      return CompletableFuture.allOf();
   }

   @Nullable
   private ShapedRecipe enhance(ResourceLocation id, ShapedRecipe vanilla) {
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
   private Ingredient enhance(ResourceLocation name, Ingredient vanilla) {
      if (this.excludes.contains(name)) {
         return null;
      } else {
         boolean modified = false;
         List<Ingredient.Value> items = new ArrayList<>();
         Ingredient.Value[] vanillaItems = vanilla.getValues();
         if (vanillaItems.length == 1 && vanillaItems[0] instanceof Ingredient.ItemValue itemValue) {
            Item item = itemValue.item().getItem();
            Ingredient replacement = this.specialReplacements.get(item);
            if (replacement != null) {
               return replacement;
            }
         }

         for (Ingredient.Value entry : vanillaItems) {
            if (entry instanceof Ingredient.ItemValue) {
               ItemStack stack = entry.getItems().stream().findFirst().orElse(ItemStack.EMPTY);
               TagKey<Item> replacement = this.replacements.get(stack.getItem());
               if (replacement != null) {
                  items.add(new Ingredient.TagValue(replacement));
                  modified = true;
               } else {
                  items.add(entry);
               }
            } else {
               items.add(entry);
            }
         }

         return modified ? Ingredient.fromValues(items.stream()) : null;
      }
   }
}
