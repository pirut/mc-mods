package net.neoforged.neoforge.common.data.internal;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagCopyingItemTagProvider;

public final class NeoForgeItemTagsProvider extends BlockTagCopyingItemTagProvider {
   public NeoForgeItemTagsProvider(
      PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags
   ) {
      super(output, lookupProvider, blockTags, "neoforge");
   }

   @Override
   public void addTags(HolderLookup.Provider lookupProvider) {
      this.copy(Tags.Blocks.BARRELS, Tags.Items.BARRELS);
      this.copy(Tags.Blocks.BARRELS_WOODEN, Tags.Items.BARRELS_WOODEN);
      this.tag(Tags.Items.BONES).add(Items.BONE);
      this.copy(Tags.Blocks.BOOKSHELVES, Tags.Items.BOOKSHELVES);
      this.tag(Tags.Items.BRICKS).addTags(new TagKey[]{Tags.Items.BRICKS_NORMAL, Tags.Items.BRICKS_NETHER, Tags.Items.BRICKS_RESIN});
      this.tag(Tags.Items.BRICKS_NORMAL).add(Items.BRICK);
      this.tag(Tags.Items.BRICKS_NETHER).add(Items.NETHER_BRICK);
      this.tag(Tags.Items.BRICKS_RESIN).add(Items.RESIN_BRICK);
      this.tag(Tags.Items.BUCKETS_EMPTY).add(Items.BUCKET);
      this.tag(Tags.Items.BUCKETS_WATER).add(Items.WATER_BUCKET);
      this.tag(Tags.Items.BUCKETS_LAVA).add(Items.LAVA_BUCKET);
      this.tag(Tags.Items.BUCKETS_MILK).add(Items.MILK_BUCKET);
      this.tag(Tags.Items.BUCKETS_POWDER_SNOW).add(Items.POWDER_SNOW_BUCKET);
      this.tag(Tags.Items.BUCKETS_ENTITY_WATER)
         .add(Items.AXOLOTL_BUCKET, Items.COD_BUCKET, Items.PUFFERFISH_BUCKET, Items.TADPOLE_BUCKET, Items.TROPICAL_FISH_BUCKET, Items.SALMON_BUCKET);
      this.tag(Tags.Items.BUCKETS)
         .addTags(
            new TagKey[]{
               Tags.Items.BUCKETS_EMPTY,
               Tags.Items.BUCKETS_WATER,
               Tags.Items.BUCKETS_LAVA,
               Tags.Items.BUCKETS_MILK,
               Tags.Items.BUCKETS_POWDER_SNOW,
               Tags.Items.BUCKETS_ENTITY_WATER
            }
         );
      this.copy(Tags.Blocks.BUDDING_BLOCKS, Tags.Items.BUDDING_BLOCKS);
      this.copy(Tags.Blocks.BUDS, Tags.Items.BUDS);
      this.copy(Tags.Blocks.CHAINS, Tags.Items.CHAINS);
      this.copy(Tags.Blocks.CHESTS, Tags.Items.CHESTS);
      this.copy(Tags.Blocks.CHESTS_ENDER, Tags.Items.CHESTS_ENDER);
      this.copy(Tags.Blocks.CHESTS_TRAPPED, Tags.Items.CHESTS_TRAPPED);
      this.copy(Tags.Blocks.CHESTS_WOODEN, Tags.Items.CHESTS_WOODEN);
      this.copy(Tags.Blocks.CLUSTERS, Tags.Items.CLUSTERS);
      this.tag(Tags.Items.CLUMPS).addTags(new TagKey[]{Tags.Items.CLUMPS_RESIN});
      this.tag(Tags.Items.CLUMPS_RESIN).add(Items.RESIN_CLUMP);
      this.copy(Tags.Blocks.COBBLESTONES, Tags.Items.COBBLESTONES);
      this.copy(Tags.Blocks.COBBLESTONES_NORMAL, Tags.Items.COBBLESTONES_NORMAL);
      this.copy(Tags.Blocks.COBBLESTONES_INFESTED, Tags.Items.COBBLESTONES_INFESTED);
      this.copy(Tags.Blocks.COBBLESTONES_MOSSY, Tags.Items.COBBLESTONES_MOSSY);
      this.copy(Tags.Blocks.COBBLESTONES_DEEPSLATE, Tags.Items.COBBLESTONES_DEEPSLATE);
      this.copy(Tags.Blocks.CONCRETES, Tags.Items.CONCRETES);
      this.tag(Tags.Items.CONCRETE_POWDERS)
         .add(Items.WHITE_CONCRETE_POWDER)
         .add(Items.ORANGE_CONCRETE_POWDER)
         .add(Items.MAGENTA_CONCRETE_POWDER)
         .add(Items.LIGHT_BLUE_CONCRETE_POWDER)
         .add(Items.YELLOW_CONCRETE_POWDER)
         .add(Items.LIME_CONCRETE_POWDER)
         .add(Items.PINK_CONCRETE_POWDER)
         .add(Items.GRAY_CONCRETE_POWDER)
         .add(Items.LIGHT_GRAY_CONCRETE_POWDER)
         .add(Items.CYAN_CONCRETE_POWDER)
         .add(Items.PURPLE_CONCRETE_POWDER)
         .add(Items.BLUE_CONCRETE_POWDER)
         .add(Items.BROWN_CONCRETE_POWDER)
         .add(Items.GREEN_CONCRETE_POWDER)
         .add(Items.RED_CONCRETE_POWDER)
         .add(Items.BLACK_CONCRETE_POWDER);
      this.tag(Tags.Items.CROPS)
         .addTags(
            new TagKey[]{
               Tags.Items.CROPS_BEETROOT,
               Tags.Items.CROPS_CACTUS,
               Tags.Items.CROPS_CARROT,
               Tags.Items.CROPS_COCOA_BEAN,
               Tags.Items.CROPS_MELON,
               Tags.Items.CROPS_NETHER_WART,
               Tags.Items.CROPS_POTATO,
               Tags.Items.CROPS_PUMPKIN,
               Tags.Items.CROPS_SUGAR_CANE,
               Tags.Items.CROPS_WHEAT
            }
         );
      this.tag(Tags.Items.CROPS_BEETROOT).add(Items.BEETROOT);
      this.tag(Tags.Items.CROPS_CACTUS).add(Items.CACTUS);
      this.tag(Tags.Items.CROPS_CARROT).add(Items.CARROT);
      this.tag(Tags.Items.CROPS_COCOA_BEAN).add(Items.COCOA_BEANS);
      this.tag(Tags.Items.CROPS_MELON).add(Items.MELON);
      this.tag(Tags.Items.CROPS_NETHER_WART).add(Items.NETHER_WART);
      this.tag(Tags.Items.CROPS_POTATO).add(Items.POTATO);
      this.tag(Tags.Items.CROPS_PUMPKIN).add(Items.PUMPKIN);
      this.tag(Tags.Items.CROPS_SUGAR_CANE).add(Items.SUGAR_CANE);
      this.tag(Tags.Items.CROPS_WHEAT).add(Items.WHEAT);
      this.tag(Tags.Items.DRINKS)
         .addTags(
            new TagKey[]{
               Tags.Items.DRINKS_WATER,
               Tags.Items.DRINKS_WATERY,
               Tags.Items.DRINKS_MILK,
               Tags.Items.DRINKS_HONEY,
               Tags.Items.DRINKS_MAGIC,
               Tags.Items.DRINKS_OMINOUS,
               Tags.Items.DRINKS_JUICE
            }
         );
      this.tag(Tags.Items.DRINKS_WATER);
      this.tag(Tags.Items.DRINKS_WATERY).add(Items.POTION).addTags(new TagKey[]{Tags.Items.DRINKS_WATER});
      this.tag(Tags.Items.DRINKS_MILK).add(Items.MILK_BUCKET);
      this.tag(Tags.Items.DRINKS_HONEY).add(Items.HONEY_BOTTLE);
      this.tag(Tags.Items.DRINKS_MAGIC).add(Items.POTION).addTags(new TagKey[]{Tags.Items.DRINKS_OMINOUS});
      this.tag(Tags.Items.DRINKS_OMINOUS).add(Items.OMINOUS_BOTTLE);
      this.tag(Tags.Items.DRINKS_JUICE);
      this.addColored(Tags.Items.DYED, "{color}_banner");
      this.addColored(Tags.Items.DYED, "{color}_bed");
      this.addColored(Tags.Items.DYED, "{color}_candle");
      this.addColored(Tags.Items.DYED, "{color}_carpet");
      this.addColored(Tags.Items.DYED, "{color}_concrete");
      this.addColored(Tags.Items.DYED, "{color}_concrete_powder");
      this.addColored(Tags.Items.DYED, "{color}_glazed_terracotta");
      this.addColored(Tags.Items.DYED, "{color}_shulker_box");
      this.addColored(Tags.Items.DYED, "{color}_stained_glass");
      this.addColored(Tags.Items.DYED, "{color}_stained_glass_pane");
      this.addColored(Tags.Items.DYED, "{color}_terracotta");
      this.addColored(Tags.Items.DYED, "{color}_wool");
      this.addColoredTags(xva$0 -> rec$.addTags(new TagKey[]{xva$0}), Tags.Items.DYED);
      this.tag(Tags.Items.DUSTS).addTags(new TagKey[]{Tags.Items.DUSTS_GLOWSTONE, Tags.Items.DUSTS_REDSTONE});
      this.tag(Tags.Items.DUSTS_GLOWSTONE).add(Items.GLOWSTONE_DUST);
      this.tag(Tags.Items.DUSTS_REDSTONE).add(Items.REDSTONE);
      this.addColored(Tags.Items.DYES, "{color}_dye");
      this.addColoredTags(xva$0 -> rec$.addTags(new TagKey[]{xva$0}), Tags.Items.DYES);
      this.tag(Tags.Items.EGGS).add(Items.EGG, Items.BROWN_EGG, Items.BLUE_EGG);
      this.tag(Tags.Items.ENCHANTING_FUELS).addTag(Tags.Items.GEMS_LAPIS);
      this.copy(Tags.Blocks.END_STONES, Tags.Items.END_STONES);
      this.tag(Tags.Items.ENDER_PEARLS).add(Items.ENDER_PEARL);
      this.tag(Tags.Items.FEATHERS).add(Items.FEATHER);
      this.copy(Tags.Blocks.FENCE_GATES, Tags.Items.FENCE_GATES);
      this.copy(Tags.Blocks.FENCE_GATES_WOODEN, Tags.Items.FENCE_GATES_WOODEN);
      this.copy(Tags.Blocks.FENCES, Tags.Items.FENCES);
      this.copy(Tags.Blocks.FENCES_NETHER_BRICK, Tags.Items.FENCES_NETHER_BRICK);
      this.copy(Tags.Blocks.FENCES_WOODEN, Tags.Items.FENCES_WOODEN);
      this.tag(Tags.Items.FERTILIZERS).add(Items.BONE_MEAL);
      this.copy(Tags.Blocks.FLOWERS_SMALL, Tags.Items.FLOWERS_SMALL);
      this.copy(Tags.Blocks.FLOWERS_TALL, Tags.Items.FLOWERS_TALL);
      this.copy(Tags.Blocks.FLOWERS, Tags.Items.FLOWERS);
      this.tag(Tags.Items.FOODS_FRUIT).add(Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.CHORUS_FRUIT, Items.MELON_SLICE);
      this.tag(Tags.Items.FOODS_VEGETABLE).add(Items.CARROT, Items.GOLDEN_CARROT, Items.POTATO, Items.BEETROOT);
      this.tag(Tags.Items.FOODS_BERRY).add(Items.SWEET_BERRIES, Items.GLOW_BERRIES);
      this.tag(Tags.Items.FOODS_BREAD).add(Items.BREAD);
      this.tag(Tags.Items.FOODS_COOKIE).add(Items.COOKIE);
      this.tag(Tags.Items.FOODS_DOUGH);
      this.tag(Tags.Items.FOODS_RAW_MEAT).add(Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.RABBIT, Items.MUTTON);
      this.tag(Tags.Items.FOODS_RAW_FISH).add(Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH);
      this.tag(Tags.Items.FOODS_COOKED_MEAT).add(Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN, Items.COOKED_RABBIT, Items.COOKED_MUTTON);
      this.tag(Tags.Items.FOODS_COOKED_FISH).add(Items.COOKED_COD, Items.COOKED_SALMON);
      this.tag(Tags.Items.FOODS_SOUP).add(Items.BEETROOT_SOUP, Items.MUSHROOM_STEW, Items.RABBIT_STEW, Items.SUSPICIOUS_STEW);
      this.tag(Tags.Items.FOODS_CANDY);
      this.tag(Tags.Items.FOODS_PIE).add(Items.PUMPKIN_PIE);
      this.tag(Tags.Items.FOODS_GOLDEN).add(Items.GOLDEN_APPLE).add(Items.ENCHANTED_GOLDEN_APPLE).add(Items.GOLDEN_CARROT);
      this.tag(Tags.Items.FOODS_EDIBLE_WHEN_PLACED).add(Items.CAKE);
      this.tag(Tags.Items.FOODS_FOOD_POISONING).add(Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.SPIDER_EYE, Items.CHICKEN, Items.ROTTEN_FLESH);
      this.tag(Tags.Items.FOODS)
         .add(Items.BAKED_POTATO, Items.PUMPKIN_PIE, Items.HONEY_BOTTLE, Items.OMINOUS_BOTTLE, Items.DRIED_KELP)
         .addTags(
            new TagKey[]{
               Tags.Items.FOODS_FRUIT,
               Tags.Items.FOODS_VEGETABLE,
               Tags.Items.FOODS_BERRY,
               Tags.Items.FOODS_BREAD,
               Tags.Items.FOODS_COOKIE,
               Tags.Items.FOODS_DOUGH,
               Tags.Items.FOODS_RAW_MEAT,
               Tags.Items.FOODS_RAW_FISH,
               Tags.Items.FOODS_COOKED_MEAT,
               Tags.Items.FOODS_COOKED_FISH,
               Tags.Items.FOODS_SOUP,
               Tags.Items.FOODS_CANDY,
               Tags.Items.FOODS_PIE,
               Tags.Items.FOODS_GOLDEN,
               Tags.Items.FOODS_EDIBLE_WHEN_PLACED,
               Tags.Items.FOODS_FOOD_POISONING
            }
         );
      this.tag(Tags.Items.ANIMAL_FOODS)
         .addTags(
            new TagKey[]{
               ItemTags.ARMADILLO_FOOD,
               ItemTags.AXOLOTL_FOOD,
               ItemTags.BEE_FOOD,
               ItemTags.CAMEL_FOOD,
               ItemTags.CAT_FOOD,
               ItemTags.CHICKEN_FOOD,
               ItemTags.COW_FOOD,
               ItemTags.FOX_FOOD,
               ItemTags.FROG_FOOD,
               ItemTags.GOAT_FOOD,
               ItemTags.HOGLIN_FOOD,
               ItemTags.HORSE_FOOD,
               ItemTags.LLAMA_FOOD,
               ItemTags.OCELOT_FOOD,
               ItemTags.PANDA_FOOD,
               ItemTags.PARROT_FOOD,
               ItemTags.PIG_FOOD,
               ItemTags.PIGLIN_FOOD,
               ItemTags.RABBIT_FOOD,
               ItemTags.SHEEP_FOOD,
               ItemTags.SNIFFER_FOOD,
               ItemTags.STRIDER_FOOD,
               ItemTags.TURTLE_FOOD,
               ItemTags.WOLF_FOOD
            }
         );
      this.tag(Tags.Items.GEMS)
         .addTags(
            new TagKey[]{
               Tags.Items.GEMS_AMETHYST,
               Tags.Items.GEMS_DIAMOND,
               Tags.Items.GEMS_EMERALD,
               Tags.Items.GEMS_LAPIS,
               Tags.Items.GEMS_PRISMARINE,
               Tags.Items.GEMS_QUARTZ
            }
         );
      this.tag(Tags.Items.GEMS_AMETHYST).add(Items.AMETHYST_SHARD);
      this.tag(Tags.Items.GEMS_DIAMOND).add(Items.DIAMOND);
      this.tag(Tags.Items.GEMS_EMERALD).add(Items.EMERALD);
      this.tag(Tags.Items.GEMS_LAPIS).add(Items.LAPIS_LAZULI);
      this.tag(Tags.Items.GEMS_PRISMARINE).add(Items.PRISMARINE_CRYSTALS);
      this.tag(Tags.Items.GEMS_QUARTZ).add(Items.QUARTZ);
      this.copy(Tags.Blocks.GLASS_BLOCKS, Tags.Items.GLASS_BLOCKS);
      this.copy(Tags.Blocks.GLASS_BLOCKS_COLORLESS, Tags.Items.GLASS_BLOCKS_COLORLESS);
      this.copy(Tags.Blocks.GLASS_BLOCKS_TINTED, Tags.Items.GLASS_BLOCKS_TINTED);
      this.copy(Tags.Blocks.GLASS_BLOCKS_CHEAP, Tags.Items.GLASS_BLOCKS_CHEAP);
      this.copy(Tags.Blocks.GLASS_PANES, Tags.Items.GLASS_PANES);
      this.copy(Tags.Blocks.GLASS_PANES_COLORLESS, Tags.Items.GLASS_PANES_COLORLESS);
      this.copy(Tags.Blocks.GLAZED_TERRACOTTAS, Tags.Items.GLAZED_TERRACOTTAS);
      this.copy(Tags.Blocks.GRAVELS, Tags.Items.GRAVELS);
      this.tag(Tags.Items.GUNPOWDERS).add(Items.GUNPOWDER);
      this.tag(Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS);
      this.tag(Tags.Items.INGOTS).addTags(new TagKey[]{Tags.Items.INGOTS_COPPER, Tags.Items.INGOTS_GOLD, Tags.Items.INGOTS_IRON, Tags.Items.INGOTS_NETHERITE});
      this.tag(Tags.Items.INGOTS_COPPER).add(Items.COPPER_INGOT);
      this.tag(Tags.Items.INGOTS_GOLD).add(Items.GOLD_INGOT);
      this.tag(Tags.Items.INGOTS_IRON).add(Items.IRON_INGOT);
      this.tag(Tags.Items.INGOTS_NETHERITE).add(Items.NETHERITE_INGOT);
      this.tag(Tags.Items.LEATHERS).add(Items.LEATHER);
      this.tag(Tags.Items.MUSHROOMS).add(Items.BROWN_MUSHROOM, Items.RED_MUSHROOM);
      this.tag(Tags.Items.MUSIC_DISCS)
         .add(
            Items.MUSIC_DISC_13,
            Items.MUSIC_DISC_CAT,
            Items.MUSIC_DISC_BLOCKS,
            Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR,
            Items.MUSIC_DISC_MALL,
            Items.MUSIC_DISC_MELLOHI,
            Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD,
            Items.MUSIC_DISC_WARD,
            Items.MUSIC_DISC_11,
            Items.MUSIC_DISC_WAIT,
            Items.MUSIC_DISC_OTHERSIDE,
            Items.MUSIC_DISC_5,
            Items.MUSIC_DISC_PIGSTEP,
            Items.MUSIC_DISC_RELIC,
            Items.MUSIC_DISC_CREATOR,
            Items.MUSIC_DISC_CREATOR_MUSIC_BOX,
            Items.MUSIC_DISC_PRECIPICE,
            Items.MUSIC_DISC_TEARS,
            Items.MUSIC_DISC_LAVA_CHICKEN
         );
      this.tag(Tags.Items.NETHER_STARS).add(Items.NETHER_STAR);
      this.copy(Tags.Blocks.NETHERRACKS, Tags.Items.NETHERRACKS);
      this.tag(Tags.Items.NUGGETS).addTags(new TagKey[]{Tags.Items.NUGGETS_COPPER, Tags.Items.NUGGETS_IRON, Tags.Items.NUGGETS_GOLD});
      this.tag(Tags.Items.NUGGETS_COPPER).add(Items.COPPER_NUGGET);
      this.tag(Tags.Items.NUGGETS_IRON).add(Items.IRON_NUGGET);
      this.tag(Tags.Items.NUGGETS_GOLD).add(Items.GOLD_NUGGET);
      this.copy(Tags.Blocks.OBSIDIANS, Tags.Items.OBSIDIANS);
      this.copy(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE, Tags.Items.ORE_BEARING_GROUND_DEEPSLATE);
      this.copy(Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK, Tags.Items.ORE_BEARING_GROUND_NETHERRACK);
      this.copy(Tags.Blocks.ORE_BEARING_GROUND_STONE, Tags.Items.ORE_BEARING_GROUND_STONE);
      this.copy(Tags.Blocks.ORE_RATES_DENSE, Tags.Items.ORE_RATES_DENSE);
      this.copy(Tags.Blocks.ORE_RATES_SINGULAR, Tags.Items.ORE_RATES_SINGULAR);
      this.copy(Tags.Blocks.ORE_RATES_SPARSE, Tags.Items.ORE_RATES_SPARSE);
      this.copy(Tags.Blocks.ORES, Tags.Items.ORES);
      this.copy(Tags.Blocks.ORES_COAL, Tags.Items.ORES_COAL);
      this.copy(Tags.Blocks.ORES_COPPER, Tags.Items.ORES_COPPER);
      this.copy(Tags.Blocks.ORES_DIAMOND, Tags.Items.ORES_DIAMOND);
      this.copy(Tags.Blocks.ORES_EMERALD, Tags.Items.ORES_EMERALD);
      this.copy(Tags.Blocks.ORES_GOLD, Tags.Items.ORES_GOLD);
      this.copy(Tags.Blocks.ORES_IRON, Tags.Items.ORES_IRON);
      this.copy(Tags.Blocks.ORES_LAPIS, Tags.Items.ORES_LAPIS);
      this.copy(Tags.Blocks.ORES_QUARTZ, Tags.Items.ORES_QUARTZ);
      this.copy(Tags.Blocks.ORES_REDSTONE, Tags.Items.ORES_REDSTONE);
      this.copy(Tags.Blocks.ORES_NETHERITE_SCRAP, Tags.Items.ORES_NETHERITE_SCRAP);
      this.copy(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, Tags.Items.ORES_IN_GROUND_DEEPSLATE);
      this.copy(Tags.Blocks.ORES_IN_GROUND_NETHERRACK, Tags.Items.ORES_IN_GROUND_NETHERRACK);
      this.copy(Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Items.ORES_IN_GROUND_STONE);
      this.tag(Tags.Items.PIGLIN_USABLE_CROSSBOWS).addTag(Tags.Items.TOOLS_CROSSBOW);
      this.tag(Tags.Items.PILLAGER_USABLE_CROSSBOWS).addTags(new TagKey[]{Tags.Items.TOOLS_CROSSBOW});
      this.copy(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES, Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES);
      this.copy(Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES, Tags.Items.PLAYER_WORKSTATIONS_FURNACES);
      this.tag(Tags.Items.POTIONS_BOTTLE).add(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
      this.tag(Tags.Items.POTIONS).addTags(new TagKey[]{Tags.Items.POTIONS_BOTTLE});
      this.copy(Tags.Blocks.OBSIDIANS_NORMAL, Tags.Items.OBSIDIANS_NORMAL);
      this.copy(Tags.Blocks.OBSIDIANS_CRYING, Tags.Items.OBSIDIANS_CRYING);
      this.copy(Tags.Blocks.PUMPKINS, Tags.Items.PUMPKINS);
      this.copy(Tags.Blocks.PUMPKINS_NORMAL, Tags.Items.PUMPKINS_NORMAL);
      this.copy(Tags.Blocks.PUMPKINS_CARVED, Tags.Items.PUMPKINS_CARVED);
      this.copy(Tags.Blocks.PUMPKINS_JACK_O_LANTERNS, Tags.Items.PUMPKINS_JACK_O_LANTERNS);
      this.tag(Tags.Items.RAW_MATERIALS).addTags(new TagKey[]{Tags.Items.RAW_MATERIALS_COPPER, Tags.Items.RAW_MATERIALS_GOLD, Tags.Items.RAW_MATERIALS_IRON});
      this.tag(Tags.Items.RAW_MATERIALS_COPPER).add(Items.RAW_COPPER);
      this.tag(Tags.Items.RAW_MATERIALS_GOLD).add(Items.RAW_GOLD);
      this.tag(Tags.Items.RAW_MATERIALS_IRON).add(Items.RAW_IRON);
      this.tag(Tags.Items.RODS).addTags(new TagKey[]{Tags.Items.RODS_WOODEN, Tags.Items.RODS_BLAZE, Tags.Items.RODS_BREEZE});
      this.tag(Tags.Items.RODS_BLAZE).add(Items.BLAZE_ROD);
      this.tag(Tags.Items.RODS_BREEZE).add(Items.BREEZE_ROD);
      this.tag(Tags.Items.RODS_WOODEN).add(Items.STICK);
      this.copy(Tags.Blocks.ROPES, Tags.Items.ROPES);
      this.copy(Tags.Blocks.SANDS, Tags.Items.SANDS);
      this.copy(Tags.Blocks.SANDS_COLORLESS, Tags.Items.SANDS_COLORLESS);
      this.copy(Tags.Blocks.SANDS_RED, Tags.Items.SANDS_RED);
      this.copy(Tags.Blocks.SANDSTONE_BLOCKS, Tags.Items.SANDSTONE_BLOCKS);
      this.copy(Tags.Blocks.SANDSTONE_SLABS, Tags.Items.SANDSTONE_SLABS);
      this.copy(Tags.Blocks.SANDSTONE_STAIRS, Tags.Items.SANDSTONE_STAIRS);
      this.copy(Tags.Blocks.SANDSTONE_RED_BLOCKS, Tags.Items.SANDSTONE_RED_BLOCKS);
      this.copy(Tags.Blocks.SANDSTONE_RED_SLABS, Tags.Items.SANDSTONE_RED_SLABS);
      this.copy(Tags.Blocks.SANDSTONE_RED_STAIRS, Tags.Items.SANDSTONE_RED_STAIRS);
      this.copy(Tags.Blocks.SANDSTONE_UNCOLORED_BLOCKS, Tags.Items.SANDSTONE_UNCOLORED_BLOCKS);
      this.copy(Tags.Blocks.SANDSTONE_UNCOLORED_SLABS, Tags.Items.SANDSTONE_UNCOLORED_SLABS);
      this.copy(Tags.Blocks.SANDSTONE_UNCOLORED_STAIRS, Tags.Items.SANDSTONE_UNCOLORED_STAIRS);
      this.tag(Tags.Items.SEEDS)
         .addTags(
            new TagKey[]{
               Tags.Items.SEEDS_BEETROOT,
               Tags.Items.SEEDS_MELON,
               Tags.Items.SEEDS_PUMPKIN,
               Tags.Items.SEEDS_TORCHFLOWER,
               Tags.Items.SEEDS_PITCHER_PLANT,
               Tags.Items.SEEDS_WHEAT
            }
         );
      this.tag(Tags.Items.SEEDS_BEETROOT).add(Items.BEETROOT_SEEDS);
      this.tag(Tags.Items.SEEDS_MELON).add(Items.MELON_SEEDS);
      this.tag(Tags.Items.SEEDS_PUMPKIN).add(Items.PUMPKIN_SEEDS);
      this.tag(Tags.Items.SEEDS_TORCHFLOWER).add(Items.TORCHFLOWER_SEEDS);
      this.tag(Tags.Items.SEEDS_PITCHER_PLANT).add(Items.PITCHER_POD);
      this.tag(Tags.Items.SEEDS_WHEAT).add(Items.WHEAT_SEEDS);
      this.tag(Tags.Items.SKELETON_USABLE_BOWS).addTag(Tags.Items.TOOLS_BOW);
      this.tag(Tags.Items.SLIME_BALLS).add(Items.SLIME_BALL);
      this.tag(Tags.Items.SHULKER_BOXES)
         .add(Items.SHULKER_BOX)
         .add(Items.WHITE_SHULKER_BOX)
         .add(Items.ORANGE_SHULKER_BOX)
         .add(Items.MAGENTA_SHULKER_BOX)
         .add(Items.LIGHT_BLUE_SHULKER_BOX)
         .add(Items.YELLOW_SHULKER_BOX)
         .add(Items.LIME_SHULKER_BOX)
         .add(Items.PINK_SHULKER_BOX)
         .add(Items.GRAY_SHULKER_BOX)
         .add(Items.LIGHT_GRAY_SHULKER_BOX)
         .add(Items.CYAN_SHULKER_BOX)
         .add(Items.PURPLE_SHULKER_BOX)
         .add(Items.BLUE_SHULKER_BOX)
         .add(Items.BROWN_SHULKER_BOX)
         .add(Items.GREEN_SHULKER_BOX)
         .add(Items.RED_SHULKER_BOX)
         .add(Items.BLACK_SHULKER_BOX);
      this.copy(Tags.Blocks.STONES, Tags.Items.STONES);
      this.copy(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_BONE_MEAL, Tags.Items.STORAGE_BLOCKS_BONE_MEAL);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_COAL, Tags.Items.STORAGE_BLOCKS_COAL);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_COPPER, Tags.Items.STORAGE_BLOCKS_COPPER);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, Tags.Items.STORAGE_BLOCKS_DIAMOND);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_DRIED_KELP, Tags.Items.STORAGE_BLOCKS_DRIED_KELP);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_EMERALD, Tags.Items.STORAGE_BLOCKS_EMERALD);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_GOLD, Tags.Items.STORAGE_BLOCKS_GOLD);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_IRON, Tags.Items.STORAGE_BLOCKS_IRON);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_LAPIS, Tags.Items.STORAGE_BLOCKS_LAPIS);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_NETHERITE, Tags.Items.STORAGE_BLOCKS_NETHERITE);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER, Tags.Items.STORAGE_BLOCKS_RAW_COPPER);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD, Tags.Items.STORAGE_BLOCKS_RAW_GOLD);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON, Tags.Items.STORAGE_BLOCKS_RAW_IRON);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_REDSTONE, Tags.Items.STORAGE_BLOCKS_REDSTONE);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_RESIN, Tags.Items.STORAGE_BLOCKS_RESIN);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_SLIME, Tags.Items.STORAGE_BLOCKS_SLIME);
      this.copy(Tags.Blocks.STORAGE_BLOCKS_WHEAT, Tags.Items.STORAGE_BLOCKS_WHEAT);
      this.tag(Tags.Items.STRINGS).add(Items.STRING);
      this.copy(Tags.Blocks.OVERWORLD_NATURAL_LOGS, Tags.Items.OVERWORLD_NATURAL_LOGS);
      this.copy(Tags.Blocks.NETHER_NATURAL_LOGS, Tags.Items.NETHER_NATURAL_LOGS);
      this.copy(Tags.Blocks.NATURAL_LOGS, Tags.Items.NATURAL_LOGS);
      this.copy(Tags.Blocks.NATURAL_WOODS, Tags.Items.NATURAL_WOODS);
      this.copy(Tags.Blocks.STRIPPED_LOGS, Tags.Items.STRIPPED_LOGS);
      this.copy(Tags.Blocks.STRIPPED_WOODS, Tags.Items.STRIPPED_WOODS);
      this.tag(Tags.Items.VILLAGER_JOB_SITES)
         .add(
            Items.BARREL,
            Items.BLAST_FURNACE,
            Items.BREWING_STAND,
            Items.CARTOGRAPHY_TABLE,
            Items.CAULDRON,
            Items.COMPOSTER,
            Items.FLETCHING_TABLE,
            Items.GRINDSTONE,
            Items.LECTERN,
            Items.LOOM,
            Items.SMITHING_TABLE,
            Items.SMOKER,
            Items.STONECUTTER
         );
      this.tag(Tags.Items.TOOLS_SHIELD).add(Items.SHIELD);
      this.tag(Tags.Items.TOOLS_BOW).add(Items.BOW);
      this.tag(Tags.Items.TOOLS_BRUSH).add(Items.BRUSH);
      this.tag(Tags.Items.TOOLS_MACE).add(Items.MACE);
      this.tag(Tags.Items.TOOLS_CROSSBOW).add(Items.CROSSBOW);
      this.tag(Tags.Items.TOOLS_FISHING_ROD).add(Items.FISHING_ROD);
      this.tag(Tags.Items.TOOLS_SHEAR).add(Items.SHEARS);
      this.tag(Tags.Items.TOOLS_SPEAR).add(Items.TRIDENT);
      this.tag(Tags.Items.TOOLS_IGNITER).add(Items.FLINT_AND_STEEL);
      this.tag(Tags.Items.TOOLS_WRENCH);
      this.tag(Tags.Items.MINING_TOOL_TOOLS)
         .add(
            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.COPPER_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.IRON_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE
         );
      this.tag(Tags.Items.MELEE_WEAPON_TOOLS)
         .add(
            Items.MACE,
            Items.TRIDENT,
            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.COPPER_SWORD,
            Items.GOLDEN_SWORD,
            Items.IRON_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD,
            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.COPPER_AXE,
            Items.GOLDEN_AXE,
            Items.IRON_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE
         );
      this.tag(Tags.Items.RANGED_WEAPON_TOOLS).add(Items.BOW, Items.CROSSBOW, Items.TRIDENT);
      this.tag(Tags.Items.TOOLS)
         .addTags(new TagKey[]{ItemTags.AXES, ItemTags.HOES, ItemTags.PICKAXES, ItemTags.SHOVELS, ItemTags.SWORDS})
         .addTags(
            new TagKey[]{
               Tags.Items.TOOLS_BOW,
               Tags.Items.TOOLS_BRUSH,
               Tags.Items.TOOLS_CROSSBOW,
               Tags.Items.TOOLS_FISHING_ROD,
               Tags.Items.TOOLS_IGNITER,
               Tags.Items.TOOLS_SHEAR,
               Tags.Items.TOOLS_SHIELD,
               Tags.Items.TOOLS_SPEAR,
               Tags.Items.TOOLS_MACE,
               Tags.Items.TOOLS_WRENCH,
               Tags.Items.MINING_TOOL_TOOLS,
               Tags.Items.MELEE_WEAPON_TOOLS,
               Tags.Items.RANGED_WEAPON_TOOLS
            }
         );
      this.tag(Tags.Items.ARMORS)
         .addOptionalTag(Tags.Items.ARMORS_HUMANOID)
         .addOptionalTag(Tags.Items.ARMORS_HORSE)
         .addOptionalTag(Tags.Items.ARMORS_NAUTILUS)
         .addOptionalTag(Tags.Items.ARMORS_WOLF);
      this.tag(Tags.Items.ARMORS_HORSE)
         .add(Items.LEATHER_HORSE_ARMOR)
         .add(Items.COPPER_HORSE_ARMOR)
         .add(Items.IRON_HORSE_ARMOR)
         .add(Items.GOLDEN_HORSE_ARMOR)
         .add(Items.DIAMOND_HORSE_ARMOR)
         .add(Items.NETHERITE_HORSE_ARMOR);
      this.tag(Tags.Items.ARMORS_NAUTILUS)
         .add(Items.COPPER_NAUTILUS_ARMOR)
         .add(Items.IRON_NAUTILUS_ARMOR)
         .add(Items.GOLDEN_NAUTILUS_ARMOR)
         .add(Items.DIAMOND_NAUTILUS_ARMOR)
         .add(Items.NETHERITE_NAUTILUS_ARMOR);
      this.tag(Tags.Items.ARMORS_WOLF).add(Items.WOLF_ARMOR);
      this.tag(Tags.Items.ARMORS_HUMANOID).addTags(new TagKey[]{ItemTags.HEAD_ARMOR, ItemTags.CHEST_ARMOR, ItemTags.LEG_ARMOR, ItemTags.FOOT_ARMOR});
      this.tag(Tags.Items.ENCHANTABLES)
         .addTags(
            new TagKey[]{
               ItemTags.ARMOR_ENCHANTABLE,
               ItemTags.EQUIPPABLE_ENCHANTABLE,
               ItemTags.WEAPON_ENCHANTABLE,
               ItemTags.MELEE_WEAPON_ENCHANTABLE,
               ItemTags.SHARP_WEAPON_ENCHANTABLE,
               ItemTags.SWEEPING_ENCHANTABLE,
               ItemTags.MINING_ENCHANTABLE,
               ItemTags.MINING_LOOT_ENCHANTABLE,
               ItemTags.FISHING_ENCHANTABLE,
               ItemTags.TRIDENT_ENCHANTABLE,
               ItemTags.BOW_ENCHANTABLE,
               ItemTags.CROSSBOW_ENCHANTABLE,
               ItemTags.MACE_ENCHANTABLE,
               ItemTags.FIRE_ASPECT_ENCHANTABLE,
               ItemTags.DURABILITY_ENCHANTABLE,
               ItemTags.VANISHING_ENCHANTABLE
            }
         );
      this.tagWithOptionalLegacy(Tags.Items.BONES);
      this.tag(Tags.Items.BRICKS_NORMAL).addOptionalTag(forge("ingots/brick"));
      this.tag(Tags.Items.BRICKS_NETHER).addOptionalTag(forge("ingots/nether_brick"));
      this.tagWithOptionalLegacy(Tags.Items.CROPS);
      this.tagWithOptionalLegacy(Tags.Items.CROPS_BEETROOT);
      this.tagWithOptionalLegacy(Tags.Items.CROPS_CARROT);
      this.tagWithOptionalLegacy(Tags.Items.CROPS_NETHER_WART);
      this.tagWithOptionalLegacy(Tags.Items.CROPS_POTATO);
      this.tagWithOptionalLegacy(Tags.Items.CROPS_WHEAT);
      this.tagWithOptionalLegacy(Tags.Items.DUSTS);
      this.tagWithOptionalLegacy(Tags.Items.DUSTS_GLOWSTONE);
      this.tagWithOptionalLegacy(Tags.Items.DUSTS_REDSTONE);
      this.tagColoredWithOptionalLegacy(Tags.Items.DYES);
      this.tag(Tags.Items.DYED_BLACK).addOptionalTag(forge("glass/black")).addOptionalTag(forge("stained_glass/black"));
      this.tag(Tags.Items.DYED_BLUE).addOptionalTag(forge("glass/blue")).addOptionalTag(forge("stained_glass/blue"));
      this.tag(Tags.Items.DYED_BROWN).addOptionalTag(forge("glass/brown")).addOptionalTag(forge("stained_glass/brown"));
      this.tag(Tags.Items.DYED_CYAN).addOptionalTag(forge("glass/cyan")).addOptionalTag(forge("stained_glass/cyan"));
      this.tag(Tags.Items.DYED_GRAY).addOptionalTag(forge("glass/gray")).addOptionalTag(forge("stained_glass/gray"));
      this.tag(Tags.Items.DYED_GREEN).addOptionalTag(forge("glass/green")).addOptionalTag(forge("stained_glass/green"));
      this.tag(Tags.Items.DYED_LIGHT_BLUE).addOptionalTag(forge("glass/light_blue")).addOptionalTag(forge("stained_glass/light_blue"));
      this.tag(Tags.Items.DYED_LIGHT_GRAY).addOptionalTag(forge("glass/light_gray")).addOptionalTag(forge("stained_glass/light_gray"));
      this.tag(Tags.Items.DYED_LIME).addOptionalTag(forge("glass/lime")).addOptionalTag(forge("stained_glass/lime"));
      this.tag(Tags.Items.DYED_MAGENTA).addOptionalTag(forge("glass/magenta")).addOptionalTag(forge("stained_glass/magenta"));
      this.tag(Tags.Items.DYED_MAGENTA).addOptionalTag(forge("glass/magenta")).addOptionalTag(forge("stained_glass/magenta"));
      this.tag(Tags.Items.DYED_ORANGE).addOptionalTag(forge("glass/orange")).addOptionalTag(forge("stained_glass/orange"));
      this.tag(Tags.Items.DYED_PINK).addOptionalTag(forge("glass/pink")).addOptionalTag(forge("stained_glass/pink"));
      this.tag(Tags.Items.DYED_PURPLE).addOptionalTag(forge("glass/purple")).addOptionalTag(forge("stained_glass/purple"));
      this.tag(Tags.Items.DYED_RED).addOptionalTag(forge("glass/red")).addOptionalTag(forge("stained_glass/red"));
      this.tag(Tags.Items.DYED_WHITE).addOptionalTag(forge("glass/white")).addOptionalTag(forge("stained_glass/white"));
      this.tag(Tags.Items.DYED_YELLOW).addOptionalTag(forge("glass/yellow")).addOptionalTag(forge("stained_glass/yellow"));
      this.tagWithOptionalLegacy(Tags.Items.ENDER_PEARLS);
      this.tagWithOptionalLegacy(Tags.Items.FEATHERS);
      this.tagWithOptionalLegacy(Tags.Items.GEMS);
      this.tagWithOptionalLegacy(Tags.Items.GEMS_AMETHYST);
      this.tagWithOptionalLegacy(Tags.Items.GEMS_DIAMOND);
      this.tagWithOptionalLegacy(Tags.Items.GEMS_EMERALD);
      this.tagWithOptionalLegacy(Tags.Items.GEMS_LAPIS);
      this.tagWithOptionalLegacy(Tags.Items.GEMS_PRISMARINE);
      this.tagWithOptionalLegacy(Tags.Items.GEMS_QUARTZ);
      this.tag(Tags.Items.GUNPOWDERS).addOptionalTag(forge("gunpowder"));
      this.tagWithOptionalLegacy(Tags.Items.INGOTS);
      this.tagWithOptionalLegacy(Tags.Items.INGOTS_COPPER);
      this.tagWithOptionalLegacy(Tags.Items.INGOTS_GOLD);
      this.tagWithOptionalLegacy(Tags.Items.INGOTS_IRON);
      this.tagWithOptionalLegacy(Tags.Items.INGOTS_NETHERITE);
      this.tag(Tags.Items.LEATHERS).addOptionalTag(forge("leather"));
      this.tagWithOptionalLegacy(Tags.Items.MUSHROOMS);
      this.tagWithOptionalLegacy(Tags.Items.NETHER_STARS);
      this.tagWithOptionalLegacy(Tags.Items.NUGGETS);
      this.tagWithOptionalLegacy(Tags.Items.NUGGETS_IRON);
      this.tagWithOptionalLegacy(Tags.Items.NUGGETS_GOLD);
      this.tagWithOptionalLegacy(Tags.Items.RAW_MATERIALS);
      this.tagWithOptionalLegacy(Tags.Items.RAW_MATERIALS_COPPER);
      this.tagWithOptionalLegacy(Tags.Items.RAW_MATERIALS_GOLD);
      this.tagWithOptionalLegacy(Tags.Items.RAW_MATERIALS_IRON);
      this.tagWithOptionalLegacy(Tags.Items.RODS);
      this.tagWithOptionalLegacy(Tags.Items.RODS_BLAZE);
      this.tagWithOptionalLegacy(Tags.Items.RODS_WOODEN);
      this.tagWithOptionalLegacy(Tags.Items.SEEDS);
      this.tagWithOptionalLegacy(Tags.Items.SEEDS_BEETROOT);
      this.tagWithOptionalLegacy(Tags.Items.SEEDS_MELON);
      this.tagWithOptionalLegacy(Tags.Items.SEEDS_PUMPKIN);
      this.tagWithOptionalLegacy(Tags.Items.SEEDS_WHEAT);
      this.tagWithOptionalLegacy(Tags.Items.SLIME_BALLS);
      this.tagWithOptionalLegacy(Tags.Items.STRINGS);
      this.tag(Tags.Items.TOOLS_SHEAR).addOptionalTag(forge("shears"));
      this.tag(Tags.Items.TOOLS_SPEAR).addOptionalTag(forge("tools/tridents"));
      this.tagWithOptionalLegacy(Tags.Items.TOOLS);
      this.tagWithOptionalLegacy(Tags.Items.ARMORS);
      this.tag(Tags.Items.TOOLS_SHIELD).addOptionalTag(forge("tools/shields"));
      this.tag(Tags.Items.TOOLS_BOW).addOptionalTag(forge("tools/bows"));
      this.tag(Tags.Items.TOOLS_BRUSH).addOptionalTag(forge("tools/brushes"));
      this.tag(Tags.Items.TOOLS_CROSSBOW).addOptionalTag(forge("tools/crossbows"));
      this.tag(Tags.Items.TOOLS_FISHING_ROD).addOptionalTag(forge("tools/fishing_rods"));
      this.tag(Tags.Items.TOOLS_SHEAR).addOptionalTag(forge("tools/shears"));
      this.tag(Tags.Items.TOOLS_SPEAR).addOptionalTag(forge("tools/tridents"));
      this.tag(Tags.Items.TOOLS_SHIELD).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/shields")));
      this.tag(Tags.Items.TOOLS_BOW).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/bows")));
      this.tag(Tags.Items.TOOLS_BRUSH).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/brushes")));
      this.tag(Tags.Items.TOOLS_CROSSBOW).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/crossbows")));
      this.tag(Tags.Items.TOOLS_FISHING_ROD).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/fishing_rods")));
      this.tag(Tags.Items.TOOLS_SHEAR).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/shears")));
      this.tag(Tags.Items.TOOLS_SPEAR).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "tools/tridents")));
      this.tag(Tags.Items.FOODS_FRUIT).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/fruits")));
      this.tag(Tags.Items.FOODS_VEGETABLE).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/vegetables")));
      this.tag(Tags.Items.FOODS_BERRY).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/berries")));
      this.tag(Tags.Items.FOODS_BREAD).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/breads")));
      this.tag(Tags.Items.FOODS_COOKIE).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/cookies")));
      this.tag(Tags.Items.FOODS_DOUGH).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/doughs")));
      this.tag(Tags.Items.FOODS_RAW_MEAT).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/raw_meats")));
      this.tag(Tags.Items.FOODS_COOKED_MEAT).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/cooked_meats")));
      this.tag(Tags.Items.FOODS_RAW_FISH).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/raw_fishes")));
      this.tag(Tags.Items.FOODS_COOKED_FISH).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/cooked_fishes")));
      this.tag(Tags.Items.FOODS_SOUP).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/soups")));
      this.tag(Tags.Items.FOODS_CANDY).addOptionalTag(ItemTags.create(Identifier.fromNamespaceAndPath("c", "foods/candies")));
   }

   private TagAppender<Item, Item> tagWithOptionalLegacy(TagKey<Item> tag) {
      TagAppender<Item, Item> tagAppender = this.tag(tag);
      tagAppender.addOptionalTag(forge(tag.location().getPath()));
      return tagAppender;
   }

   private void tagColoredWithOptionalLegacy(TagKey<Item> group) {
      String prefix = group.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         TagKey<Item> tag = this.getForgeItemTag(prefix + color.getName());
         this.tagWithOptionalLegacy(tag);
      }
   }

   private void addColored(TagKey<Item> group, String pattern) {
      String prefix = group.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         Identifier key = Identifier.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName()));
         TagKey<Item> tag = this.getForgeItemTag(prefix + color.getName());
         Item item = BuiltInRegistries.ITEM.getValue(key);
         if (item == null || item == Items.AIR) {
            throw new IllegalStateException("Unknown vanilla item: " + key);
         }

         this.tag(tag).add(item);
      }
   }

   private void addColoredTags(Consumer<TagKey<Item>> consumer, TagKey<Item> group) {
      String prefix = group.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         TagKey<Item> tag = this.getForgeItemTag(prefix + color.getName());
         consumer.accept(tag);
      }
   }

   private TagKey<Block> getForgeBlockTag(String name) {
      try {
         name = name.toUpperCase(Locale.ENGLISH);
         return (TagKey<Block>)Tags.Blocks.class.getDeclaredField(name).get(null);
      } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException var3) {
         throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
      }
   }

   private TagKey<Item> getForgeItemTag(String name) {
      try {
         name = name.toUpperCase(Locale.ENGLISH);
         return (TagKey<Item>)Tags.Items.class.getDeclaredField(name).get(null);
      } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException var3) {
         throw new IllegalStateException(Tags.Items.class.getName() + " is missing tag name: " + name);
      }
   }

   private static TagKey<Item> forge(String name) {
      return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("forge", name));
   }

   @Override
   public String getName() {
      return "NeoForge Item Tags";
   }
}
