package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class FancyTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<FancyTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70136_ -> trunkPlacerParts(p_70136_).apply(p_70136_, FancyTrunkPlacer::new)
   );
   private static final double TRUNK_HEIGHT_SCALE = 0.618;
   private static final double CLUSTER_DENSITY_MAGIC = 1.382;
   private static final double BRANCH_SLOPE = 0.381;
   private static final double BRANCH_LENGTH_MAGIC = 0.328;

   public FancyTrunkPlacer(int p_70094_, int p_70095_, int p_70096_) {
      super(p_70094_, p_70095_, p_70096_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FANCY_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader p_226093_,
      BiConsumer<BlockPos, BlockState> p_226094_,
      RandomSource p_226095_,
      int p_226096_,
      BlockPos p_226097_,
      TreeConfiguration p_226098_
   ) {
      int $$6 = 5;
      int $$7 = p_226096_ + 2;
      int $$8 = Mth.floor($$7 * 0.618);
      setDirtAt(p_226093_, p_226094_, p_226095_, p_226097_.below(), p_226098_);
      double $$9 = 1.0;
      int $$10 = Math.min(1, Mth.floor(1.382 + Math.pow(1.0 * $$7 / 13.0, 2.0)));
      int $$11 = p_226097_.getY() + $$8;
      int $$12 = $$7 - 5;
      List<FancyTrunkPlacer.FoliageCoords> $$13 = Lists.newArrayList();
      $$13.add(new FancyTrunkPlacer.FoliageCoords(p_226097_.above($$12), $$11));

      for (; $$12 >= 0; $$12--) {
         float $$14 = treeShape($$7, $$12);
         if (!($$14 < 0.0F)) {
            for (int $$15 = 0; $$15 < $$10; $$15++) {
               double $$16 = 1.0;
               double $$17 = 1.0 * $$14 * (p_226095_.nextFloat() + 0.328);
               double $$18 = p_226095_.nextFloat() * 2.0F * Math.PI;
               double $$19 = $$17 * Math.sin($$18) + 0.5;
               double $$20 = $$17 * Math.cos($$18) + 0.5;
               BlockPos $$21 = p_226097_.offset(Mth.floor($$19), $$12 - 1, Mth.floor($$20));
               BlockPos $$22 = $$21.above(5);
               if (this.makeLimb(p_226093_, p_226094_, p_226095_, $$21, $$22, false, p_226098_)) {
                  int $$23 = p_226097_.getX() - $$21.getX();
                  int $$24 = p_226097_.getZ() - $$21.getZ();
                  double $$25 = $$21.getY() - Math.sqrt($$23 * $$23 + $$24 * $$24) * 0.381;
                  int $$26 = $$25 > $$11 ? $$11 : (int)$$25;
                  BlockPos $$27 = new BlockPos(p_226097_.getX(), $$26, p_226097_.getZ());
                  if (this.makeLimb(p_226093_, p_226094_, p_226095_, $$27, $$21, false, p_226098_)) {
                     $$13.add(new FancyTrunkPlacer.FoliageCoords($$21, $$27.getY()));
                  }
               }
            }
         }
      }

      this.makeLimb(p_226093_, p_226094_, p_226095_, p_226097_, p_226097_.above($$8), true, p_226098_);
      this.makeBranches(p_226093_, p_226094_, p_226095_, $$7, p_226097_, $$13, p_226098_);
      List<FoliagePlacer.FoliageAttachment> $$28 = Lists.newArrayList();

      for (FancyTrunkPlacer.FoliageCoords $$29 : $$13) {
         if (this.trimBranches($$7, $$29.getBranchBase() - p_226097_.getY())) {
            $$28.add($$29.attachment);
         }
      }

      return $$28;
   }

   private boolean makeLimb(
      LevelSimulatedReader p_226108_,
      BiConsumer<BlockPos, BlockState> p_226109_,
      RandomSource p_226110_,
      BlockPos p_226111_,
      BlockPos p_226112_,
      boolean p_226113_,
      TreeConfiguration p_226114_
   ) {
      if (!p_226113_ && Objects.equals(p_226111_, p_226112_)) {
         return true;
      } else {
         BlockPos $$7 = p_226112_.offset(-p_226111_.getX(), -p_226111_.getY(), -p_226111_.getZ());
         int $$8 = this.getSteps($$7);
         float $$9 = (float)$$7.getX() / $$8;
         float $$10 = (float)$$7.getY() / $$8;
         float $$11 = (float)$$7.getZ() / $$8;

         for (int $$12 = 0; $$12 <= $$8; $$12++) {
            BlockPos $$13 = p_226111_.offset(Mth.floor(0.5F + $$12 * $$9), Mth.floor(0.5F + $$12 * $$10), Mth.floor(0.5F + $$12 * $$11));
            if (p_226113_) {
               this.placeLog(
                  p_226108_,
                  p_226109_,
                  p_226110_,
                  $$13,
                  p_226114_,
                  p_161826_ -> p_161826_.trySetValue(RotatedPillarBlock.AXIS, this.getLogAxis(p_226111_, $$13))
               );
            } else if (!this.isFree(p_226108_, $$13)) {
               return false;
            }
         }

         return true;
      }
   }

   private int getSteps(BlockPos p_70128_) {
      int $$1 = Mth.abs(p_70128_.getX());
      int $$2 = Mth.abs(p_70128_.getY());
      int $$3 = Mth.abs(p_70128_.getZ());
      return Math.max($$1, Math.max($$2, $$3));
   }

   private Direction.Axis getLogAxis(BlockPos p_70130_, BlockPos p_70131_) {
      Direction.Axis $$2 = Direction.Axis.Y;
      int $$3 = Math.abs(p_70131_.getX() - p_70130_.getX());
      int $$4 = Math.abs(p_70131_.getZ() - p_70130_.getZ());
      int $$5 = Math.max($$3, $$4);
      if ($$5 > 0) {
         if ($$3 == $$5) {
            $$2 = Direction.Axis.X;
         } else {
            $$2 = Direction.Axis.Z;
         }
      }

      return $$2;
   }

   private boolean trimBranches(int p_70099_, int p_70100_) {
      return p_70100_ >= p_70099_ * 0.2;
   }

   private void makeBranches(
      LevelSimulatedReader p_226100_,
      BiConsumer<BlockPos, BlockState> p_226101_,
      RandomSource p_226102_,
      int p_226103_,
      BlockPos p_226104_,
      List<FancyTrunkPlacer.FoliageCoords> p_226105_,
      TreeConfiguration p_226106_
   ) {
      for (FancyTrunkPlacer.FoliageCoords $$7 : p_226105_) {
         int $$8 = $$7.getBranchBase();
         BlockPos $$9 = new BlockPos(p_226104_.getX(), $$8, p_226104_.getZ());
         if (!$$9.equals($$7.attachment.pos()) && this.trimBranches(p_226103_, $$8 - p_226104_.getY())) {
            this.makeLimb(p_226100_, p_226101_, p_226102_, $$9, $$7.attachment.pos(), true, p_226106_);
         }
      }
   }

   private static float treeShape(int p_70133_, int p_70134_) {
      if (p_70134_ < p_70133_ * 0.3F) {
         return -1.0F;
      } else {
         float $$2 = p_70133_ / 2.0F;
         float $$3 = $$2 - p_70134_;
         float $$4 = Mth.sqrt($$2 * $$2 - $$3 * $$3);
         if ($$3 == 0.0F) {
            $$4 = $$2;
         } else if (Math.abs($$3) >= $$2) {
            return 0.0F;
         }

         return $$4 * 0.5F;
      }
   }

   static class FoliageCoords {
      final FoliagePlacer.FoliageAttachment attachment;
      private final int branchBase;

      public FoliageCoords(BlockPos p_70140_, int p_70141_) {
         this.attachment = new FoliagePlacer.FoliageAttachment(p_70140_, 0, false);
         this.branchBase = p_70141_;
      }

      public int getBranchBase() {
         return this.branchBase;
      }
   }
}
