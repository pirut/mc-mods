package net.neoforged.neoforge.common.data.internal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

public final class NeoForgeBiomeTagsProvider extends BiomeTagsProvider {
   public NeoForgeBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, lookupProvider, "neoforge");
   }

   @Override
   protected void addTags(HolderLookup.Provider lookupProvider) {
      this.tag(Tags.Biomes.NO_DEFAULT_MONSTERS).add(Biomes.MUSHROOM_FIELDS, Biomes.DEEP_DARK);
      this.tag(Tags.Biomes.HIDDEN_FROM_LOCATOR_SELECTION);
      this.tag(Tags.Biomes.IS_VOID).add(Biomes.THE_VOID);
      this.tag(Tags.Biomes.IS_END).addTags(new TagKey[]{BiomeTags.IS_END});
      this.tag(Tags.Biomes.IS_NETHER).addTags(new TagKey[]{BiomeTags.IS_NETHER});
      this.tag(Tags.Biomes.IS_OVERWORLD).addTags(new TagKey[]{BiomeTags.IS_OVERWORLD});
      this.tag(Tags.Biomes.IS_HOT_OVERWORLD)
         .add(Biomes.JUNGLE)
         .add(Biomes.BAMBOO_JUNGLE)
         .add(Biomes.SPARSE_JUNGLE)
         .add(Biomes.DESERT)
         .add(Biomes.BADLANDS)
         .add(Biomes.WOODED_BADLANDS)
         .add(Biomes.ERODED_BADLANDS)
         .add(Biomes.SAVANNA)
         .add(Biomes.SAVANNA_PLATEAU)
         .add(Biomes.WINDSWEPT_SAVANNA)
         .add(Biomes.STONY_PEAKS)
         .add(Biomes.MUSHROOM_FIELDS)
         .add(Biomes.WARM_OCEAN);
      this.tag(Tags.Biomes.IS_HOT_NETHER)
         .add(Biomes.NETHER_WASTES)
         .add(Biomes.CRIMSON_FOREST)
         .add(Biomes.WARPED_FOREST)
         .add(Biomes.SOUL_SAND_VALLEY)
         .add(Biomes.BASALT_DELTAS);
      this.tag(Tags.Biomes.IS_HOT_END);
      this.tag(Tags.Biomes.IS_HOT).addTag(Tags.Biomes.IS_HOT_OVERWORLD).addTag(Tags.Biomes.IS_HOT_NETHER).addTag(Tags.Biomes.IS_HOT_END);
      this.tag(Tags.Biomes.IS_COLD_OVERWORLD)
         .add(Biomes.TAIGA)
         .add(Biomes.OLD_GROWTH_PINE_TAIGA)
         .add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
         .add(Biomes.WINDSWEPT_HILLS)
         .add(Biomes.WINDSWEPT_GRAVELLY_HILLS)
         .add(Biomes.WINDSWEPT_FOREST)
         .add(Biomes.SNOWY_PLAINS)
         .add(Biomes.ICE_SPIKES)
         .add(Biomes.GROVE)
         .add(Biomes.SNOWY_SLOPES)
         .add(Biomes.JAGGED_PEAKS)
         .add(Biomes.FROZEN_PEAKS)
         .add(Biomes.STONY_SHORE)
         .add(Biomes.SNOWY_BEACH)
         .add(Biomes.SNOWY_TAIGA)
         .add(Biomes.FROZEN_RIVER)
         .add(Biomes.COLD_OCEAN)
         .add(Biomes.FROZEN_OCEAN)
         .add(Biomes.DEEP_COLD_OCEAN)
         .add(Biomes.DEEP_FROZEN_OCEAN);
      this.tag(Tags.Biomes.IS_COLD_NETHER);
      this.tag(Tags.Biomes.IS_COLD_END)
         .add(Biomes.THE_END)
         .add(Biomes.SMALL_END_ISLANDS)
         .add(Biomes.END_MIDLANDS)
         .add(Biomes.END_HIGHLANDS)
         .add(Biomes.END_BARRENS);
      this.tag(Tags.Biomes.IS_COLD).addTag(Tags.Biomes.IS_COLD_OVERWORLD).addTag(Tags.Biomes.IS_COLD_NETHER).addTag(Tags.Biomes.IS_COLD_END);
      this.tag(Tags.Biomes.IS_TEMPERATE_OVERWORLD)
         .add(Biomes.PLAINS)
         .add(Biomes.SUNFLOWER_PLAINS)
         .add(Biomes.FOREST)
         .add(Biomes.FLOWER_FOREST)
         .add(Biomes.BIRCH_FOREST)
         .add(Biomes.OLD_GROWTH_BIRCH_FOREST)
         .add(Biomes.DARK_FOREST)
         .add(Biomes.CHERRY_GROVE)
         .add(Biomes.MEADOW)
         .add(Biomes.SWAMP)
         .add(Biomes.MANGROVE_SWAMP)
         .add(Biomes.BEACH)
         .add(Biomes.OCEAN)
         .add(Biomes.DEEP_OCEAN);
      this.tag(Tags.Biomes.IS_TEMPERATE_NETHER);
      this.tag(Tags.Biomes.IS_TEMPERATE_END);
      this.tag(Tags.Biomes.IS_TEMPERATE)
         .addTag(Tags.Biomes.IS_TEMPERATE_OVERWORLD)
         .addTag(Tags.Biomes.IS_TEMPERATE_NETHER)
         .addTag(Tags.Biomes.IS_TEMPERATE_END);
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD)
         .add(Biomes.WOODED_BADLANDS)
         .add(Biomes.SAVANNA)
         .add(Biomes.SAVANNA_PLATEAU)
         .add(Biomes.SPARSE_JUNGLE)
         .add(Biomes.WINDSWEPT_SAVANNA)
         .add(Biomes.WINDSWEPT_FOREST)
         .add(Biomes.WINDSWEPT_HILLS)
         .add(Biomes.WINDSWEPT_GRAVELLY_HILLS)
         .add(Biomes.SNOWY_SLOPES)
         .add(Biomes.JAGGED_PEAKS)
         .add(Biomes.FROZEN_PEAKS);
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION_NETHER);
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION_END);
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION)
         .addTag(Tags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD)
         .addTag(Tags.Biomes.IS_SPARSE_VEGETATION_NETHER)
         .addTag(Tags.Biomes.IS_SPARSE_VEGETATION_END);
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION_OVERWORLD)
         .add(Biomes.DARK_FOREST)
         .add(Biomes.OLD_GROWTH_BIRCH_FOREST)
         .add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
         .add(Biomes.JUNGLE)
         .add(Biomes.BAMBOO_JUNGLE)
         .add(Biomes.MANGROVE_SWAMP);
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION_NETHER);
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION_END);
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION)
         .addTag(Tags.Biomes.IS_DENSE_VEGETATION_OVERWORLD)
         .addTag(Tags.Biomes.IS_DENSE_VEGETATION_NETHER)
         .addTag(Tags.Biomes.IS_DENSE_VEGETATION_END);
      this.tag(Tags.Biomes.IS_WET_OVERWORLD)
         .add(Biomes.SWAMP)
         .add(Biomes.MANGROVE_SWAMP)
         .add(Biomes.JUNGLE)
         .add(Biomes.BAMBOO_JUNGLE)
         .add(Biomes.SPARSE_JUNGLE)
         .add(Biomes.BEACH)
         .add(Biomes.LUSH_CAVES)
         .add(Biomes.DRIPSTONE_CAVES);
      this.tag(Tags.Biomes.IS_WET_NETHER);
      this.tag(Tags.Biomes.IS_WET_END);
      this.tag(Tags.Biomes.IS_WET).addTag(Tags.Biomes.IS_WET_OVERWORLD).addTag(Tags.Biomes.IS_WET_NETHER).addTag(Tags.Biomes.IS_WET_END);
      this.tag(Tags.Biomes.IS_DRY_OVERWORLD)
         .add(Biomes.DESERT)
         .add(Biomes.BADLANDS)
         .add(Biomes.WOODED_BADLANDS)
         .add(Biomes.ERODED_BADLANDS)
         .add(Biomes.SAVANNA)
         .add(Biomes.SAVANNA_PLATEAU)
         .add(Biomes.WINDSWEPT_SAVANNA);
      this.tag(Tags.Biomes.IS_DRY_NETHER)
         .add(Biomes.NETHER_WASTES)
         .add(Biomes.CRIMSON_FOREST)
         .add(Biomes.WARPED_FOREST)
         .add(Biomes.SOUL_SAND_VALLEY)
         .add(Biomes.BASALT_DELTAS);
      this.tag(Tags.Biomes.IS_DRY_END)
         .add(Biomes.THE_END)
         .add(Biomes.SMALL_END_ISLANDS)
         .add(Biomes.END_MIDLANDS)
         .add(Biomes.END_HIGHLANDS)
         .add(Biomes.END_BARRENS);
      this.tag(Tags.Biomes.IS_DRY).addTag(Tags.Biomes.IS_DRY_OVERWORLD).addTag(Tags.Biomes.IS_DRY_NETHER).addTag(Tags.Biomes.IS_DRY_END);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_OAK).add(Biomes.FOREST).add(Biomes.FLOWER_FOREST).add(Biomes.SWAMP).add(Biomes.WOODED_BADLANDS);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_BIRCH).add(Biomes.BIRCH_FOREST).add(Biomes.OLD_GROWTH_BIRCH_FOREST);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_SPRUCE)
         .add(Biomes.TAIGA)
         .add(Biomes.SNOWY_TAIGA)
         .add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
         .add(Biomes.OLD_GROWTH_PINE_TAIGA)
         .add(Biomes.GROVE);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_JUNGLE).add(Biomes.JUNGLE).add(Biomes.SPARSE_JUNGLE);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_ACACIA).add(Biomes.SAVANNA).add(Biomes.SAVANNA_PLATEAU).add(Biomes.WINDSWEPT_SAVANNA);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_DARK_OAK).add(Biomes.DARK_FOREST);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_MANGROVE).add(Biomes.MANGROVE_SWAMP);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_CHERRY).add(Biomes.CHERRY_GROVE);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_PALE_OAK).add(Biomes.PALE_GARDEN);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_BAMBOO).add(Biomes.BAMBOO_JUNGLE);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_CRIMSON).add(Biomes.CRIMSON_FOREST);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE_WARPED).add(Biomes.WARPED_FOREST);
      this.tag(Tags.Biomes.PRIMARY_WOOD_TYPE)
         .addTags(
            new TagKey[]{
               Tags.Biomes.PRIMARY_WOOD_TYPE_OAK,
               Tags.Biomes.PRIMARY_WOOD_TYPE_BIRCH,
               Tags.Biomes.PRIMARY_WOOD_TYPE_SPRUCE,
               Tags.Biomes.PRIMARY_WOOD_TYPE_JUNGLE,
               Tags.Biomes.PRIMARY_WOOD_TYPE_ACACIA,
               Tags.Biomes.PRIMARY_WOOD_TYPE_DARK_OAK,
               Tags.Biomes.PRIMARY_WOOD_TYPE_MANGROVE,
               Tags.Biomes.PRIMARY_WOOD_TYPE_CHERRY,
               Tags.Biomes.PRIMARY_WOOD_TYPE_PALE_OAK,
               Tags.Biomes.PRIMARY_WOOD_TYPE_BAMBOO,
               Tags.Biomes.PRIMARY_WOOD_TYPE_CRIMSON,
               Tags.Biomes.PRIMARY_WOOD_TYPE_WARPED
            }
         );
      this.tag(Tags.Biomes.IS_CONIFEROUS_TREE).addTags(new TagKey[]{Tags.Biomes.IS_TAIGA}).add(Biomes.GROVE);
      this.tag(Tags.Biomes.IS_SAVANNA_TREE).addTags(new TagKey[]{Tags.Biomes.IS_SAVANNA});
      this.tag(Tags.Biomes.IS_JUNGLE_TREE).addTags(new TagKey[]{Tags.Biomes.IS_JUNGLE});
      this.tag(Tags.Biomes.IS_DECIDUOUS_TREE)
         .add(
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.PALE_GARDEN,
            Biomes.WINDSWEPT_FOREST
         );
      this.tag(Tags.Biomes.IS_MOUNTAIN_SLOPE).add(Biomes.SNOWY_SLOPES, Biomes.MEADOW, Biomes.GROVE, Biomes.CHERRY_GROVE);
      this.tag(Tags.Biomes.IS_MOUNTAIN_PEAK).add(Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS);
      this.tag(Tags.Biomes.IS_MOUNTAIN).addTag(BiomeTags.IS_MOUNTAIN).addTag(Tags.Biomes.IS_MOUNTAIN_PEAK).addTag(Tags.Biomes.IS_MOUNTAIN_SLOPE);
      this.tag(Tags.Biomes.IS_FOREST).addTags(new TagKey[]{BiomeTags.IS_FOREST});
      this.tag(Tags.Biomes.IS_BIRCH_FOREST).add(Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);
      this.tag(Tags.Biomes.IS_FLOWER_FOREST).add(Biomes.FLOWER_FOREST);
      this.tag(Tags.Biomes.IS_DARK_FOREST).add(Biomes.DARK_FOREST, Biomes.PALE_GARDEN);
      this.tag(Tags.Biomes.IS_FLORAL).addTags(new TagKey[]{Tags.Biomes.IS_FLOWER_FOREST}).add(Biomes.SUNFLOWER_PLAINS, Biomes.CHERRY_GROVE, Biomes.MEADOW);
      this.tag(Tags.Biomes.IS_BEACH).addTags(new TagKey[]{BiomeTags.IS_BEACH});
      this.tag(Tags.Biomes.IS_STONY_SHORES).add(Biomes.STONY_SHORE);
      this.tag(Tags.Biomes.IS_DESERT).add(Biomes.DESERT);
      this.tag(Tags.Biomes.IS_BADLANDS).addTags(new TagKey[]{BiomeTags.IS_BADLANDS});
      this.tag(Tags.Biomes.IS_PLAINS).add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS);
      this.tag(Tags.Biomes.IS_SNOWY_PLAINS).add(Biomes.SNOWY_PLAINS);
      this.tag(Tags.Biomes.IS_TAIGA).addTags(new TagKey[]{BiomeTags.IS_TAIGA});
      this.tag(Tags.Biomes.IS_HILL).addTags(new TagKey[]{BiomeTags.IS_HILL});
      this.tag(Tags.Biomes.IS_WINDSWEPT).add(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_SAVANNA);
      this.tag(Tags.Biomes.IS_SAVANNA).addTags(new TagKey[]{BiomeTags.IS_SAVANNA});
      this.tag(Tags.Biomes.IS_JUNGLE).addTags(new TagKey[]{BiomeTags.IS_JUNGLE});
      this.tag(Tags.Biomes.IS_SNOWY)
         .add(
            Biomes.SNOWY_BEACH,
            Biomes.SNOWY_PLAINS,
            Biomes.ICE_SPIKES,
            Biomes.SNOWY_TAIGA,
            Biomes.GROVE,
            Biomes.SNOWY_SLOPES,
            Biomes.JAGGED_PEAKS,
            Biomes.FROZEN_PEAKS
         );
      this.tag(Tags.Biomes.IS_ICY).add(Biomes.ICE_SPIKES, Biomes.FROZEN_PEAKS);
      this.tag(Tags.Biomes.IS_SWAMP).add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
      this.tag(Tags.Biomes.IS_OLD_GROWTH).add(Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
      this.tag(Tags.Biomes.IS_LUSH).add(Biomes.LUSH_CAVES);
      this.tag(Tags.Biomes.IS_SANDY).add(Biomes.DESERT, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS, Biomes.BEACH);
      this.tag(Tags.Biomes.IS_MUSHROOM).add(Biomes.MUSHROOM_FIELDS);
      this.tag(Tags.Biomes.IS_PLATEAU).add(Biomes.WOODED_BADLANDS, Biomes.SAVANNA_PLATEAU, Biomes.CHERRY_GROVE, Biomes.MEADOW);
      this.tag(Tags.Biomes.IS_SPOOKY).add(Biomes.DARK_FOREST, Biomes.PALE_GARDEN, Biomes.DEEP_DARK);
      this.tag(Tags.Biomes.IS_WASTELAND);
      this.tag(Tags.Biomes.IS_RARE)
         .add(
            Biomes.SUNFLOWER_PLAINS,
            Biomes.FLOWER_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.BAMBOO_JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.ERODED_BADLANDS,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA,
            Biomes.ICE_SPIKES,
            Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.PALE_GARDEN,
            Biomes.MUSHROOM_FIELDS,
            Biomes.DEEP_DARK
         );
      this.tag(Tags.Biomes.IS_RIVER).addTags(new TagKey[]{BiomeTags.IS_RIVER});
      this.tag(Tags.Biomes.IS_SHALLOW_OCEAN).add(Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.COLD_OCEAN, Biomes.FROZEN_OCEAN);
      this.tag(Tags.Biomes.IS_DEEP_OCEAN).addTags(new TagKey[]{BiomeTags.IS_DEEP_OCEAN});
      this.tag(Tags.Biomes.IS_OCEAN)
         .addTags(new TagKey[]{BiomeTags.IS_OCEAN})
         .addTags(new TagKey[]{Tags.Biomes.IS_SHALLOW_OCEAN})
         .addTags(new TagKey[]{Tags.Biomes.IS_DEEP_OCEAN});
      this.tag(Tags.Biomes.IS_AQUATIC_ICY).add(Biomes.FROZEN_RIVER, Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_OCEAN);
      this.tag(Tags.Biomes.IS_AQUATIC).addTag(Tags.Biomes.IS_OCEAN).addTag(Tags.Biomes.IS_RIVER);
      this.tag(Tags.Biomes.IS_CAVE).add(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK);
      this.tag(Tags.Biomes.IS_UNDERGROUND).addTag(Tags.Biomes.IS_CAVE);
      this.tag(Tags.Biomes.IS_NETHER_FOREST).add(Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST);
      this.tag(Tags.Biomes.IS_OUTER_END_ISLAND).add(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.END_BARRENS);
      this.tag(Tags.Biomes.IS_MOUNTAIN_SLOPE).addOptionalTag(this.forge("is_slope"));
      this.tag(Tags.Biomes.IS_MOUNTAIN_PEAK).addOptionalTag(this.forge("is_peak"));
      this.tagWithOptionalLegacy(Tags.Biomes.IS_MOUNTAIN);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_HOT_OVERWORLD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_HOT_NETHER);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_HOT_END);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_HOT);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_COLD_OVERWORLD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_COLD_NETHER);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_COLD_END);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_COLD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SPARSE_VEGETATION_NETHER);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SPARSE_VEGETATION_END);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SPARSE_VEGETATION);
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION_OVERWORLD).addOptionalTag(this.forge("is_sparse/overworld"));
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION_NETHER).addOptionalTag(this.forge("is_sparse/nether"));
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION_END).addOptionalTag(this.forge("is_sparse/end"));
      this.tag(Tags.Biomes.IS_SPARSE_VEGETATION).addOptionalTag(this.forge("is_sparse"));
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION_OVERWORLD).addOptionalTag(this.forge("is_dense/overworld"));
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION_NETHER).addOptionalTag(this.forge("is_dense/nether"));
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION_END).addOptionalTag(this.forge("is_dense/end"));
      this.tag(Tags.Biomes.IS_DENSE_VEGETATION).addOptionalTag(this.forge("is_dense"));
      this.tagWithOptionalLegacy(Tags.Biomes.IS_WET_OVERWORLD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_WET_NETHER);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_WET_END);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_WET);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_DRY_OVERWORLD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_DRY_NETHER);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_DRY_END);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_DRY);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_CONIFEROUS_TREE);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SPOOKY);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_DEAD);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_LUSH);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_MUSHROOM);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_MAGICAL);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_RARE);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_PLATEAU);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_FLORAL);
      this.tag(Tags.Biomes.IS_AQUATIC).addOptionalTag(this.forge("is_water"));
      this.tagWithOptionalLegacy(Tags.Biomes.IS_DESERT);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_PLAINS);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SWAMP);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SANDY);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_SNOWY);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_WASTELAND);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_VOID);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_CAVE);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_END);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_NETHER);
      this.tagWithOptionalLegacy(Tags.Biomes.IS_OVERWORLD);
   }

   @SafeVarargs
   private void tag(ResourceKey<Biome> biome, TagKey<Biome>... tags) {
      for (TagKey<Biome> key : tags) {
         this.tag(key).add(biome);
      }
   }

   private TagAppender<ResourceKey<Biome>, Biome> tagWithOptionalLegacy(TagKey<Biome> tag) {
      return this.tag(tag).addOptionalTag(this.forge(tag.location().getPath()));
   }

   private TagKey<Biome> forge(String id) {
      return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("forge", id));
   }

   @Override
   public String getName() {
      return "NeoForge Biome Tags";
   }
}
