package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.EnterBlockTrigger;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;
import org.jspecify.annotations.Nullable;

public abstract class RecipeProvider {
   protected final HolderLookup.Provider registries;
   protected final HolderGetter<Item> items;
   protected final RecipeOutput output;
   private static final Map<BlockFamily.Variant, RecipeProvider.FamilyRecipeProvider> SHAPE_BUILDERS = ImmutableMap.builder()
      .put(
         BlockFamily.Variant.BUTTON,
         (RecipeProvider.FamilyRecipeProvider)(p_359442_, p_359443_, p_359444_) -> p_359442_.buttonBuilder(p_359443_, Ingredient.of(p_359444_))
      )
      .put(
         BlockFamily.Variant.CHISELED,
         (RecipeProvider.FamilyRecipeProvider)(p_359448_, p_359449_, p_359450_) -> p_359448_.chiseledBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_359449_, Ingredient.of(p_359450_)
         )
      )
      .put(
         BlockFamily.Variant.CUT,
         (RecipeProvider.FamilyRecipeProvider)(p_359439_, p_359440_, p_359441_) -> p_359439_.cutBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_359440_, Ingredient.of(p_359441_)
         )
      )
      .put(
         BlockFamily.Variant.DOOR,
         (RecipeProvider.FamilyRecipeProvider)(p_359456_, p_359457_, p_359458_) -> p_359456_.doorBuilder(p_359457_, Ingredient.of(p_359458_))
      )
      .put(
         BlockFamily.Variant.CUSTOM_FENCE,
         (RecipeProvider.FamilyRecipeProvider)(p_359445_, p_359446_, p_359447_) -> p_359445_.fenceBuilder(p_359446_, Ingredient.of(p_359447_))
      )
      .put(
         BlockFamily.Variant.FENCE,
         (RecipeProvider.FamilyRecipeProvider)(p_359430_, p_359431_, p_359432_) -> p_359430_.fenceBuilder(p_359431_, Ingredient.of(p_359432_))
      )
      .put(
         BlockFamily.Variant.CUSTOM_FENCE_GATE,
         (RecipeProvider.FamilyRecipeProvider)(p_359420_, p_359421_, p_359422_) -> p_359420_.fenceGateBuilder(p_359421_, Ingredient.of(p_359422_))
      )
      .put(
         BlockFamily.Variant.FENCE_GATE,
         (RecipeProvider.FamilyRecipeProvider)(p_359411_, p_359412_, p_359413_) -> p_359411_.fenceGateBuilder(p_359412_, Ingredient.of(p_359413_))
      )
      .put(
         BlockFamily.Variant.SIGN,
         (RecipeProvider.FamilyRecipeProvider)(p_359408_, p_359409_, p_359410_) -> p_359408_.signBuilder(p_359409_, Ingredient.of(p_359410_))
      )
      .put(
         BlockFamily.Variant.SLAB,
         (RecipeProvider.FamilyRecipeProvider)(p_359405_, p_359406_, p_359407_) -> p_359405_.slabBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_359406_, Ingredient.of(p_359407_)
         )
      )
      .put(
         BlockFamily.Variant.STAIRS,
         (RecipeProvider.FamilyRecipeProvider)(p_359433_, p_359434_, p_359435_) -> p_359433_.stairBuilder(p_359434_, Ingredient.of(p_359435_))
      )
      .put(
         BlockFamily.Variant.PRESSURE_PLATE,
         (RecipeProvider.FamilyRecipeProvider)(p_359451_, p_359452_, p_359453_) -> p_359451_.pressurePlateBuilder(
            RecipeCategory.REDSTONE, p_359452_, Ingredient.of(p_359453_)
         )
      )
      .put(
         BlockFamily.Variant.POLISHED,
         (RecipeProvider.FamilyRecipeProvider)(p_359417_, p_359418_, p_359419_) -> p_359417_.polishedBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_359418_, Ingredient.of(p_359419_)
         )
      )
      .put(
         BlockFamily.Variant.TRAPDOOR,
         (RecipeProvider.FamilyRecipeProvider)(p_359414_, p_359415_, p_359416_) -> p_359414_.trapdoorBuilder(p_359415_, Ingredient.of(p_359416_))
      )
      .put(
         BlockFamily.Variant.WALL,
         (RecipeProvider.FamilyRecipeProvider)(p_359436_, p_359437_, p_359438_) -> p_359436_.wallBuilder(
            RecipeCategory.DECORATIONS, p_359437_, Ingredient.of(p_359438_)
         )
      )
      .build();

   protected RecipeProvider(HolderLookup.Provider p_360573_, RecipeOutput p_360872_) {
      this.registries = p_360573_;
      this.items = p_360573_.lookupOrThrow(Registries.ITEM);
      this.output = p_360872_;
   }

   protected abstract void buildRecipes();

   protected void generateForEnabledBlockFamilies(FeatureFlagSet p_251836_) {
      BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateRecipe).forEach(p_359455_ -> this.generateRecipes(p_359455_, p_251836_));
   }

   protected void oneToOneConversionRecipe(ItemLike p_176558_, ItemLike p_176559_, @Nullable String p_176560_) {
      this.oneToOneConversionRecipe(p_176558_, p_176559_, p_176560_, 1);
   }

   protected void oneToOneConversionRecipe(ItemLike p_176553_, ItemLike p_176554_, @Nullable String p_176555_, int p_362382_) {
      this.shapeless(RecipeCategory.MISC, p_176553_, p_362382_)
         .requires(p_176554_)
         .group(p_176555_)
         .unlockedBy(getHasName(p_176554_), this.has(p_176554_))
         .save(this.output, getConversionRecipeName(p_176553_, p_176554_));
   }

   protected void oreSmelting(List<ItemLike> p_250172_, RecipeCategory p_250588_, ItemLike p_251868_, float p_250789_, int p_252144_, String p_251687_) {
      this.oreCooking(RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, p_250172_, p_250588_, p_251868_, p_250789_, p_252144_, p_251687_, "_from_smelting");
   }

   protected void oreBlasting(List<ItemLike> p_251504_, RecipeCategory p_248846_, ItemLike p_249735_, float p_248783_, int p_250303_, String p_251984_) {
      this.oreCooking(RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, p_251504_, p_248846_, p_249735_, p_248783_, p_250303_, p_251984_, "_from_blasting");
   }

   protected <T extends AbstractCookingRecipe> void oreCooking(
      RecipeSerializer<T> p_251817_,
      AbstractCookingRecipe.Factory<T> p_312707_,
      List<ItemLike> p_249619_,
      RecipeCategory p_251154_,
      ItemLike p_250066_,
      float p_251871_,
      int p_251316_,
      String p_251450_,
      String p_249236_
   ) {
      for (ItemLike itemlike : p_249619_) {
         SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), p_251154_, p_250066_, p_251871_, p_251316_, p_251817_, p_312707_)
            .group(p_251450_)
            .unlockedBy(getHasName(itemlike), this.has(itemlike))
            .save(this.output, getItemName(p_250066_) + p_249236_ + "_" + getItemName(itemlike));
      }
   }

   protected void netheriteSmithing(Item p_250046_, RecipeCategory p_248986_, Item p_250389_) {
      SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(p_250046_),
            this.tag(ItemTags.NETHERITE_TOOL_MATERIALS),
            p_248986_,
            p_250389_
         )
         .unlocks("has_netherite_ingot", this.has(ItemTags.NETHERITE_TOOL_MATERIALS))
         .save(this.output, getItemName(p_250389_) + "_smithing");
   }

   protected void trimSmithing(Item p_285461_, ResourceKey<TrimPattern> p_379766_, ResourceKey<Recipe<?>> p_399566_) {
      Holder.Reference<TrimPattern> reference = this.registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(p_379766_);
      SmithingTrimRecipeBuilder.smithingTrim(
            Ingredient.of(p_285461_), this.tag(ItemTags.TRIMMABLE_ARMOR), this.tag(ItemTags.TRIM_MATERIALS), reference, RecipeCategory.MISC
         )
         .unlocks("has_smithing_trim_template", this.has(p_285461_))
         .save(this.output, p_399566_);
   }

   protected void twoByTwoPacker(RecipeCategory p_250881_, ItemLike p_252184_, ItemLike p_249710_) {
      this.shaped(p_250881_, p_252184_, 1)
         .define('#', p_249710_)
         .pattern("##")
         .pattern("##")
         .unlockedBy(getHasName(p_249710_), this.has(p_249710_))
         .save(this.output);
   }

   protected void threeByThreePacker(RecipeCategory p_259247_, ItemLike p_259376_, ItemLike p_259717_, String p_260308_) {
      this.shapeless(p_259247_, p_259376_).requires(p_259717_, 9).unlockedBy(p_260308_, this.has(p_259717_)).save(this.output);
   }

   protected void threeByThreePacker(RecipeCategory p_259186_, ItemLike p_259360_, ItemLike p_259263_) {
      this.threeByThreePacker(p_259186_, p_259360_, p_259263_, getHasName(p_259263_));
   }

   protected void planksFromLog(ItemLike p_259052_, TagKey<Item> p_259045_, int p_259471_) {
      this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_259052_, p_259471_)
         .requires(p_259045_)
         .group("planks")
         .unlockedBy("has_log", this.has(p_259045_))
         .save(this.output);
   }

   protected void planksFromLogs(ItemLike p_259193_, TagKey<Item> p_259818_, int p_259807_) {
      this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_259193_, p_259807_)
         .requires(p_259818_)
         .group("planks")
         .unlockedBy("has_logs", this.has(p_259818_))
         .save(this.output);
   }

   protected void woodFromLogs(ItemLike p_126004_, ItemLike p_126005_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_126004_, 3)
         .define('#', p_126005_)
         .pattern("##")
         .pattern("##")
         .group("bark")
         .unlockedBy("has_log", this.has(p_126005_))
         .save(this.output);
   }

   protected void woodenBoat(ItemLike p_126023_, ItemLike p_126024_) {
      this.shaped(RecipeCategory.TRANSPORTATION, p_126023_)
         .define('#', p_126024_)
         .pattern("# #")
         .pattern("###")
         .group("boat")
         .unlockedBy("in_water", insideOf(Blocks.WATER))
         .save(this.output);
   }

   protected void chestBoat(ItemLike p_236373_, ItemLike p_236374_) {
      this.shapeless(RecipeCategory.TRANSPORTATION, p_236373_)
         .requires(Blocks.CHEST)
         .requires(p_236374_)
         .group("chest_boat")
         .unlockedBy("has_boat", this.has(ItemTags.BOATS))
         .save(this.output);
   }

   protected RecipeBuilder buttonBuilder(ItemLike p_176659_, Ingredient p_176660_) {
      return this.shapeless(RecipeCategory.REDSTONE, p_176659_).requires(p_176660_);
   }

   protected RecipeBuilder doorBuilder(ItemLike p_176671_, Ingredient p_176672_) {
      return this.shaped(RecipeCategory.REDSTONE, p_176671_, 3).define('#', p_176672_).pattern("##").pattern("##").pattern("##");
   }

   protected RecipeBuilder fenceBuilder(ItemLike p_176679_, Ingredient p_176680_) {
      int i = p_176679_ == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
      Item item = p_176679_ == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
      return this.shaped(RecipeCategory.DECORATIONS, p_176679_, i).define('W', p_176680_).define('#', item).pattern("W#W").pattern("W#W");
   }

   protected RecipeBuilder fenceGateBuilder(ItemLike p_176685_, Ingredient p_176686_) {
      return this.shaped(RecipeCategory.REDSTONE, p_176685_).define('#', Items.STICK).define('W', p_176686_).pattern("#W#").pattern("#W#");
   }

   protected void pressurePlate(ItemLike p_176692_, ItemLike p_176693_) {
      this.pressurePlateBuilder(RecipeCategory.REDSTONE, p_176692_, Ingredient.of(p_176693_))
         .unlockedBy(getHasName(p_176693_), this.has(p_176693_))
         .save(this.output);
   }

   protected RecipeBuilder pressurePlateBuilder(RecipeCategory p_251447_, ItemLike p_251989_, Ingredient p_249211_) {
      return this.shaped(p_251447_, p_251989_).define('#', p_249211_).pattern("##");
   }

   protected void slab(RecipeCategory p_251848_, ItemLike p_249368_, ItemLike p_252133_) {
      this.slabBuilder(p_251848_, p_249368_, Ingredient.of(p_252133_)).unlockedBy(getHasName(p_252133_), this.has(p_252133_)).save(this.output);
   }

   protected void shelf(ItemLike p_435352_, ItemLike p_432772_) {
      this.shaped(RecipeCategory.DECORATIONS, p_435352_, 6)
         .define('#', p_432772_)
         .pattern("###")
         .pattern("   ")
         .pattern("###")
         .group("shelf")
         .unlockedBy(getHasName(p_432772_), this.has(p_432772_))
         .save(this.output);
   }

   protected RecipeBuilder slabBuilder(RecipeCategory p_251707_, ItemLike p_251284_, Ingredient p_248824_) {
      return this.shaped(p_251707_, p_251284_, 6).define('#', p_248824_).pattern("###");
   }

   protected RecipeBuilder stairBuilder(ItemLike p_176711_, Ingredient p_176712_) {
      return this.shaped(RecipeCategory.BUILDING_BLOCKS, p_176711_, 4).define('#', p_176712_).pattern("#  ").pattern("## ").pattern("###");
   }

   protected RecipeBuilder trapdoorBuilder(ItemLike p_176721_, Ingredient p_176722_) {
      return this.shaped(RecipeCategory.REDSTONE, p_176721_, 2).define('#', p_176722_).pattern("###").pattern("###");
   }

   protected RecipeBuilder signBuilder(ItemLike p_176727_, Ingredient p_176728_) {
      return this.shaped(RecipeCategory.DECORATIONS, p_176727_, 3)
         .group("sign")
         .define('#', p_176728_)
         .define('X', Items.STICK)
         .pattern("###")
         .pattern("###")
         .pattern(" X ");
   }

   protected void hangingSign(ItemLike p_252355_, ItemLike p_250437_) {
      this.shaped(RecipeCategory.DECORATIONS, p_252355_, 6)
         .group("hanging_sign")
         .define('#', p_250437_)
         .define('X', Items.IRON_CHAIN)
         .pattern("X X")
         .pattern("###")
         .pattern("###")
         .unlockedBy("has_stripped_logs", this.has(p_250437_))
         .save(this.output);
   }

   protected void colorItemWithDye(List<Item> p_289675_, List<Item> p_289672_, String p_289641_, RecipeCategory p_423651_) {
      this.colorWithDye(p_289675_, p_289672_, null, p_289641_, p_423651_);
   }

   protected void colorWithDye(List<Item> p_361753_, List<Item> p_362611_, @Nullable Item p_361184_, String p_362682_, RecipeCategory p_365350_) {
      for (int i = 0; i < p_361753_.size(); i++) {
         Item item = p_361753_.get(i);
         Item item1 = p_362611_.get(i);
         Stream<Item> stream = p_362611_.stream().filter(p_288265_ -> !p_288265_.equals(item1));
         if (p_361184_ != null) {
            stream = Stream.concat(stream, Stream.of(p_361184_));
         }

         this.shapeless(p_365350_, item1)
            .requires(item)
            .requires(Ingredient.of(stream))
            .group(p_362682_)
            .unlockedBy("has_needed_dye", this.has(item))
            .save(this.output, "dye_" + getItemName(item1));
      }
   }

   protected void carpet(ItemLike p_176718_, ItemLike p_176719_) {
      this.shaped(RecipeCategory.DECORATIONS, p_176718_, 3)
         .define('#', p_176719_)
         .pattern("##")
         .group("carpet")
         .unlockedBy(getHasName(p_176719_), this.has(p_176719_))
         .save(this.output);
   }

   protected void bedFromPlanksAndWool(ItemLike p_126075_, ItemLike p_126076_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126075_)
         .define('#', p_126076_)
         .define('X', ItemTags.PLANKS)
         .pattern("###")
         .pattern("XXX")
         .group("bed")
         .unlockedBy(getHasName(p_126076_), this.has(p_126076_))
         .save(this.output);
   }

   protected void banner(ItemLike p_126083_, ItemLike p_126084_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126083_)
         .define('#', p_126084_)
         .define('|', Items.STICK)
         .pattern("###")
         .pattern("###")
         .pattern(" | ")
         .group("banner")
         .unlockedBy(getHasName(p_126084_), this.has(p_126084_))
         .save(this.output);
   }

   protected void stainedGlassFromGlassAndDye(ItemLike p_126087_, ItemLike p_126088_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_126087_, 8)
         .define('#', Blocks.GLASS)
         .define('X', p_126088_)
         .pattern("###")
         .pattern("#X#")
         .pattern("###")
         .group("stained_glass")
         .unlockedBy("has_glass", this.has(Blocks.GLASS))
         .save(this.output);
   }

   protected void dryGhast(ItemLike p_416739_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_416739_, 1)
         .define('#', Items.GHAST_TEAR)
         .define('X', Items.SOUL_SAND)
         .pattern("###")
         .pattern("#X#")
         .pattern("###")
         .group("dry_ghast")
         .unlockedBy(getHasName(Items.GHAST_TEAR), this.has(Items.GHAST_TEAR))
         .save(this.output);
   }

   protected void harness(ItemLike p_416620_, ItemLike p_416110_) {
      this.shaped(RecipeCategory.COMBAT, p_416620_)
         .define('#', p_416110_)
         .define('G', Items.GLASS)
         .define('L', Items.LEATHER)
         .pattern("LLL")
         .pattern("G#G")
         .group("harness")
         .unlockedBy("has_dried_ghast", this.has(Blocks.DRIED_GHAST))
         .save(this.output);
   }

   protected void stainedGlassPaneFromStainedGlass(ItemLike p_126091_, ItemLike p_126092_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126091_, 16)
         .define('#', p_126092_)
         .pattern("###")
         .pattern("###")
         .group("stained_glass_pane")
         .unlockedBy("has_glass", this.has(p_126092_))
         .save(this.output);
   }

   protected void stainedGlassPaneFromGlassPaneAndDye(ItemLike p_126095_, ItemLike p_126096_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126095_, 8)
         .define('#', Blocks.GLASS_PANE)
         .define('$', p_126096_)
         .pattern("###")
         .pattern("#$#")
         .pattern("###")
         .group("stained_glass_pane")
         .unlockedBy("has_glass_pane", this.has(Blocks.GLASS_PANE))
         .unlockedBy(getHasName(p_126096_), this.has(p_126096_))
         .save(this.output, getConversionRecipeName(p_126095_, Blocks.GLASS_PANE));
   }

   protected void coloredTerracottaFromTerracottaAndDye(ItemLike p_126099_, ItemLike p_126100_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_126099_, 8)
         .define('#', Blocks.TERRACOTTA)
         .define('X', p_126100_)
         .pattern("###")
         .pattern("#X#")
         .pattern("###")
         .group("stained_terracotta")
         .unlockedBy("has_terracotta", this.has(Blocks.TERRACOTTA))
         .save(this.output);
   }

   protected void concretePowder(ItemLike p_126103_, ItemLike p_126104_) {
      this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_126103_, 8)
         .requires(p_126104_)
         .requires(Blocks.SAND, 4)
         .requires(Blocks.GRAVEL, 4)
         .group("concrete_powder")
         .unlockedBy("has_sand", this.has(Blocks.SAND))
         .unlockedBy("has_gravel", this.has(Blocks.GRAVEL))
         .save(this.output);
   }

   protected void candle(ItemLike p_176544_, ItemLike p_176545_) {
      this.shapeless(RecipeCategory.DECORATIONS, p_176544_)
         .requires(Blocks.CANDLE)
         .requires(p_176545_)
         .group("dyed_candle")
         .unlockedBy(getHasName(p_176545_), this.has(p_176545_))
         .save(this.output);
   }

   protected void wall(RecipeCategory p_251148_, ItemLike p_250499_, ItemLike p_249970_) {
      this.wallBuilder(p_251148_, p_250499_, Ingredient.of(p_249970_)).unlockedBy(getHasName(p_249970_), this.has(p_249970_)).save(this.output);
   }

   protected RecipeBuilder wallBuilder(RecipeCategory p_249083_, ItemLike p_250754_, Ingredient p_250311_) {
      return this.shaped(p_249083_, p_250754_, 6).define('#', p_250311_).pattern("###").pattern("###");
   }

   protected void polished(RecipeCategory p_248719_, ItemLike p_250032_, ItemLike p_250021_) {
      this.polishedBuilder(p_248719_, p_250032_, Ingredient.of(p_250021_)).unlockedBy(getHasName(p_250021_), this.has(p_250021_)).save(this.output);
   }

   protected RecipeBuilder polishedBuilder(RecipeCategory p_249131_, ItemLike p_251242_, Ingredient p_251412_) {
      return this.shaped(p_249131_, p_251242_, 4).define('S', p_251412_).pattern("SS").pattern("SS");
   }

   protected void cut(RecipeCategory p_252306_, ItemLike p_249686_, ItemLike p_251100_) {
      this.cutBuilder(p_252306_, p_249686_, Ingredient.of(p_251100_)).unlockedBy(getHasName(p_251100_), this.has(p_251100_)).save(this.output);
   }

   protected ShapedRecipeBuilder cutBuilder(RecipeCategory p_250895_, ItemLike p_251147_, Ingredient p_251563_) {
      return this.shaped(p_250895_, p_251147_, 4).define('#', p_251563_).pattern("##").pattern("##");
   }

   protected void chiseled(RecipeCategory p_251604_, ItemLike p_251049_, ItemLike p_252267_) {
      this.chiseledBuilder(p_251604_, p_251049_, Ingredient.of(p_252267_)).unlockedBy(getHasName(p_252267_), this.has(p_252267_)).save(this.output);
   }

   protected void mosaicBuilder(RecipeCategory p_248788_, ItemLike p_251925_, ItemLike p_252242_) {
      this.shaped(p_248788_, p_251925_)
         .define('#', p_252242_)
         .pattern("#")
         .pattern("#")
         .unlockedBy(getHasName(p_252242_), this.has(p_252242_))
         .save(this.output);
   }

   protected ShapedRecipeBuilder chiseledBuilder(RecipeCategory p_251755_, ItemLike p_249782_, Ingredient p_250087_) {
      return this.shaped(p_251755_, p_249782_).define('#', p_250087_).pattern("#").pattern("#");
   }

   protected void stonecutterResultFromBase(RecipeCategory p_248911_, ItemLike p_251265_, ItemLike p_250033_) {
      this.stonecutterResultFromBase(p_248911_, p_251265_, p_250033_, 1);
   }

   protected void stonecutterResultFromBase(RecipeCategory p_250609_, ItemLike p_251254_, ItemLike p_249666_, int p_251462_) {
      SingleItemRecipeBuilder.stonecutting(Ingredient.of(p_249666_), p_250609_, p_251254_, p_251462_)
         .unlockedBy(getHasName(p_249666_), this.has(p_249666_))
         .save(this.output, getConversionRecipeName(p_251254_, p_249666_) + "_stonecutting");
   }

   protected void smeltingResultFromBase(ItemLike p_176741_, ItemLike p_176742_) {
      SimpleCookingRecipeBuilder.smelting(Ingredient.of(p_176742_), RecipeCategory.BUILDING_BLOCKS, p_176741_, 0.1F, 200)
         .unlockedBy(getHasName(p_176742_), this.has(p_176742_))
         .save(this.output);
   }

   protected void nineBlockStorageRecipes(RecipeCategory p_250083_, ItemLike p_250042_, RecipeCategory p_248977_, ItemLike p_251911_) {
      this.nineBlockStorageRecipes(p_250083_, p_250042_, p_248977_, p_251911_, getSimpleRecipeName(p_251911_), null, getSimpleRecipeName(p_250042_), null);
   }

   protected void nineBlockStorageRecipesWithCustomPacking(
      RecipeCategory p_250885_, ItemLike p_251651_, RecipeCategory p_250874_, ItemLike p_248576_, String p_250171_, String p_249386_
   ) {
      this.nineBlockStorageRecipes(p_250885_, p_251651_, p_250874_, p_248576_, p_250171_, p_249386_, getSimpleRecipeName(p_251651_), null);
   }

   protected void nineBlockStorageRecipesRecipesWithCustomUnpacking(
      RecipeCategory p_248979_, ItemLike p_249101_, RecipeCategory p_252036_, ItemLike p_250886_, String p_248768_, String p_250847_
   ) {
      this.nineBlockStorageRecipes(p_248979_, p_249101_, p_252036_, p_250886_, getSimpleRecipeName(p_250886_), null, p_248768_, p_250847_);
   }

   protected void nineBlockStorageRecipes(
      RecipeCategory p_251203_,
      ItemLike p_251689_,
      RecipeCategory p_251376_,
      ItemLike p_248771_,
      String p_364391_,
      @Nullable String p_361531_,
      String p_363105_,
      @Nullable String p_365446_
   ) {
      this.shapeless(p_251203_, p_251689_, 9)
         .requires(p_248771_)
         .group(p_365446_)
         .unlockedBy(getHasName(p_248771_), this.has(p_248771_))
         .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.parse(p_363105_)));
      this.shaped(p_251376_, p_248771_)
         .define('#', p_251689_)
         .pattern("###")
         .pattern("###")
         .pattern("###")
         .group(p_361531_)
         .unlockedBy(getHasName(p_251689_), this.has(p_251689_))
         .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.parse(p_364391_)));
   }

   protected void copySmithingTemplate(ItemLike p_350799_, ItemLike p_365321_) {
      this.shaped(RecipeCategory.MISC, p_350799_, 2)
         .define('#', Items.DIAMOND)
         .define('C', p_365321_)
         .define('S', p_350799_)
         .pattern("#S#")
         .pattern("#C#")
         .pattern("###")
         .unlockedBy(getHasName(p_350799_), this.has(p_350799_))
         .save(this.output);
   }

   protected void copySmithingTemplate(ItemLike p_266974_, Ingredient p_360677_) {
      this.shaped(RecipeCategory.MISC, p_266974_, 2)
         .define('#', Items.DIAMOND)
         .define('C', p_360677_)
         .define('S', p_266974_)
         .pattern("#S#")
         .pattern("#C#")
         .pattern("###")
         .unlockedBy(getHasName(p_266974_), this.has(p_266974_))
         .save(this.output);
   }

   protected <T extends AbstractCookingRecipe> void cookRecipes(
      String p_126008_, RecipeSerializer<T> p_250529_, AbstractCookingRecipe.Factory<T> p_312449_, int p_126010_
   ) {
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.BEEF, Items.COOKED_BEEF, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.COD, Items.COOKED_COD, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.KELP, Items.DRIED_KELP, 0.1F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.SALMON, Items.COOKED_SALMON, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.POTATO, Items.BAKED_POTATO, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312449_, p_126010_, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
   }

   protected <T extends AbstractCookingRecipe> void simpleCookingRecipe(
      String p_249709_,
      RecipeSerializer<T> p_251876_,
      AbstractCookingRecipe.Factory<T> p_312056_,
      int p_249258_,
      ItemLike p_250669_,
      ItemLike p_250224_,
      float p_252138_
   ) {
      SimpleCookingRecipeBuilder.generic(Ingredient.of(p_250669_), RecipeCategory.FOOD, p_250224_, p_252138_, p_249258_, p_251876_, p_312056_)
         .unlockedBy(getHasName(p_250669_), this.has(p_250669_))
         .save(this.output, getItemName(p_250224_) + "_from_" + p_249709_);
   }

   protected void waxRecipes(FeatureFlagSet p_313879_) {
      DataMapHooks.INVERSE_WAXABLES_DATAMAP
         .forEach(
            (p_470522_, p_470523_) -> {
               if (p_470523_.requiredFeatures().isSubsetOf(p_313879_)) {
                  Pair<RecipeCategory, String> pair = (Pair<RecipeCategory, String>)HoneycombItem.WAXED_RECIPES
                     .getOrDefault(p_470523_, Pair.of(RecipeCategory.BUILDING_BLOCKS, getItemName(p_470523_)));
                  RecipeCategory recipecategory = (RecipeCategory)pair.getFirst();
                  String s = (String)pair.getSecond();
                  this.shapeless(recipecategory, p_470523_)
                     .requires(p_470522_)
                     .requires(Items.HONEYCOMB)
                     .group(s)
                     .unlockedBy(getHasName(p_470522_), this.has(p_470522_))
                     .save(this.output, getConversionRecipeName(p_470523_, Items.HONEYCOMB));
               }
            }
         );
   }

   protected void grate(Block p_309021_, Block p_309140_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_309021_, 4)
         .define('M', p_309140_)
         .pattern(" M ")
         .pattern("M M")
         .pattern(" M ")
         .group(getItemName(p_309021_))
         .unlockedBy(getHasName(p_309140_), this.has(p_309140_))
         .save(this.output);
   }

   protected void copperBulb(Block p_309026_, Block p_308866_) {
      this.shaped(RecipeCategory.REDSTONE, p_309026_, 4)
         .define('C', p_308866_)
         .define('R', Items.REDSTONE)
         .define('B', Items.BLAZE_ROD)
         .pattern(" C ")
         .pattern("CBC")
         .pattern(" R ")
         .unlockedBy(getHasName(p_308866_), this.has(p_308866_))
         .group(getItemName(p_309026_))
         .save(this.output);
   }

   protected void waxedChiseled(Block p_470646_, Block p_470715_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_470646_)
         .define('M', p_470715_)
         .pattern(" M ")
         .pattern(" M ")
         .group(getItemName(p_470646_))
         .unlockedBy(getHasName(p_470715_), this.has(p_470715_))
         .save(this.output);
   }

   protected void suspiciousStew(Item p_360920_, SuspiciousEffectHolder p_361278_) {
      ItemStack itemstack = new ItemStack(
         Items.SUSPICIOUS_STEW.builtInRegistryHolder(),
         1,
         DataComponentPatch.builder().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, p_361278_.getSuspiciousEffects()).build()
      );
      this.shapeless(RecipeCategory.FOOD, itemstack)
         .requires(Items.BOWL)
         .requires(Items.BROWN_MUSHROOM)
         .requires(Items.RED_MUSHROOM)
         .requires(p_360920_)
         .group("suspicious_stew")
         .unlockedBy(getHasName(p_360920_), this.has(p_360920_))
         .save(this.output, getItemName(itemstack.getItem()) + "_from_" + getItemName(p_360920_));
   }

   protected void generateRecipes(BlockFamily p_176582_, FeatureFlagSet p_313799_) {
      p_176582_.getVariants()
         .forEach(
            (p_359428_, p_359429_) -> {
               if (p_359429_.requiredFeatures().isSubsetOf(p_313799_)) {
                  RecipeProvider.FamilyRecipeProvider recipeprovider$familyrecipeprovider = SHAPE_BUILDERS.get(p_359428_);
                  ItemLike itemlike = this.getBaseBlock(p_176582_, p_359428_);
                  if (recipeprovider$familyrecipeprovider != null) {
                     RecipeBuilder recipebuilder = recipeprovider$familyrecipeprovider.create(this, p_359429_, itemlike);
                     p_176582_.getRecipeGroupPrefix()
                        .ifPresent(p_293701_ -> recipebuilder.group(p_293701_ + (p_359428_ == BlockFamily.Variant.CUT ? "" : "_" + p_359428_.getRecipeGroup())));
                     recipebuilder.unlockedBy(p_176582_.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), this.has(itemlike));
                     recipebuilder.save(this.output);
                  }

                  if (p_359428_ == BlockFamily.Variant.CRACKED) {
                     this.smeltingResultFromBase(p_359429_, itemlike);
                  }
               }
            }
         );
   }

   protected Block getBaseBlock(BlockFamily p_176524_, BlockFamily.Variant p_176525_) {
      if (p_176525_ == BlockFamily.Variant.CHISELED) {
         if (!p_176524_.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
            throw new IllegalStateException("Slab is not defined for the family.");
         } else {
            return p_176524_.get(BlockFamily.Variant.SLAB);
         }
      } else {
         return p_176524_.getBaseBlock();
      }
   }

   protected static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block p_125980_) {
      return CriteriaTriggers.ENTER_BLOCK
         .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(p_125980_.builtInRegistryHolder()), Optional.empty()));
   }

   protected Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints p_467374_, ItemLike p_176522_) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, p_176522_).withCount(p_467374_));
   }

   protected Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike p_125978_) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, p_125978_));
   }

   protected Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> p_206407_) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, p_206407_));
   }

   protected static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder... p_468568_) {
      return inventoryTrigger(Arrays.stream(p_468568_).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
   }

   protected static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... p_468471_) {
      return CriteriaTriggers.INVENTORY_CHANGED
         .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(p_468471_)));
   }

   protected static String getHasName(ItemLike p_176603_) {
      return "has_" + getItemName(p_176603_);
   }

   protected static String getItemName(ItemLike p_176633_) {
      return BuiltInRegistries.ITEM.getKey(p_176633_.asItem()).getPath();
   }

   protected static String getSimpleRecipeName(ItemLike p_176645_) {
      return getItemName(p_176645_);
   }

   protected static String getConversionRecipeName(ItemLike p_176518_, ItemLike p_176519_) {
      return getItemName(p_176518_) + "_from_" + getItemName(p_176519_);
   }

   protected static String getSmeltingRecipeName(ItemLike p_176657_) {
      return getItemName(p_176657_) + "_from_smelting";
   }

   protected static String getBlastingRecipeName(ItemLike p_176669_) {
      return getItemName(p_176669_) + "_from_blasting";
   }

   protected Ingredient tag(TagKey<Item> p_364630_) {
      return Ingredient.of(this.items.getOrThrow(p_364630_));
   }

   protected ShapedRecipeBuilder shaped(RecipeCategory category, ItemStack stack) {
      return ShapedRecipeBuilder.shaped(this.items, category, stack);
   }

   protected ShapedRecipeBuilder shaped(RecipeCategory p_360632_, ItemLike p_365035_) {
      return ShapedRecipeBuilder.shaped(this.items, p_360632_, p_365035_);
   }

   protected ShapedRecipeBuilder shaped(RecipeCategory p_363994_, ItemLike p_365113_, int p_362095_) {
      return ShapedRecipeBuilder.shaped(this.items, p_363994_, p_365113_, p_362095_);
   }

   protected ShapelessRecipeBuilder shapeless(RecipeCategory p_364602_, ItemStack p_361999_) {
      return ShapelessRecipeBuilder.shapeless(this.items, p_364602_, p_361999_);
   }

   protected ShapelessRecipeBuilder shapeless(RecipeCategory p_364319_, ItemLike p_364774_) {
      return ShapelessRecipeBuilder.shapeless(this.items, p_364319_, p_364774_);
   }

   protected ShapelessRecipeBuilder shapeless(RecipeCategory p_362256_, ItemLike p_363786_, int p_365368_) {
      return ShapelessRecipeBuilder.shapeless(this.items, p_362256_, p_363786_, p_365368_);
   }

   @FunctionalInterface
   interface FamilyRecipeProvider {
      RecipeBuilder create(RecipeProvider var1, ItemLike var2, ItemLike var3);
   }

   public abstract static class Runner implements DataProvider {
      private final PackOutput packOutput;
      private final CompletableFuture<HolderLookup.Provider> registries;

      protected Runner(PackOutput p_365369_, CompletableFuture<HolderLookup.Provider> p_361563_) {
         this.packOutput = p_365369_;
         this.registries = p_361563_;
      }

      @Override
      public final CompletableFuture<?> run(CachedOutput p_364823_) {
         return this.registries
            .thenCompose(
               p_364211_ -> {
                  final PackOutput.PathProvider packoutput$pathprovider = this.packOutput.createRegistryElementsPathProvider(Registries.RECIPE);
                  final PackOutput.PathProvider packoutput$pathprovider1 = this.packOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                  final Set<ResourceKey<Recipe<?>>> set = Sets.newHashSet();
                  final List<CompletableFuture<?>> list = new ArrayList<>();
                  RecipeOutput recipeoutput = new RecipeOutput() {
                     @Override
                     public void accept(ResourceKey<Recipe<?>> p_380360_, Recipe<?> p_362882_, @Nullable AdvancementHolder p_364507_, ICondition... conditions) {
                        if (!set.add(p_380360_)) {
                           throw new IllegalStateException("Duplicate recipe " + p_380360_.identifier());
                        } else {
                           this.saveRecipe(p_380360_, p_362882_, conditions);
                           if (p_364507_ != null) {
                              this.saveAdvancement(p_364507_, conditions);
                           }
                        }
                     }

                     @Override
                     public Advancement.Builder advancement() {
                        return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                     }

                     @Override
                     public void includeRootAdvancement() {
                        AdvancementHolder advancementholder = Advancement.Builder.recipeAdvancement()
                           .addCriterion("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                           .build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                        this.saveAdvancement(advancementholder);
                     }

                     private void saveRecipe(ResourceKey<Recipe<?>> p_380099_, Recipe<?> p_364792_) {
                        this.saveRecipe(p_380099_, p_364792_);
                     }

                     private void saveRecipe(ResourceKey<Recipe<?>> p_380099_, Recipe<?> p_364792_, ICondition... conditions) {
                        list.add(
                           DataProvider.saveStable(
                              p_364823_,
                              p_364211_,
                              Recipe.CONDITIONAL_CODEC,
                              Optional.of(new WithConditions<>(p_364792_, conditions)),
                              packoutput$pathprovider.json(p_380099_.identifier())
                           )
                        );
                     }

                     private void saveAdvancement(AdvancementHolder p_363148_) {
                        this.saveAdvancement(p_363148_);
                     }

                     private void saveAdvancement(AdvancementHolder p_363148_, ICondition... conditions) {
                        list.add(
                           DataProvider.saveStable(
                              p_364823_,
                              p_364211_,
                              Advancement.CONDITIONAL_CODEC,
                              Optional.of(new WithConditions<>(p_363148_.value(), conditions)),
                              packoutput$pathprovider1.json(p_363148_.id())
                           )
                        );
                     }
                  };
                  this.createRecipeProvider(p_364211_, recipeoutput).buildRecipes();
                  return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
               }
            );
      }

      protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider var1, RecipeOutput var2);
   }
}
