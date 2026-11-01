package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

public abstract class WorldCarver<C extends CarverConfiguration> {
   public static final WorldCarver<CaveCarverConfiguration> CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
   public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
   public static final WorldCarver<CanyonCarverConfiguration> CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
   protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
   protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
   protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
   protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
   protected Set<Fluid> liquids = ImmutableSet.of(Fluids.WATER);
   private final MapCodec<ConfiguredWorldCarver<C>> configuredCodec;

   private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String key, F carver) {
      return Registry.register(BuiltInRegistries.CARVER, key, carver);
   }

   public WorldCarver(Codec<C> codec) {
      this.configuredCodec = codec.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config);
   }

   public ConfiguredWorldCarver<C> configured(C config) {
      return new ConfiguredWorldCarver<>(this, config);
   }

   public MapCodec<ConfiguredWorldCarver<C>> configuredCodec() {
      return this.configuredCodec;
   }

   public int getRange() {
      return 4;
   }

   protected boolean carveEllipsoid(
      CarvingContext context,
      C config,
      ChunkAccess chunk,
      Function<BlockPos, Holder<Biome>> biomeAccessor,
      Aquifer aquifer,
      double x,
      double y,
      double z,
      double horizontalRadius,
      double verticalRadius,
      CarvingMask carvingMask,
      WorldCarver.CarveSkipChecker skipChecker
   ) {
      ChunkPos chunkPos = chunk.getPos();
      double d = chunkPos.getMiddleBlockX();
      double e = chunkPos.getMiddleBlockZ();
      double f = 16.0 + horizontalRadius * 2.0;
      if (!(Math.abs(x - d) > f) && !(Math.abs(z - e) > f)) {
         int i = chunkPos.getMinBlockX();
         int j = chunkPos.getMinBlockZ();
         int k = Math.max(Mth.floor(x - horizontalRadius) - i - 1, 0);
         int l = Math.min(Mth.floor(x + horizontalRadius) - i, 15);
         int m = Math.max(Mth.floor(y - verticalRadius) - 1, context.getMinGenY() + 1);
         int n = chunk.isUpgrading() ? 0 : 7;
         int o = Math.min(Mth.floor(y + verticalRadius) + 1, context.getMinGenY() + context.getGenDepth() - 1 - n);
         int p = Math.max(Mth.floor(z - horizontalRadius) - j - 1, 0);
         int q = Math.min(Mth.floor(z + horizontalRadius) - j, 15);
         boolean bl = false;
         BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();

         for (int r = k; r <= l; r++) {
            int s = chunkPos.getBlockX(r);
            double g = (s + 0.5 - x) / horizontalRadius;

            for (int t = p; t <= q; t++) {
               int u = chunkPos.getBlockZ(t);
               double h = (u + 0.5 - z) / horizontalRadius;
               if (!(g * g + h * h >= 1.0)) {
                  MutableBoolean mutableBoolean = new MutableBoolean(false);

                  for (int v = o; v > m; v--) {
                     double w = (v - 0.5 - y) / verticalRadius;
                     if (!skipChecker.shouldSkip(context, g, w, h, v) && (!carvingMask.get(r, v, t) || isDebugEnabled(config))) {
                        carvingMask.set(r, v, t);
                        mutableBlockPos.set(s, v, u);
                        bl |= this.carveBlock(context, config, chunk, biomeAccessor, carvingMask, mutableBlockPos, mutableBlockPos2, aquifer, mutableBoolean);
                     }
                  }
               }
            }
         }

         return bl;
      } else {
         return false;
      }
   }

   protected boolean carveBlock(
      CarvingContext context,
      C config,
      ChunkAccess chunk,
      Function<BlockPos, Holder<Biome>> biomeGetter,
      CarvingMask carvingMask,
      BlockPos.MutableBlockPos pos,
      BlockPos.MutableBlockPos checkPos,
      Aquifer aquifer,
      MutableBoolean reachedSurface
   ) {
      BlockState blockState = chunk.getBlockState(pos);
      if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
         reachedSurface.setTrue();
      }

      if (!this.canReplaceBlock(config, blockState) && !isDebugEnabled(config)) {
         return false;
      } else {
         BlockState blockState2 = this.getCarveState(context, config, pos, aquifer);
         if (blockState2 == null) {
            return false;
         } else {
            chunk.setBlockState(pos, blockState2);
            if (aquifer.shouldScheduleFluidUpdate() && !blockState2.getFluidState().isEmpty()) {
               chunk.markPosForPostprocessing(pos);
            }

            if (reachedSurface.isTrue()) {
               checkPos.setWithOffset(pos, Direction.DOWN);
               if (chunk.getBlockState(checkPos).is(Blocks.DIRT)) {
                  context.topMaterial(biomeGetter, chunk, checkPos, !blockState2.getFluidState().isEmpty()).ifPresent(blockStatex -> {
                     chunk.setBlockState(checkPos, blockStatex);
                     if (!blockStatex.getFluidState().isEmpty()) {
                        chunk.markPosForPostprocessing(checkPos);
                     }
                  });
               }
            }

            return true;
         }
      }
   }

   @Nullable
   private BlockState getCarveState(CarvingContext context, C config, BlockPos pos, Aquifer aquifer) {
      if (pos.getY() <= config.lavaLevel.resolveY(context)) {
         return LAVA.createLegacyBlock();
      } else {
         BlockState blockState = aquifer.computeSubstance(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()), 0.0);
         if (blockState == null) {
            return isDebugEnabled(config) ? config.debugSettings.getBarrierState() : null;
         } else {
            return isDebugEnabled(config) ? getDebugState(config, blockState) : blockState;
         }
      }
   }

   private static BlockState getDebugState(CarverConfiguration config, BlockState state) {
      if (state.is(Blocks.AIR)) {
         return config.debugSettings.getAirState();
      } else if (state.is(Blocks.WATER)) {
         BlockState blockState = config.debugSettings.getWaterState();
         return blockState.hasProperty(BlockStateProperties.WATERLOGGED) ? blockState.setValue(BlockStateProperties.WATERLOGGED, true) : blockState;
      } else {
         return state.is(Blocks.LAVA) ? config.debugSettings.getLavaState() : state;
      }
   }

   public abstract boolean carve(
      CarvingContext context,
      C config,
      ChunkAccess chunk,
      Function<BlockPos, Holder<Biome>> biomeAccessor,
      RandomSource random,
      Aquifer aquifer,
      ChunkPos chunkPos,
      CarvingMask carvingMask
   );

   public abstract boolean isStartChunk(C config, RandomSource random);

   protected boolean canReplaceBlock(C config, BlockState state) {
      return state.is(config.replaceable);
   }

   protected static boolean canReach(ChunkPos chunkPos, double x, double z, int branchIndex, int branchCount, float width) {
      double d = chunkPos.getMiddleBlockX();
      double e = chunkPos.getMiddleBlockZ();
      double f = x - d;
      double g = z - e;
      double h = branchCount - branchIndex;
      double i = width + 2.0F + 16.0F;
      return f * f + g * g - h * h <= i * i;
   }

   private static boolean isDebugEnabled(CarverConfiguration config) {
      return SharedConstants.DEBUG_CARVERS || config.debugSettings.isDebugMode();
   }

   public interface CarveSkipChecker {
      boolean shouldSkip(CarvingContext carvingContext, double d, double e, double f, int i);
   }
}
