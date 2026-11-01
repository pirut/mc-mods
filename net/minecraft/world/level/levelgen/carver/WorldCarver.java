package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
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

   private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String p_65066_, F p_65067_) {
      return Registry.register(BuiltInRegistries.CARVER, p_65066_, p_65067_);
   }

   public WorldCarver(Codec<C> p_159366_) {
      this.configuredCodec = p_159366_.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config);
   }

   public ConfiguredWorldCarver<C> configured(C p_65064_) {
      return new ConfiguredWorldCarver<>(this, p_65064_);
   }

   public MapCodec<ConfiguredWorldCarver<C>> configuredCodec() {
      return this.configuredCodec;
   }

   public int getRange() {
      return 4;
   }

   protected boolean carveEllipsoid(
      CarvingContext p_190754_,
      C p_190755_,
      ChunkAccess p_190756_,
      Function<BlockPos, Holder<Biome>> p_190757_,
      Aquifer p_190758_,
      double p_190759_,
      double p_190760_,
      double p_190761_,
      double p_190762_,
      double p_190763_,
      CarvingMask p_190764_,
      WorldCarver.CarveSkipChecker p_190765_
   ) {
      ChunkPos $$12 = p_190756_.getPos();
      double $$13 = $$12.getMiddleBlockX();
      double $$14 = $$12.getMiddleBlockZ();
      double $$15 = 16.0 + p_190762_ * 2.0;
      if (!(Math.abs(p_190759_ - $$13) > $$15) && !(Math.abs(p_190761_ - $$14) > $$15)) {
         int $$16 = $$12.getMinBlockX();
         int $$17 = $$12.getMinBlockZ();
         int $$18 = Math.max(Mth.floor(p_190759_ - p_190762_) - $$16 - 1, 0);
         int $$19 = Math.min(Mth.floor(p_190759_ + p_190762_) - $$16, 15);
         int $$20 = Math.max(Mth.floor(p_190760_ - p_190763_) - 1, p_190754_.getMinGenY() + 1);
         int $$21 = p_190756_.isUpgrading() ? 0 : 7;
         int $$22 = Math.min(Mth.floor(p_190760_ + p_190763_) + 1, p_190754_.getMinGenY() + p_190754_.getGenDepth() - 1 - $$21);
         int $$23 = Math.max(Mth.floor(p_190761_ - p_190762_) - $$17 - 1, 0);
         int $$24 = Math.min(Mth.floor(p_190761_ + p_190762_) - $$17, 15);
         boolean $$25 = false;
         BlockPos.MutableBlockPos $$26 = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos $$27 = new BlockPos.MutableBlockPos();

         for (int $$28 = $$18; $$28 <= $$19; $$28++) {
            int $$29 = $$12.getBlockX($$28);
            double $$30 = ($$29 + 0.5 - p_190759_) / p_190762_;

            for (int $$31 = $$23; $$31 <= $$24; $$31++) {
               int $$32 = $$12.getBlockZ($$31);
               double $$33 = ($$32 + 0.5 - p_190761_) / p_190762_;
               if (!($$30 * $$30 + $$33 * $$33 >= 1.0)) {
                  MutableBoolean $$34 = new MutableBoolean(false);

                  for (int $$35 = $$22; $$35 > $$20; $$35--) {
                     double $$36 = ($$35 - 0.5 - p_190760_) / p_190763_;
                     if (!p_190765_.shouldSkip(p_190754_, $$30, $$36, $$33, $$35) && (!p_190764_.get($$28, $$35, $$31) || isDebugEnabled(p_190755_))) {
                        p_190764_.set($$28, $$35, $$31);
                        $$26.set($$29, $$35, $$32);
                        $$25 |= this.carveBlock(p_190754_, p_190755_, p_190756_, p_190757_, p_190764_, $$26, $$27, p_190758_, $$34);
                     }
                  }
               }
            }
         }

         return $$25;
      } else {
         return false;
      }
   }

   protected boolean carveBlock(
      CarvingContext p_190744_,
      C p_190745_,
      ChunkAccess p_190746_,
      Function<BlockPos, Holder<Biome>> p_190747_,
      CarvingMask p_190748_,
      BlockPos.MutableBlockPos p_190749_,
      BlockPos.MutableBlockPos p_190750_,
      Aquifer p_190751_,
      MutableBoolean p_190752_
   ) {
      BlockState $$9 = p_190746_.getBlockState(p_190749_);
      if ($$9.is(Blocks.GRASS_BLOCK) || $$9.is(Blocks.MYCELIUM)) {
         p_190752_.setTrue();
      }

      if (!this.canReplaceBlock(p_190745_, $$9) && !isDebugEnabled(p_190745_)) {
         return false;
      } else {
         BlockState $$10 = this.getCarveState(p_190744_, p_190745_, p_190749_, p_190751_);
         if ($$10 == null) {
            return false;
         } else {
            p_190746_.setBlockState(p_190749_, $$10, false);
            if (p_190751_.shouldScheduleFluidUpdate() && !$$10.getFluidState().isEmpty()) {
               p_190746_.markPosForPostprocessing(p_190749_);
            }

            if (p_190752_.isTrue()) {
               p_190750_.setWithOffset(p_190749_, Direction.DOWN);
               if (p_190746_.getBlockState(p_190750_).is(Blocks.DIRT)) {
                  p_190744_.topMaterial(p_190747_, p_190746_, p_190750_, !$$10.getFluidState().isEmpty()).ifPresent(p_284918_ -> {
                     p_190746_.setBlockState(p_190750_, p_284918_, false);
                     if (!p_284918_.getFluidState().isEmpty()) {
                        p_190746_.markPosForPostprocessing(p_190750_);
                     }
                  });
               }
            }

            return true;
         }
      }
   }

   @Nullable
   private BlockState getCarveState(CarvingContext p_159419_, C p_159420_, BlockPos p_159421_, Aquifer p_159422_) {
      if (p_159421_.getY() <= p_159420_.lavaLevel.resolveY(p_159419_)) {
         return LAVA.createLegacyBlock();
      } else {
         BlockState $$4 = p_159422_.computeSubstance(new DensityFunction.SinglePointContext(p_159421_.getX(), p_159421_.getY(), p_159421_.getZ()), 0.0);
         if ($$4 == null) {
            return isDebugEnabled(p_159420_) ? p_159420_.debugSettings.getBarrierState() : null;
         } else {
            return isDebugEnabled(p_159420_) ? getDebugState(p_159420_, $$4) : $$4;
         }
      }
   }

   private static BlockState getDebugState(CarverConfiguration p_159382_, BlockState p_159383_) {
      if (p_159383_.is(Blocks.AIR)) {
         return p_159382_.debugSettings.getAirState();
      } else if (p_159383_.is(Blocks.WATER)) {
         BlockState $$2 = p_159382_.debugSettings.getWaterState();
         return $$2.hasProperty(BlockStateProperties.WATERLOGGED) ? $$2.setValue(BlockStateProperties.WATERLOGGED, true) : $$2;
      } else {
         return p_159383_.is(Blocks.LAVA) ? p_159382_.debugSettings.getLavaState() : p_159383_;
      }
   }

   public abstract boolean carve(
      CarvingContext var1, C var2, ChunkAccess var3, Function<BlockPos, Holder<Biome>> var4, RandomSource var5, Aquifer var6, ChunkPos var7, CarvingMask var8
   );

   public abstract boolean isStartChunk(C var1, RandomSource var2);

   protected boolean canReplaceBlock(C p_224911_, BlockState p_224912_) {
      return p_224912_.is(p_224911_.replaceable);
   }

   protected static boolean canReach(ChunkPos p_159368_, double p_159369_, double p_159370_, int p_159371_, int p_159372_, float p_159373_) {
      double $$6 = p_159368_.getMiddleBlockX();
      double $$7 = p_159368_.getMiddleBlockZ();
      double $$8 = p_159369_ - $$6;
      double $$9 = p_159370_ - $$7;
      double $$10 = p_159372_ - p_159371_;
      double $$11 = p_159373_ + 2.0F + 16.0F;
      return $$8 * $$8 + $$9 * $$9 - $$10 * $$10 <= $$11 * $$11;
   }

   private static boolean isDebugEnabled(CarverConfiguration p_159424_) {
      return p_159424_.debugSettings.isDebugMode();
   }

   public interface CarveSkipChecker {
      boolean shouldSkip(CarvingContext var1, double var2, double var4, double var6, int var8);
   }
}
