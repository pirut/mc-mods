package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class Shapes {
   public static final double EPSILON = 1.0E-7;
   public static final double BIG_EPSILON = 1.0E-6;
   private static final VoxelShape BLOCK = Util.make(() -> {
      DiscreteVoxelShape $$0 = new BitSetDiscreteVoxelShape(1, 1, 1);
      $$0.fill(0, 0, 0);
      return new CubeVoxelShape($$0);
   });
   public static final VoxelShape INFINITY = box(
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY
   );
   private static final VoxelShape EMPTY = new ArrayVoxelShape(
      new BitSetDiscreteVoxelShape(0, 0, 0),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0})
   );

   public static VoxelShape empty() {
      return EMPTY;
   }

   public static VoxelShape block() {
      return BLOCK;
   }

   public static VoxelShape box(double p_83049_, double p_83050_, double p_83051_, double p_83052_, double p_83053_, double p_83054_) {
      if (!(p_83049_ > p_83052_) && !(p_83050_ > p_83053_) && !(p_83051_ > p_83054_)) {
         return create(p_83049_, p_83050_, p_83051_, p_83052_, p_83053_, p_83054_);
      } else {
         throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
      }
   }

   public static VoxelShape create(double p_166050_, double p_166051_, double p_166052_, double p_166053_, double p_166054_, double p_166055_) {
      if (!(p_166053_ - p_166050_ < 1.0E-7) && !(p_166054_ - p_166051_ < 1.0E-7) && !(p_166055_ - p_166052_ < 1.0E-7)) {
         int $$6 = findBits(p_166050_, p_166053_);
         int $$7 = findBits(p_166051_, p_166054_);
         int $$8 = findBits(p_166052_, p_166055_);
         if ($$6 < 0 || $$7 < 0 || $$8 < 0) {
            return new ArrayVoxelShape(
               BLOCK.shape,
               DoubleArrayList.wrap(new double[]{p_166050_, p_166053_}),
               DoubleArrayList.wrap(new double[]{p_166051_, p_166054_}),
               DoubleArrayList.wrap(new double[]{p_166052_, p_166055_})
            );
         } else if ($$6 == 0 && $$7 == 0 && $$8 == 0) {
            return block();
         } else {
            int $$9 = 1 << $$6;
            int $$10 = 1 << $$7;
            int $$11 = 1 << $$8;
            BitSetDiscreteVoxelShape $$12 = BitSetDiscreteVoxelShape.withFilledBounds(
               $$9,
               $$10,
               $$11,
               (int)Math.round(p_166050_ * $$9),
               (int)Math.round(p_166051_ * $$10),
               (int)Math.round(p_166052_ * $$11),
               (int)Math.round(p_166053_ * $$9),
               (int)Math.round(p_166054_ * $$10),
               (int)Math.round(p_166055_ * $$11)
            );
            return new CubeVoxelShape($$12);
         }
      } else {
         return empty();
      }
   }

   public static VoxelShape create(AABB p_83065_) {
      return create(p_83065_.minX, p_83065_.minY, p_83065_.minZ, p_83065_.maxX, p_83065_.maxY, p_83065_.maxZ);
   }

   @VisibleForTesting
   protected static int findBits(double p_83042_, double p_83043_) {
      if (!(p_83042_ < -1.0E-7) && !(p_83043_ > 1.0000001)) {
         for (int $$2 = 0; $$2 <= 3; $$2++) {
            int $$3 = 1 << $$2;
            double $$4 = p_83042_ * $$3;
            double $$5 = p_83043_ * $$3;
            boolean $$6 = Math.abs($$4 - Math.round($$4)) < 1.0E-7 * $$3;
            boolean $$7 = Math.abs($$5 - Math.round($$5)) < 1.0E-7 * $$3;
            if ($$6 && $$7) {
               return $$2;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static long lcm(int p_83056_, int p_83057_) {
      return (long)p_83056_ * (p_83057_ / IntMath.gcd(p_83056_, p_83057_));
   }

   public static VoxelShape or(VoxelShape p_83111_, VoxelShape p_83112_) {
      return join(p_83111_, p_83112_, BooleanOp.OR);
   }

   public static VoxelShape or(VoxelShape p_83125_, VoxelShape... p_83126_) {
      return Arrays.stream(p_83126_).reduce(p_83125_, Shapes::or);
   }

   public static VoxelShape join(VoxelShape p_83114_, VoxelShape p_83115_, BooleanOp p_83116_) {
      return joinUnoptimized(p_83114_, p_83115_, p_83116_).optimize();
   }

   public static VoxelShape joinUnoptimized(VoxelShape p_83149_, VoxelShape p_83150_, BooleanOp p_83151_) {
      if (p_83151_.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else if (p_83149_ == p_83150_) {
         return p_83151_.apply(true, true) ? p_83149_ : empty();
      } else {
         boolean $$3 = p_83151_.apply(true, false);
         boolean $$4 = p_83151_.apply(false, true);
         if (p_83149_.isEmpty()) {
            return $$4 ? p_83150_ : empty();
         } else if (p_83150_.isEmpty()) {
            return $$3 ? p_83149_ : empty();
         } else {
            IndexMerger $$5 = createIndexMerger(1, p_83149_.getCoords(Direction.Axis.X), p_83150_.getCoords(Direction.Axis.X), $$3, $$4);
            IndexMerger $$6 = createIndexMerger($$5.size() - 1, p_83149_.getCoords(Direction.Axis.Y), p_83150_.getCoords(Direction.Axis.Y), $$3, $$4);
            IndexMerger $$7 = createIndexMerger(
               ($$5.size() - 1) * ($$6.size() - 1), p_83149_.getCoords(Direction.Axis.Z), p_83150_.getCoords(Direction.Axis.Z), $$3, $$4
            );
            BitSetDiscreteVoxelShape $$8 = BitSetDiscreteVoxelShape.join(p_83149_.shape, p_83150_.shape, $$5, $$6, $$7, p_83151_);
            return (VoxelShape)($$5 instanceof DiscreteCubeMerger && $$6 instanceof DiscreteCubeMerger && $$7 instanceof DiscreteCubeMerger
               ? new CubeVoxelShape($$8)
               : new ArrayVoxelShape($$8, $$5.getList(), $$6.getList(), $$7.getList()));
         }
      }
   }

   public static boolean joinIsNotEmpty(VoxelShape p_83158_, VoxelShape p_83159_, BooleanOp p_83160_) {
      if (p_83160_.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else {
         boolean $$3 = p_83158_.isEmpty();
         boolean $$4 = p_83159_.isEmpty();
         if (!$$3 && !$$4) {
            if (p_83158_ == p_83159_) {
               return p_83160_.apply(true, true);
            } else {
               boolean $$5 = p_83160_.apply(true, false);
               boolean $$6 = p_83160_.apply(false, true);

               for (Direction.Axis $$7 : AxisCycle.AXIS_VALUES) {
                  if (p_83158_.max($$7) < p_83159_.min($$7) - 1.0E-7) {
                     return $$5 || $$6;
                  }

                  if (p_83159_.max($$7) < p_83158_.min($$7) - 1.0E-7) {
                     return $$5 || $$6;
                  }
               }

               IndexMerger $$8 = createIndexMerger(1, p_83158_.getCoords(Direction.Axis.X), p_83159_.getCoords(Direction.Axis.X), $$5, $$6);
               IndexMerger $$9 = createIndexMerger($$8.size() - 1, p_83158_.getCoords(Direction.Axis.Y), p_83159_.getCoords(Direction.Axis.Y), $$5, $$6);
               IndexMerger $$10 = createIndexMerger(
                  ($$8.size() - 1) * ($$9.size() - 1), p_83158_.getCoords(Direction.Axis.Z), p_83159_.getCoords(Direction.Axis.Z), $$5, $$6
               );
               return joinIsNotEmpty($$8, $$9, $$10, p_83158_.shape, p_83159_.shape, p_83160_);
            }
         } else {
            return p_83160_.apply(!$$3, !$$4);
         }
      }
   }

   private static boolean joinIsNotEmpty(
      IndexMerger p_83104_, IndexMerger p_83105_, IndexMerger p_83106_, DiscreteVoxelShape p_83107_, DiscreteVoxelShape p_83108_, BooleanOp p_83109_
   ) {
      return !p_83104_.forMergedIndexes(
         (p_83100_, p_83101_, p_83102_) -> p_83105_.forMergedIndexes(
            (p_166046_, p_166047_, p_166048_) -> p_83106_.forMergedIndexes(
               (p_166036_, p_166037_, p_166038_) -> !p_83109_.apply(
                  p_83107_.isFullWide(p_83100_, p_166046_, p_166036_), p_83108_.isFullWide(p_83101_, p_166047_, p_166037_)
               )
            )
         )
      );
   }

   public static double collide(Direction.Axis p_193136_, AABB p_193137_, Iterable<VoxelShape> p_193138_, double p_193139_) {
      for (VoxelShape $$4 : p_193138_) {
         if (Math.abs(p_193139_) < 1.0E-7) {
            return 0.0;
         }

         p_193139_ = $$4.collide(p_193136_, p_193137_, p_193139_);
      }

      return p_193139_;
   }

   public static boolean blockOccudes(VoxelShape p_83118_, VoxelShape p_83119_, Direction p_83120_) {
      if (p_83118_ == block() && p_83119_ == block()) {
         return true;
      } else if (p_83119_.isEmpty()) {
         return false;
      } else {
         Direction.Axis $$3 = p_83120_.getAxis();
         Direction.AxisDirection $$4 = p_83120_.getAxisDirection();
         VoxelShape $$5 = $$4 == Direction.AxisDirection.POSITIVE ? p_83118_ : p_83119_;
         VoxelShape $$6 = $$4 == Direction.AxisDirection.POSITIVE ? p_83119_ : p_83118_;
         BooleanOp $$7 = $$4 == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
         return DoubleMath.fuzzyEquals($$5.max($$3), 1.0, 1.0E-7)
            && DoubleMath.fuzzyEquals($$6.min($$3), 0.0, 1.0E-7)
            && !joinIsNotEmpty(new SliceShape($$5, $$3, $$5.shape.getSize($$3) - 1), new SliceShape($$6, $$3, 0), $$7);
      }
   }

   public static VoxelShape getFaceShape(VoxelShape p_83122_, Direction p_83123_) {
      if (p_83122_ == block()) {
         return block();
      } else {
         Direction.Axis $$2 = p_83123_.getAxis();
         boolean $$3;
         int $$4;
         if (p_83123_.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            $$3 = DoubleMath.fuzzyEquals(p_83122_.max($$2), 1.0, 1.0E-7);
            $$4 = p_83122_.shape.getSize($$2) - 1;
         } else {
            $$3 = DoubleMath.fuzzyEquals(p_83122_.min($$2), 0.0, 1.0E-7);
            $$4 = 0;
         }

         return (VoxelShape)(!$$3 ? empty() : new SliceShape(p_83122_, $$2, $$4));
      }
   }

   public static boolean mergedFaceOccludes(VoxelShape p_83153_, VoxelShape p_83154_, Direction p_83155_) {
      if (p_83153_ != block() && p_83154_ != block()) {
         Direction.Axis $$3 = p_83155_.getAxis();
         Direction.AxisDirection $$4 = p_83155_.getAxisDirection();
         VoxelShape $$5 = $$4 == Direction.AxisDirection.POSITIVE ? p_83153_ : p_83154_;
         VoxelShape $$6 = $$4 == Direction.AxisDirection.POSITIVE ? p_83154_ : p_83153_;
         if (!DoubleMath.fuzzyEquals($$5.max($$3), 1.0, 1.0E-7)) {
            $$5 = empty();
         }

         if (!DoubleMath.fuzzyEquals($$6.min($$3), 0.0, 1.0E-7)) {
            $$6 = empty();
         }

         return !joinIsNotEmpty(
            block(), joinUnoptimized(new SliceShape($$5, $$3, $$5.shape.getSize($$3) - 1), new SliceShape($$6, $$3, 0), BooleanOp.OR), BooleanOp.ONLY_FIRST
         );
      } else {
         return true;
      }
   }

   public static boolean faceShapeOccludes(VoxelShape p_83146_, VoxelShape p_83147_) {
      if (p_83146_ == block() || p_83147_ == block()) {
         return true;
      } else {
         return p_83146_.isEmpty() && p_83147_.isEmpty()
            ? false
            : !joinIsNotEmpty(block(), joinUnoptimized(p_83146_, p_83147_, BooleanOp.OR), BooleanOp.ONLY_FIRST);
      }
   }

   @VisibleForTesting
   protected static IndexMerger createIndexMerger(int p_83059_, DoubleList p_83060_, DoubleList p_83061_, boolean p_83062_, boolean p_83063_) {
      int $$5 = p_83060_.size() - 1;
      int $$6 = p_83061_.size() - 1;
      if (p_83060_ instanceof CubePointRange && p_83061_ instanceof CubePointRange) {
         long $$7 = lcm($$5, $$6);
         if (p_83059_ * $$7 <= 256L) {
            return new DiscreteCubeMerger($$5, $$6);
         }
      }

      if (p_83060_.getDouble($$5) < p_83061_.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(p_83060_, p_83061_, false);
      } else if (p_83061_.getDouble($$6) < p_83060_.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(p_83061_, p_83060_, true);
      } else {
         return (IndexMerger)($$5 == $$6 && Objects.equals(p_83060_, p_83061_)
            ? new IdenticalMerger(p_83060_)
            : new IndirectMerger(p_83060_, p_83061_, p_83062_, p_83063_));
      }
   }

   public interface DoubleLineConsumer {
      void consume(double var1, double var3, double var5, double var7, double var9, double var11);
   }
}
