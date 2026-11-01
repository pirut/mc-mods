package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.AquaticFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CarvingMaskPlacement;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class AquaticPlacements {
   public static final ResourceKey<PlacedFeature> SEAGRASS_WARM = PlacementUtils.createKey("seagrass_warm");
   public static final ResourceKey<PlacedFeature> SEAGRASS_NORMAL = PlacementUtils.createKey("seagrass_normal");
   public static final ResourceKey<PlacedFeature> SEAGRASS_COLD = PlacementUtils.createKey("seagrass_cold");
   public static final ResourceKey<PlacedFeature> SEAGRASS_RIVER = PlacementUtils.createKey("seagrass_river");
   public static final ResourceKey<PlacedFeature> SEAGRASS_SWAMP = PlacementUtils.createKey("seagrass_swamp");
   public static final ResourceKey<PlacedFeature> SEAGRASS_DEEP_WARM = PlacementUtils.createKey("seagrass_deep_warm");
   public static final ResourceKey<PlacedFeature> SEAGRASS_DEEP = PlacementUtils.createKey("seagrass_deep");
   public static final ResourceKey<PlacedFeature> SEAGRASS_DEEP_COLD = PlacementUtils.createKey("seagrass_deep_cold");
   public static final ResourceKey<PlacedFeature> SEAGRASS_SIMPLE = PlacementUtils.createKey("seagrass_simple");
   public static final ResourceKey<PlacedFeature> SEA_PICKLE = PlacementUtils.createKey("sea_pickle");
   public static final ResourceKey<PlacedFeature> KELP_COLD = PlacementUtils.createKey("kelp_cold");
   public static final ResourceKey<PlacedFeature> KELP_WARM = PlacementUtils.createKey("kelp_warm");
   public static final ResourceKey<PlacedFeature> WARM_OCEAN_VEGETATION = PlacementUtils.createKey("warm_ocean_vegetation");

   private static List<PlacementModifier> seagrassPlacement(int p_195234_) {
      return List.of(InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, CountPlacement.of(p_195234_), BiomeFilter.biome());
   }

   public static void bootstrap(BootstrapContext<PlacedFeature> p_321710_) {
      HolderGetter<ConfiguredFeature<?, ?>> $$1 = p_321710_.lookup(Registries.CONFIGURED_FEATURE);
      Holder.Reference<ConfiguredFeature<?, ?>> $$2 = $$1.getOrThrow(AquaticFeatures.SEAGRASS_SHORT);
      Holder.Reference<ConfiguredFeature<?, ?>> $$3 = $$1.getOrThrow(AquaticFeatures.SEAGRASS_SLIGHTLY_LESS_SHORT);
      Holder.Reference<ConfiguredFeature<?, ?>> $$4 = $$1.getOrThrow(AquaticFeatures.SEAGRASS_MID);
      Holder.Reference<ConfiguredFeature<?, ?>> $$5 = $$1.getOrThrow(AquaticFeatures.SEAGRASS_TALL);
      Holder.Reference<ConfiguredFeature<?, ?>> $$6 = $$1.getOrThrow(AquaticFeatures.SEAGRASS_SIMPLE);
      Holder.Reference<ConfiguredFeature<?, ?>> $$7 = $$1.getOrThrow(AquaticFeatures.SEA_PICKLE);
      Holder.Reference<ConfiguredFeature<?, ?>> $$8 = $$1.getOrThrow(AquaticFeatures.KELP);
      Holder.Reference<ConfiguredFeature<?, ?>> $$9 = $$1.getOrThrow(AquaticFeatures.WARM_OCEAN_VEGETATION);
      PlacementUtils.register(p_321710_, SEAGRASS_WARM, $$2, seagrassPlacement(80));
      PlacementUtils.register(p_321710_, SEAGRASS_NORMAL, $$2, seagrassPlacement(48));
      PlacementUtils.register(p_321710_, SEAGRASS_COLD, $$2, seagrassPlacement(32));
      PlacementUtils.register(p_321710_, SEAGRASS_RIVER, $$3, seagrassPlacement(48));
      PlacementUtils.register(p_321710_, SEAGRASS_SWAMP, $$4, seagrassPlacement(64));
      PlacementUtils.register(p_321710_, SEAGRASS_DEEP_WARM, $$5, seagrassPlacement(80));
      PlacementUtils.register(p_321710_, SEAGRASS_DEEP, $$5, seagrassPlacement(48));
      PlacementUtils.register(p_321710_, SEAGRASS_DEEP_COLD, $$5, seagrassPlacement(40));
      PlacementUtils.register(
         p_321710_,
         SEAGRASS_SIMPLE,
         $$6,
         CarvingMaskPlacement.forStep(GenerationStep.Carving.LIQUID),
         RarityFilter.onAverageOnceEvery(10),
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(
               BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.STONE),
               BlockPredicate.matchesBlocks(BlockPos.ZERO, Blocks.WATER),
               BlockPredicate.matchesBlocks(Direction.UP.getNormal(), Blocks.WATER)
            )
         ),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_321710_, SEA_PICKLE, $$7, RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_321710_,
         KELP_COLD,
         $$8,
         NoiseBasedCountPlacement.of(120, 80.0, 0.0),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_321710_,
         KELP_WARM,
         $$8,
         NoiseBasedCountPlacement.of(80, 80.0, 0.0),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_321710_,
         WARM_OCEAN_VEGETATION,
         $$9,
         NoiseBasedCountPlacement.of(20, 400.0, 0.0),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
   }
}
