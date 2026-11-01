package net.neoforged.neoforge.common.data.internal;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public final class NeoForgeBlockTagsProvider extends BlockTagsProvider {
   public NeoForgeBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, lookupProvider, "neoforge");
   }

   @Override
   public void addTags(HolderLookup.Provider p_256380_) {
      this.tag(Tags.Blocks.BARRELS).addTag(Tags.Blocks.BARRELS_WOODEN);
      this.tag(Tags.Blocks.BARRELS_WOODEN).add(Blocks.BARREL);
      this.tag(Tags.Blocks.BOOKSHELVES).add(Blocks.BOOKSHELF);
      this.tag(Tags.Blocks.BUDDING_BLOCKS).add(Blocks.BUDDING_AMETHYST);
      this.tag(Tags.Blocks.BUDS).add(Blocks.SMALL_AMETHYST_BUD).add(Blocks.MEDIUM_AMETHYST_BUD).add(Blocks.LARGE_AMETHYST_BUD);
      this.tag(Tags.Blocks.CHAINS).add(Blocks.IRON_CHAIN).addAll(Blocks.COPPER_CHAIN.asList());
      this.tag(Tags.Blocks.CHESTS)
         .add(Blocks.COPPER_CHEST)
         .addTags(new TagKey[]{Tags.Blocks.CHESTS_ENDER, Tags.Blocks.CHESTS_TRAPPED, Tags.Blocks.CHESTS_WOODEN});
      this.tag(Tags.Blocks.CHESTS_ENDER).add(Blocks.ENDER_CHEST);
      this.tag(Tags.Blocks.CHESTS_TRAPPED).add(Blocks.TRAPPED_CHEST);
      this.tag(Tags.Blocks.CHESTS_WOODEN).add(Blocks.CHEST, Blocks.TRAPPED_CHEST);
      this.tag(Tags.Blocks.CLUSTERS).add(Blocks.AMETHYST_CLUSTER);
      this.tag(Tags.Blocks.COBBLESTONES)
         .addTags(
            new TagKey[]{
               Tags.Blocks.COBBLESTONES_NORMAL, Tags.Blocks.COBBLESTONES_INFESTED, Tags.Blocks.COBBLESTONES_MOSSY, Tags.Blocks.COBBLESTONES_DEEPSLATE
            }
         );
      this.tag(Tags.Blocks.COBBLESTONES_NORMAL).add(Blocks.COBBLESTONE);
      this.tag(Tags.Blocks.COBBLESTONES_INFESTED).add(Blocks.INFESTED_COBBLESTONE);
      this.tag(Tags.Blocks.COBBLESTONES_MOSSY).add(Blocks.MOSSY_COBBLESTONE);
      this.tag(Tags.Blocks.COBBLESTONES_DEEPSLATE).add(Blocks.COBBLED_DEEPSLATE);
      this.tag(Tags.Blocks.CONCRETES)
         .add(
            Blocks.WHITE_CONCRETE,
            Blocks.ORANGE_CONCRETE,
            Blocks.MAGENTA_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE,
            Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE,
            Blocks.CYAN_CONCRETE,
            Blocks.PURPLE_CONCRETE,
            Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE,
            Blocks.GREEN_CONCRETE,
            Blocks.RED_CONCRETE,
            Blocks.BLACK_CONCRETE
         );
      this.addColored(Tags.Blocks.DYED, "{color}_banner");
      this.addColored(Tags.Blocks.DYED, "{color}_bed");
      this.addColored(Tags.Blocks.DYED, "{color}_candle");
      this.addColored(Tags.Blocks.DYED, "{color}_carpet");
      this.addColored(Tags.Blocks.DYED, "{color}_concrete");
      this.addColored(Tags.Blocks.DYED, "{color}_concrete_powder");
      this.addColored(Tags.Blocks.DYED, "{color}_glazed_terracotta");
      this.addColored(Tags.Blocks.DYED, "{color}_shulker_box");
      this.addColored(Tags.Blocks.DYED, "{color}_stained_glass");
      this.addColored(Tags.Blocks.DYED, "{color}_stained_glass_pane");
      this.addColored(Tags.Blocks.DYED, "{color}_terracotta");
      this.addColored(Tags.Blocks.DYED, "{color}_wall_banner");
      this.addColored(Tags.Blocks.DYED, "{color}_wool");
      this.addColoredTags(this.tag(Tags.Blocks.DYED)::addTag, Tags.Blocks.DYED);
      this.tag(Tags.Blocks.END_STONES).add(Blocks.END_STONE);
      this.tag(Tags.Blocks.ENDERMAN_PLACE_ON_BLACKLIST);
      this.tag(Tags.Blocks.FENCE_GATES).addTags(new TagKey[]{Tags.Blocks.FENCE_GATES_WOODEN});
      this.tag(Tags.Blocks.FENCE_GATES_WOODEN)
         .add(
            Blocks.OAK_FENCE_GATE,
            Blocks.SPRUCE_FENCE_GATE,
            Blocks.BIRCH_FENCE_GATE,
            Blocks.JUNGLE_FENCE_GATE,
            Blocks.ACACIA_FENCE_GATE,
            Blocks.DARK_OAK_FENCE_GATE,
            Blocks.CRIMSON_FENCE_GATE,
            Blocks.WARPED_FENCE_GATE,
            Blocks.MANGROVE_FENCE_GATE,
            Blocks.BAMBOO_FENCE_GATE,
            Blocks.CHERRY_FENCE_GATE
         );
      this.tag(Tags.Blocks.FENCES).addTags(new TagKey[]{Tags.Blocks.FENCES_NETHER_BRICK, Tags.Blocks.FENCES_WOODEN});
      this.tag(Tags.Blocks.FENCES_NETHER_BRICK).add(Blocks.NETHER_BRICK_FENCE);
      this.tag(Tags.Blocks.FENCES_WOODEN).addTag(BlockTags.WOODEN_FENCES);
      this.tag(Tags.Blocks.FLOWERS_SMALL)
         .add(
            Blocks.DANDELION,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER,
            Blocks.LILY_OF_THE_VALLEY,
            Blocks.WITHER_ROSE,
            Blocks.TORCHFLOWER,
            Blocks.OPEN_EYEBLOSSOM,
            Blocks.CLOSED_EYEBLOSSOM
         )
         .addOptionalTag(BlockTags.SMALL_FLOWERS);
      this.tag(Tags.Blocks.FLOWERS_TALL)
         .add(Blocks.SUNFLOWER, Blocks.LILAC, Blocks.PEONY, Blocks.ROSE_BUSH, Blocks.PITCHER_PLANT)
         .addOptionalTag(BlockTags.create(Identifier.withDefaultNamespace("tall_flowers")));
      this.tag(Tags.Blocks.FLOWERS)
         .add(
            Blocks.CHERRY_LEAVES,
            Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.FLOWERING_AZALEA,
            Blocks.MANGROVE_PROPAGULE,
            Blocks.PINK_PETALS,
            Blocks.WILDFLOWERS,
            Blocks.CHORUS_FLOWER,
            Blocks.SPORE_BLOSSOM,
            Blocks.CACTUS_FLOWER
         )
         .addTags(new TagKey[]{Tags.Blocks.FLOWERS_SMALL, Tags.Blocks.FLOWERS_TALL})
         .addOptionalTag(BlockTags.FLOWERS);
      this.tag(Tags.Blocks.GLASS_BLOCKS)
         .addTags(new TagKey[]{Tags.Blocks.GLASS_BLOCKS_COLORLESS, Tags.Blocks.GLASS_BLOCKS_CHEAP, Tags.Blocks.GLASS_BLOCKS_TINTED});
      this.tag(Tags.Blocks.GLASS_BLOCKS_COLORLESS).add(Blocks.GLASS);
      this.tag(Tags.Blocks.GLASS_BLOCKS_CHEAP)
         .add(
            Blocks.GLASS,
            Blocks.WHITE_STAINED_GLASS,
            Blocks.ORANGE_STAINED_GLASS,
            Blocks.MAGENTA_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.YELLOW_STAINED_GLASS,
            Blocks.LIME_STAINED_GLASS,
            Blocks.PINK_STAINED_GLASS,
            Blocks.GRAY_STAINED_GLASS,
            Blocks.LIGHT_GRAY_STAINED_GLASS,
            Blocks.CYAN_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS,
            Blocks.BLUE_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS,
            Blocks.GREEN_STAINED_GLASS,
            Blocks.RED_STAINED_GLASS,
            Blocks.BLACK_STAINED_GLASS
         );
      this.tag(Tags.Blocks.GLASS_BLOCKS_TINTED).add(Blocks.TINTED_GLASS);
      this.tag(Tags.Blocks.GLASS_PANES)
         .addTags(new TagKey[]{Tags.Blocks.GLASS_PANES_COLORLESS})
         .add(
            Blocks.WHITE_STAINED_GLASS_PANE,
            Blocks.ORANGE_STAINED_GLASS_PANE,
            Blocks.MAGENTA_STAINED_GLASS_PANE,
            Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
            Blocks.YELLOW_STAINED_GLASS_PANE,
            Blocks.LIME_STAINED_GLASS_PANE,
            Blocks.PINK_STAINED_GLASS_PANE,
            Blocks.GRAY_STAINED_GLASS_PANE,
            Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
            Blocks.CYAN_STAINED_GLASS_PANE,
            Blocks.PURPLE_STAINED_GLASS_PANE,
            Blocks.BLUE_STAINED_GLASS_PANE,
            Blocks.BROWN_STAINED_GLASS_PANE,
            Blocks.GREEN_STAINED_GLASS_PANE,
            Blocks.RED_STAINED_GLASS_PANE,
            Blocks.BLACK_STAINED_GLASS_PANE
         );
      this.tag(Tags.Blocks.GLASS_PANES_COLORLESS).add(Blocks.GLASS_PANE);
      this.tag(Tags.Blocks.GLAZED_TERRACOTTAS)
         .add(
            Blocks.WHITE_GLAZED_TERRACOTTA,
            Blocks.ORANGE_GLAZED_TERRACOTTA,
            Blocks.MAGENTA_GLAZED_TERRACOTTA,
            Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
            Blocks.YELLOW_GLAZED_TERRACOTTA,
            Blocks.LIME_GLAZED_TERRACOTTA,
            Blocks.PINK_GLAZED_TERRACOTTA,
            Blocks.GRAY_GLAZED_TERRACOTTA,
            Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA,
            Blocks.CYAN_GLAZED_TERRACOTTA,
            Blocks.PURPLE_GLAZED_TERRACOTTA,
            Blocks.BLUE_GLAZED_TERRACOTTA,
            Blocks.BROWN_GLAZED_TERRACOTTA,
            Blocks.GREEN_GLAZED_TERRACOTTA,
            Blocks.RED_GLAZED_TERRACOTTA,
            Blocks.BLACK_GLAZED_TERRACOTTA
         );
      this.tag(Tags.Blocks.GRAVELS).add(Blocks.GRAVEL);
      this.tag(Tags.Blocks.SKULLS)
         .add(
            Blocks.SKELETON_SKULL,
            Blocks.SKELETON_WALL_SKULL,
            Blocks.WITHER_SKELETON_SKULL,
            Blocks.WITHER_SKELETON_WALL_SKULL,
            Blocks.PLAYER_HEAD,
            Blocks.PLAYER_WALL_HEAD,
            Blocks.ZOMBIE_HEAD,
            Blocks.ZOMBIE_WALL_HEAD,
            Blocks.CREEPER_HEAD,
            Blocks.CREEPER_WALL_HEAD,
            Blocks.PIGLIN_HEAD,
            Blocks.PIGLIN_WALL_HEAD,
            Blocks.DRAGON_HEAD,
            Blocks.DRAGON_WALL_HEAD
         );
      this.tag(Tags.Blocks.HIDDEN_FROM_RECIPE_VIEWERS);
      this.tag(Tags.Blocks.NETHERRACKS).add(Blocks.NETHERRACK);
      this.tag(Tags.Blocks.OBSIDIANS_NORMAL).add(Blocks.OBSIDIAN);
      this.tag(Tags.Blocks.OBSIDIANS_CRYING).add(Blocks.CRYING_OBSIDIAN);
      this.tag(Tags.Blocks.OBSIDIANS).addTags(new TagKey[]{Tags.Blocks.OBSIDIANS_NORMAL, Tags.Blocks.OBSIDIANS_CRYING});
      this.tag(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE).add(Blocks.DEEPSLATE);
      this.tag(Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK).add(Blocks.NETHERRACK);
      this.tag(Tags.Blocks.ORE_BEARING_GROUND_STONE).add(Blocks.STONE);
      this.tag(Tags.Blocks.ORE_RATES_DENSE)
         .add(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE);
      this.tag(Tags.Blocks.ORE_RATES_SINGULAR)
         .add(
            Blocks.ANCIENT_DEBRIS,
            Blocks.COAL_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.NETHER_QUARTZ_ORE
         );
      this.tag(Tags.Blocks.ORE_RATES_SPARSE).add(Blocks.NETHER_GOLD_ORE);
      this.tag(Tags.Blocks.ORES)
         .addTags(
            new TagKey[]{
               Tags.Blocks.ORES_COAL,
               Tags.Blocks.ORES_COPPER,
               Tags.Blocks.ORES_DIAMOND,
               Tags.Blocks.ORES_EMERALD,
               Tags.Blocks.ORES_GOLD,
               Tags.Blocks.ORES_IRON,
               Tags.Blocks.ORES_LAPIS,
               Tags.Blocks.ORES_NETHERITE_SCRAP,
               Tags.Blocks.ORES_REDSTONE,
               Tags.Blocks.ORES_QUARTZ
            }
         );
      this.tag(Tags.Blocks.ORES_COAL).addTag(BlockTags.COAL_ORES);
      this.tag(Tags.Blocks.ORES_COPPER).addTag(BlockTags.COPPER_ORES);
      this.tag(Tags.Blocks.ORES_DIAMOND).addTag(BlockTags.DIAMOND_ORES);
      this.tag(Tags.Blocks.ORES_EMERALD).addTag(BlockTags.EMERALD_ORES);
      this.tag(Tags.Blocks.ORES_GOLD).addTag(BlockTags.GOLD_ORES);
      this.tag(Tags.Blocks.ORES_IRON).addTag(BlockTags.IRON_ORES);
      this.tag(Tags.Blocks.ORES_LAPIS).addTag(BlockTags.LAPIS_ORES);
      this.tag(Tags.Blocks.ORES_QUARTZ).add(Blocks.NETHER_QUARTZ_ORE);
      this.tag(Tags.Blocks.ORES_REDSTONE).addTag(BlockTags.REDSTONE_ORES);
      this.tag(Tags.Blocks.ORES_NETHERITE_SCRAP).add(Blocks.ANCIENT_DEBRIS);
      this.tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
         .add(
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE
         );
      this.tag(Tags.Blocks.ORES_IN_GROUND_NETHERRACK).add(Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE);
      this.tag(Tags.Blocks.ORES_IN_GROUND_STONE)
         .add(
            Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE
         );
      this.tag(Tags.Blocks.PUMPKINS).addTags(new TagKey[]{Tags.Blocks.PUMPKINS_NORMAL, Tags.Blocks.PUMPKINS_CARVED, Tags.Blocks.PUMPKINS_JACK_O_LANTERNS});
      this.tag(Tags.Blocks.PUMPKINS_NORMAL).add(Blocks.PUMPKIN);
      this.tag(Tags.Blocks.PUMPKINS_CARVED).add(Blocks.CARVED_PUMPKIN);
      this.tag(Tags.Blocks.PUMPKINS_JACK_O_LANTERNS).add(Blocks.JACK_O_LANTERN);
      this.tag(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES).add(Blocks.CRAFTING_TABLE);
      this.tag(Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES).add(Blocks.FURNACE);
      this.tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED);
      this.tag(Tags.Blocks.ROPES);
      this.tag(Tags.Blocks.SANDS).addTags(new TagKey[]{Tags.Blocks.SANDS_COLORLESS, Tags.Blocks.SANDS_RED});
      this.tag(Tags.Blocks.SANDS_COLORLESS).add(Blocks.SAND);
      this.tag(Tags.Blocks.SANDS_RED).add(Blocks.RED_SAND);
      this.tag(Tags.Blocks.SANDSTONE_RED_BLOCKS)
         .add(Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE);
      this.tag(Tags.Blocks.SANDSTONE_UNCOLORED_BLOCKS).add(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
      this.tag(Tags.Blocks.SANDSTONE_BLOCKS).addTags(new TagKey[]{Tags.Blocks.SANDSTONE_RED_BLOCKS, Tags.Blocks.SANDSTONE_UNCOLORED_BLOCKS});
      this.tag(Tags.Blocks.SANDSTONE_RED_SLABS).add(Blocks.RED_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_SLAB);
      this.tag(Tags.Blocks.SANDSTONE_UNCOLORED_SLABS).add(Blocks.SANDSTONE_SLAB, Blocks.CUT_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB);
      this.tag(Tags.Blocks.SANDSTONE_SLABS).addTags(new TagKey[]{Tags.Blocks.SANDSTONE_RED_SLABS, Tags.Blocks.SANDSTONE_UNCOLORED_SLABS});
      this.tag(Tags.Blocks.SANDSTONE_RED_STAIRS).add(Blocks.RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE_STAIRS);
      this.tag(Tags.Blocks.SANDSTONE_UNCOLORED_STAIRS).add(Blocks.SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_STAIRS);
      this.tag(Tags.Blocks.SANDSTONE_STAIRS).addTags(new TagKey[]{Tags.Blocks.SANDSTONE_RED_STAIRS, Tags.Blocks.SANDSTONE_UNCOLORED_STAIRS});
      this.tag(Tags.Blocks.STONES).add(Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.STONE, Blocks.DEEPSLATE, Blocks.TUFF);
      this.tag(Tags.Blocks.STORAGE_BLOCKS)
         .addTags(
            new TagKey[]{
               Tags.Blocks.STORAGE_BLOCKS_BONE_MEAL,
               Tags.Blocks.STORAGE_BLOCKS_COAL,
               Tags.Blocks.STORAGE_BLOCKS_COPPER,
               Tags.Blocks.STORAGE_BLOCKS_DIAMOND,
               Tags.Blocks.STORAGE_BLOCKS_DRIED_KELP,
               Tags.Blocks.STORAGE_BLOCKS_EMERALD,
               Tags.Blocks.STORAGE_BLOCKS_GOLD,
               Tags.Blocks.STORAGE_BLOCKS_IRON,
               Tags.Blocks.STORAGE_BLOCKS_LAPIS,
               Tags.Blocks.STORAGE_BLOCKS_NETHERITE,
               Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER,
               Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD,
               Tags.Blocks.STORAGE_BLOCKS_RAW_IRON,
               Tags.Blocks.STORAGE_BLOCKS_REDSTONE,
               Tags.Blocks.STORAGE_BLOCKS_RESIN,
               Tags.Blocks.STORAGE_BLOCKS_SLIME,
               Tags.Blocks.STORAGE_BLOCKS_WHEAT
            }
         );
      this.tag(Tags.Blocks.STORAGE_BLOCKS_BONE_MEAL).add(Blocks.BONE_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_COAL).add(Blocks.COAL_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_COPPER).add(Blocks.COPPER_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_DIAMOND).add(Blocks.DIAMOND_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_DRIED_KELP).add(Blocks.DRIED_KELP_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_EMERALD).add(Blocks.EMERALD_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_GOLD).add(Blocks.GOLD_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_IRON).add(Blocks.IRON_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_LAPIS).add(Blocks.LAPIS_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_NETHERITE).add(Blocks.NETHERITE_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER).add(Blocks.RAW_COPPER_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD).add(Blocks.RAW_GOLD_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON).add(Blocks.RAW_IRON_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_REDSTONE).add(Blocks.REDSTONE_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_RESIN).add(Blocks.RESIN_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_SLIME).add(Blocks.SLIME_BLOCK);
      this.tag(Tags.Blocks.STORAGE_BLOCKS_WHEAT).add(Blocks.HAY_BLOCK);
      this.tag(Tags.Blocks.OVERWORLD_NATURAL_LOGS).addOptionalTags(new TagKey[]{BlockTags.OVERWORLD_NATURAL_LOGS});
      this.tag(Tags.Blocks.NETHER_NATURAL_LOGS).add(Blocks.CRIMSON_STEM, Blocks.WARPED_STEM);
      this.tag(Tags.Blocks.NATURAL_LOGS).addOptionalTags(new TagKey[]{Tags.Blocks.OVERWORLD_NATURAL_LOGS, Tags.Blocks.NETHER_NATURAL_LOGS});
      this.tag(Tags.Blocks.NATURAL_WOODS)
         .add(
            Blocks.ACACIA_WOOD,
            Blocks.BIRCH_WOOD,
            Blocks.CHERRY_WOOD,
            Blocks.DARK_OAK_WOOD,
            Blocks.JUNGLE_WOOD,
            Blocks.MANGROVE_WOOD,
            Blocks.OAK_WOOD,
            Blocks.PALE_OAK_WOOD,
            Blocks.SPRUCE_WOOD,
            Blocks.CRIMSON_HYPHAE,
            Blocks.WARPED_HYPHAE
         );
      this.tag(Tags.Blocks.STRIPPED_LOGS)
         .add(
            Blocks.STRIPPED_ACACIA_LOG,
            Blocks.STRIPPED_BAMBOO_BLOCK,
            Blocks.STRIPPED_BIRCH_LOG,
            Blocks.STRIPPED_CHERRY_LOG,
            Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.STRIPPED_JUNGLE_LOG,
            Blocks.STRIPPED_MANGROVE_LOG,
            Blocks.STRIPPED_OAK_LOG,
            Blocks.STRIPPED_PALE_OAK_LOG,
            Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.STRIPPED_CRIMSON_STEM,
            Blocks.STRIPPED_WARPED_STEM
         );
      this.tag(Tags.Blocks.STRIPPED_WOODS)
         .add(
            Blocks.STRIPPED_ACACIA_WOOD,
            Blocks.STRIPPED_BIRCH_WOOD,
            Blocks.STRIPPED_CHERRY_WOOD,
            Blocks.STRIPPED_DARK_OAK_WOOD,
            Blocks.STRIPPED_JUNGLE_WOOD,
            Blocks.STRIPPED_MANGROVE_WOOD,
            Blocks.STRIPPED_OAK_WOOD,
            Blocks.STRIPPED_PALE_OAK_WOOD,
            Blocks.STRIPPED_SPRUCE_WOOD,
            Blocks.STRIPPED_CRIMSON_HYPHAE,
            Blocks.STRIPPED_WARPED_HYPHAE
         );
      this.tag(Tags.Blocks.VILLAGER_JOB_SITES)
         .add(
            Blocks.BARREL,
            Blocks.BLAST_FURNACE,
            Blocks.BREWING_STAND,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.CAULDRON,
            Blocks.WATER_CAULDRON,
            Blocks.LAVA_CAULDRON,
            Blocks.POWDER_SNOW_CAULDRON,
            Blocks.COMPOSTER,
            Blocks.FLETCHING_TABLE,
            Blocks.GRINDSTONE,
            Blocks.LECTERN,
            Blocks.LOOM,
            Blocks.SMITHING_TABLE,
            Blocks.SMOKER,
            Blocks.STONECUTTER
         );
      this.tag(Tags.Blocks.VILLAGER_FARMLANDS).add(Blocks.FARMLAND);
      this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL).addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tag(BlockTags.INCORRECT_FOR_STONE_TOOL).addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tag(BlockTags.INCORRECT_FOR_COPPER_TOOL).addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tag(BlockTags.INCORRECT_FOR_IRON_TOOL).addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tag(BlockTags.INCORRECT_FOR_GOLD_TOOL).addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL).addTag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
      this.tagWithOptionalLegacy(Tags.Blocks.BARRELS);
      this.tagWithOptionalLegacy(Tags.Blocks.BARRELS_WOODEN);
      this.tagWithOptionalLegacy(Tags.Blocks.BOOKSHELVES);
      this.tagWithOptionalLegacy(Tags.Blocks.CHESTS);
      this.tagWithOptionalLegacy(Tags.Blocks.CHESTS_ENDER);
      this.tagWithOptionalLegacy(Tags.Blocks.CHESTS_TRAPPED);
      this.tagWithOptionalLegacy(Tags.Blocks.CHESTS_WOODEN);
      this.tag(Tags.Blocks.COBBLESTONES).addOptionalTag(this.forge("cobblestone"));
      this.tag(Tags.Blocks.COBBLESTONES_NORMAL).addOptionalTag(this.forge("cobblestone/normal"));
      this.tag(Tags.Blocks.COBBLESTONES_INFESTED).addOptionalTag(this.forge("cobblestone/infested"));
      this.tag(Tags.Blocks.COBBLESTONES_MOSSY).addOptionalTag(this.forge("cobblestone/mossy"));
      this.tag(Tags.Blocks.COBBLESTONES_DEEPSLATE).addOptionalTag(this.forge("cobblestone/deepslate"));
      this.tag(Tags.Blocks.DYED_BLACK).addOptionalTag(this.forge("glass/black")).addOptionalTag(this.forge("stained_glass/black"));
      this.tag(Tags.Blocks.DYED_BLUE).addOptionalTag(this.forge("glass/blue")).addOptionalTag(this.forge("stained_glass/blue"));
      this.tag(Tags.Blocks.DYED_BROWN).addOptionalTag(this.forge("glass/brown")).addOptionalTag(this.forge("stained_glass/brown"));
      this.tag(Tags.Blocks.DYED_CYAN).addOptionalTag(this.forge("glass/cyan")).addOptionalTag(this.forge("stained_glass/cyan"));
      this.tag(Tags.Blocks.DYED_GRAY).addOptionalTag(this.forge("glass/gray")).addOptionalTag(this.forge("stained_glass/gray"));
      this.tag(Tags.Blocks.DYED_GREEN).addOptionalTag(this.forge("glass/green")).addOptionalTag(this.forge("stained_glass/green"));
      this.tag(Tags.Blocks.DYED_LIGHT_BLUE).addOptionalTag(this.forge("glass/light_blue")).addOptionalTag(this.forge("stained_glass/light_blue"));
      this.tag(Tags.Blocks.DYED_LIGHT_GRAY).addOptionalTag(this.forge("glass/light_gray")).addOptionalTag(this.forge("stained_glass/light_gray"));
      this.tag(Tags.Blocks.DYED_LIME).addOptionalTag(this.forge("glass/lime")).addOptionalTag(this.forge("stained_glass/lime"));
      this.tag(Tags.Blocks.DYED_MAGENTA).addOptionalTag(this.forge("glass/magenta")).addOptionalTag(this.forge("stained_glass/magenta"));
      this.tag(Tags.Blocks.DYED_MAGENTA).addOptionalTag(this.forge("glass/magenta")).addOptionalTag(this.forge("stained_glass/magenta"));
      this.tag(Tags.Blocks.DYED_ORANGE).addOptionalTag(this.forge("glass/orange")).addOptionalTag(this.forge("stained_glass/orange"));
      this.tag(Tags.Blocks.DYED_PINK).addOptionalTag(this.forge("glass/pink")).addOptionalTag(this.forge("stained_glass/pink"));
      this.tag(Tags.Blocks.DYED_PURPLE).addOptionalTag(this.forge("glass/purple")).addOptionalTag(this.forge("stained_glass/purple"));
      this.tag(Tags.Blocks.DYED_RED).addOptionalTag(this.forge("glass/red")).addOptionalTag(this.forge("stained_glass/red"));
      this.tag(Tags.Blocks.DYED_WHITE).addOptionalTag(this.forge("glass/white")).addOptionalTag(this.forge("stained_glass/white"));
      this.tag(Tags.Blocks.DYED_YELLOW).addOptionalTag(this.forge("glass/yellow")).addOptionalTag(this.forge("stained_glass/yellow"));
      this.tagWithOptionalLegacy(Tags.Blocks.END_STONES);
      this.tagWithOptionalLegacy(Tags.Blocks.ENDERMAN_PLACE_ON_BLACKLIST);
      this.tagWithOptionalLegacy(Tags.Blocks.FENCE_GATES);
      this.tagWithOptionalLegacy(Tags.Blocks.FENCE_GATES_WOODEN);
      this.tagWithOptionalLegacy(Tags.Blocks.FENCES);
      this.tagWithOptionalLegacy(Tags.Blocks.FENCES_NETHER_BRICK);
      this.tagWithOptionalLegacy(Tags.Blocks.FENCES_WOODEN);
      this.tag(Tags.Blocks.GRAVELS).addOptionalTag(this.forge("gravel"));
      this.tag(Tags.Blocks.GLASS_BLOCKS).addOptionalTag(this.forge("glass"));
      this.tag(Tags.Blocks.GLASS_BLOCKS_COLORLESS).addOptionalTag(this.forge("glass_colorless"));
      this.tag(Tags.Blocks.GLASS_BLOCKS_CHEAP).addOptionalTag(this.forge("glass_silica"));
      this.tag(Tags.Blocks.GLASS_BLOCKS_TINTED).addOptionalTag(this.forge("glass_tinted"));
      this.tag(Tags.Blocks.GLASS_PANES_COLORLESS).addOptionalTag(this.forge("glass_panes_colorless"));
      this.tag(Tags.Blocks.NETHERRACKS).addOptionalTag(this.forge("netherrack"));
      this.tag(Tags.Blocks.OBSIDIANS).addOptionalTag(this.forge("obsidian"));
      this.tagWithOptionalLegacy(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE);
      this.tagWithOptionalLegacy(Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK);
      this.tagWithOptionalLegacy(Tags.Blocks.ORE_BEARING_GROUND_STONE);
      this.tagWithOptionalLegacy(Tags.Blocks.ORE_RATES_DENSE);
      this.tagWithOptionalLegacy(Tags.Blocks.ORE_RATES_SINGULAR);
      this.tagWithOptionalLegacy(Tags.Blocks.ORE_RATES_SPARSE);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_COAL);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_COPPER);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_DIAMOND);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_EMERALD);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_GOLD);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_IRON);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_LAPIS);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_QUARTZ);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_REDSTONE);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_NETHERITE_SCRAP);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_IN_GROUND_NETHERRACK);
      this.tagWithOptionalLegacy(Tags.Blocks.ORES_IN_GROUND_STONE);
      this.tag(Tags.Blocks.STONES).addOptionalTag(this.forge("stone"));
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_COAL);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_COPPER);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_DIAMOND);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_EMERALD);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_GOLD);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_IRON);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_LAPIS);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_REDSTONE);
      this.tagWithOptionalLegacy(Tags.Blocks.STORAGE_BLOCKS_NETHERITE);
      this.tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED).addOptionalTag(this.forge("relocation_not_supported")).addOptionalTag(this.forge("immovable"));
      this.tag(Tags.Blocks.SANDSTONE_BLOCKS).addOptionalTag(this.forge("sandstone"));
      this.tag(Tags.Blocks.SANDS).addOptionalTag(this.forge("sand"));
      this.tag(Tags.Blocks.SANDS_COLORLESS).addOptionalTag(this.forge("sand/colorless"));
      this.tag(Tags.Blocks.SANDS_RED).addOptionalTag(this.forge("sand/red"));
   }

   private TagAppender<Block, Block> tagWithOptionalLegacy(TagKey<Block> tag) {
      TagAppender<Block, Block> tagAppender = this.tag(tag);
      tagAppender.addOptionalTag(this.forge(tag.location().getPath()));
      return tagAppender;
   }

   private void addColored(TagKey<Block> group, String pattern) {
      String prefix = group.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         Identifier key = Identifier.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName()));
         TagKey<Block> tag = this.getForgeTag(prefix + color.getName());
         Block block = BuiltInRegistries.BLOCK.getValue(key);
         if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("Unknown vanilla block: " + key);
         }

         this.tag(tag).add(block);
      }
   }

   private void addColoredTags(Consumer<TagKey<Block>> consumer, TagKey<Block> group) {
      String prefix = group.location().getPath().toUpperCase(Locale.ENGLISH) + "_";

      for (DyeColor color : DyeColor.values()) {
         TagKey<Block> tag = this.getForgeTag(prefix + color.getName());
         consumer.accept(tag);
      }
   }

   private TagKey<Block> getForgeTag(String name) {
      try {
         name = name.toUpperCase(Locale.ENGLISH);
         return (TagKey<Block>)Tags.Blocks.class.getDeclaredField(name).get(null);
      } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException var3) {
         throw new IllegalStateException(Tags.Blocks.class.getName() + " is missing tag name: " + name);
      }
   }

   private TagKey<Block> forge(String id) {
      return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("forge", id));
   }
}
