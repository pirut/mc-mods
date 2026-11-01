package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;

public class DripstoneClusterFeature extends Feature<DripstoneClusterConfiguration> {
   public DripstoneClusterFeature(Codec<DripstoneClusterConfiguration> p_159575_) {
      super(p_159575_);
   }

   @Override
   public boolean place(FeaturePlaceContext<DripstoneClusterConfiguration> p_159605_) {
      WorldGenLevel $$1 = p_159605_.level();
      BlockPos $$2 = p_159605_.origin();
      DripstoneClusterConfiguration $$3 = p_159605_.config();
      RandomSource $$4 = p_159605_.random();
      if (!DripstoneUtils.isEmptyOrWater($$1, $$2)) {
         return false;
      } else {
         int $$5 = $$3.height.sample($$4);
         float $$6 = $$3.wetness.sample($$4);
         float $$7 = $$3.density.sample($$4);
         int $$8 = $$3.radius.sample($$4);
         int $$9 = $$3.radius.sample($$4);

         for (int $$10 = -$$8; $$10 <= $$8; $$10++) {
            for (int $$11 = -$$9; $$11 <= $$9; $$11++) {
               double $$12 = this.getChanceOfStalagmiteOrStalactite($$8, $$9, $$10, $$11, $$3);
               BlockPos $$13 = $$2.offset($$10, 0, $$11);
               this.placeColumn($$1, $$4, $$13, $$10, $$11, $$6, $$12, $$5, $$7, $$3);
            }
         }

         return true;
      }
   }

   private void placeColumn(
      WorldGenLevel p_225016_,
      RandomSource p_225017_,
      BlockPos p_225018_,
      int p_225019_,
      int p_225020_,
      float p_225021_,
      double p_225022_,
      int p_225023_,
      float p_225024_,
      DripstoneClusterConfiguration p_225025_
   ) {
      Optional<Column> $$10 = Column.scan(
         p_225016_, p_225018_, p_225025_.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isNeitherEmptyNorWater
      );
      if (!$$10.isEmpty()) {
         OptionalInt $$11 = $$10.get().getCeiling();
         OptionalInt $$12 = $$10.get().getFloor();
         if (!$$11.isEmpty() || !$$12.isEmpty()) {
            boolean $$13 = p_225017_.nextFloat() < p_225021_;
            Column $$15;
            if ($$13 && $$12.isPresent() && this.canPlacePool(p_225016_, p_225018_.atY($$12.getAsInt()))) {
               int $$14 = $$12.getAsInt();
               $$15 = $$10.get().withFloor(OptionalInt.of($$14 - 1));
               p_225016_.setBlock(p_225018_.atY($$14), Blocks.WATER.defaultBlockState(), 2);
            } else {
               $$15 = $$10.get();
            }

            OptionalInt $$17 = $$15.getFloor();
            boolean $$18 = p_225017_.nextDouble() < p_225022_;
            int $$22;
            if ($$11.isPresent() && $$18 && !this.isLava(p_225016_, p_225018_.atY($$11.getAsInt()))) {
               int $$19 = p_225025_.dripstoneBlockLayerThickness.sample(p_225017_);
               this.replaceBlocksWithDripstoneBlocks(p_225016_, p_225018_.atY($$11.getAsInt()), $$19, Direction.UP);
               int $$20;
               if ($$17.isPresent()) {
                  $$20 = Math.min(p_225023_, $$11.getAsInt() - $$17.getAsInt());
               } else {
                  $$20 = p_225023_;
               }

               $$22 = this.getDripstoneHeight(p_225017_, p_225019_, p_225020_, p_225024_, $$20, p_225025_);
            } else {
               $$22 = 0;
            }

            boolean $$24 = p_225017_.nextDouble() < p_225022_;
            int $$26;
            if ($$17.isPresent() && $$24 && !this.isLava(p_225016_, p_225018_.atY($$17.getAsInt()))) {
               int $$25 = p_225025_.dripstoneBlockLayerThickness.sample(p_225017_);
               this.replaceBlocksWithDripstoneBlocks(p_225016_, p_225018_.atY($$17.getAsInt()), $$25, Direction.DOWN);
               if ($$11.isPresent()) {
                  $$26 = Math.max(
                     0, $$22 + Mth.randomBetweenInclusive(p_225017_, -p_225025_.maxStalagmiteStalactiteHeightDiff, p_225025_.maxStalagmiteStalactiteHeightDiff)
                  );
               } else {
                  $$26 = this.getDripstoneHeight(p_225017_, p_225019_, p_225020_, p_225024_, p_225023_, p_225025_);
               }
            } else {
               $$26 = 0;
            }

            int $$36;
            int $$35;
            if ($$11.isPresent() && $$17.isPresent() && $$11.getAsInt() - $$22 <= $$17.getAsInt() + $$26) {
               int $$29 = $$17.getAsInt();
               int $$30 = $$11.getAsInt();
               int $$31 = Math.max($$30 - $$22, $$29 + 1);
               int $$32 = Math.min($$29 + $$26, $$30 - 1);
               int $$33 = Mth.randomBetweenInclusive(p_225017_, $$31, $$32 + 1);
               int $$34 = $$33 - 1;
               $$35 = $$30 - $$33;
               $$36 = $$34 - $$29;
            } else {
               $$35 = $$22;
               $$36 = $$26;
            }

            boolean $$39 = p_225017_.nextBoolean() && $$35 > 0 && $$36 > 0 && $$15.getHeight().isPresent() && $$35 + $$36 == $$15.getHeight().getAsInt();
            if ($$11.isPresent()) {
               DripstoneUtils.growPointedDripstone(p_225016_, p_225018_.atY($$11.getAsInt() - 1), Direction.DOWN, $$35, $$39);
            }

            if ($$17.isPresent()) {
               DripstoneUtils.growPointedDripstone(p_225016_, p_225018_.atY($$17.getAsInt() + 1), Direction.UP, $$36, $$39);
            }
         }
      }
   }

   private boolean isLava(LevelReader p_159586_, BlockPos p_159587_) {
      return p_159586_.getBlockState(p_159587_).is(Blocks.LAVA);
   }

   private int getDripstoneHeight(RandomSource p_225009_, int p_225010_, int p_225011_, float p_225012_, int p_225013_, DripstoneClusterConfiguration p_225014_) {
      if (p_225009_.nextFloat() > p_225012_) {
         return 0;
      } else {
         int $$6 = Math.abs(p_225010_) + Math.abs(p_225011_);
         float $$7 = (float)Mth.clampedMap((double)$$6, 0.0, (double)p_225014_.maxDistanceFromCenterAffectingHeightBias, p_225013_ / 2.0, 0.0);
         return (int)randomBetweenBiased(p_225009_, 0.0F, p_225013_, $$7, p_225014_.heightDeviation);
      }
   }

   private boolean canPlacePool(WorldGenLevel p_159620_, BlockPos p_159621_) {
      BlockState $$2 = p_159620_.getBlockState(p_159621_);
      if (!$$2.is(Blocks.WATER) && !$$2.is(Blocks.DRIPSTONE_BLOCK) && !$$2.is(Blocks.POINTED_DRIPSTONE)) {
         if (p_159620_.getBlockState(p_159621_.above()).getFluidState().is(FluidTags.WATER)) {
            return false;
         } else {
            for (Direction $$3 : Direction.Plane.HORIZONTAL) {
               if (!this.canBeAdjacentToWater(p_159620_, p_159621_.relative($$3))) {
                  return false;
               }
            }

            return this.canBeAdjacentToWater(p_159620_, p_159621_.below());
         }
      } else {
         return false;
      }
   }

   private boolean canBeAdjacentToWater(LevelAccessor p_159583_, BlockPos p_159584_) {
      BlockState $$2 = p_159583_.getBlockState(p_159584_);
      return $$2.is(BlockTags.BASE_STONE_OVERWORLD) || $$2.getFluidState().is(FluidTags.WATER);
   }

   private void replaceBlocksWithDripstoneBlocks(WorldGenLevel p_159589_, BlockPos p_159590_, int p_159591_, Direction p_159592_) {
      BlockPos.MutableBlockPos $$4 = p_159590_.mutable();

      for (int $$5 = 0; $$5 < p_159591_; $$5++) {
         if (!DripstoneUtils.placeDripstoneBlockIfPossible(p_159589_, $$4)) {
            return;
         }

         $$4.move(p_159592_);
      }
   }

   private double getChanceOfStalagmiteOrStalactite(int p_159577_, int p_159578_, int p_159579_, int p_159580_, DripstoneClusterConfiguration p_159581_) {
      int $$5 = p_159577_ - Math.abs(p_159579_);
      int $$6 = p_159578_ - Math.abs(p_159580_);
      int $$7 = Math.min($$5, $$6);
      return Mth.clampedMap(
         (float)$$7, 0.0F, (float)p_159581_.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn, p_159581_.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0F
      );
   }

   private static float randomBetweenBiased(RandomSource p_225003_, float p_225004_, float p_225005_, float p_225006_, float p_225007_) {
      return ClampedNormalFloat.sample(p_225003_, p_225006_, p_225007_, p_225004_, p_225005_);
   }
}
