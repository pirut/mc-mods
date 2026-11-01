package net.minecraft.world.phys.shapes;

import java.util.BitSet;
import net.minecraft.core.Direction;

public final class BitSetDiscreteVoxelShape extends DiscreteVoxelShape {
   private final BitSet storage;
   private int xMin;
   private int yMin;
   private int zMin;
   private int xMax;
   private int yMax;
   private int zMax;

   public BitSetDiscreteVoxelShape(int p_82588_, int p_82589_, int p_82590_) {
      super(p_82588_, p_82589_, p_82590_);
      this.storage = new BitSet(p_82588_ * p_82589_ * p_82590_);
      this.xMin = p_82588_;
      this.yMin = p_82589_;
      this.zMin = p_82590_;
   }

   public static BitSetDiscreteVoxelShape withFilledBounds(
      int p_165933_, int p_165934_, int p_165935_, int p_165936_, int p_165937_, int p_165938_, int p_165939_, int p_165940_, int p_165941_
   ) {
      BitSetDiscreteVoxelShape $$9 = new BitSetDiscreteVoxelShape(p_165933_, p_165934_, p_165935_);
      $$9.xMin = p_165936_;
      $$9.yMin = p_165937_;
      $$9.zMin = p_165938_;
      $$9.xMax = p_165939_;
      $$9.yMax = p_165940_;
      $$9.zMax = p_165941_;

      for (int $$10 = p_165936_; $$10 < p_165939_; $$10++) {
         for (int $$11 = p_165937_; $$11 < p_165940_; $$11++) {
            for (int $$12 = p_165938_; $$12 < p_165941_; $$12++) {
               $$9.fillUpdateBounds($$10, $$11, $$12, false);
            }
         }
      }

      return $$9;
   }

   public BitSetDiscreteVoxelShape(DiscreteVoxelShape p_82602_) {
      super(p_82602_.xSize, p_82602_.ySize, p_82602_.zSize);
      if (p_82602_ instanceof BitSetDiscreteVoxelShape) {
         this.storage = (BitSet)((BitSetDiscreteVoxelShape)p_82602_).storage.clone();
      } else {
         this.storage = new BitSet(this.xSize * this.ySize * this.zSize);

         for (int $$1 = 0; $$1 < this.xSize; $$1++) {
            for (int $$2 = 0; $$2 < this.ySize; $$2++) {
               for (int $$3 = 0; $$3 < this.zSize; $$3++) {
                  if (p_82602_.isFull($$1, $$2, $$3)) {
                     this.storage.set(this.getIndex($$1, $$2, $$3));
                  }
               }
            }
         }
      }

      this.xMin = p_82602_.firstFull(Direction.Axis.X);
      this.yMin = p_82602_.firstFull(Direction.Axis.Y);
      this.zMin = p_82602_.firstFull(Direction.Axis.Z);
      this.xMax = p_82602_.lastFull(Direction.Axis.X);
      this.yMax = p_82602_.lastFull(Direction.Axis.Y);
      this.zMax = p_82602_.lastFull(Direction.Axis.Z);
   }

   protected int getIndex(int p_82605_, int p_82606_, int p_82607_) {
      return (p_82605_ * this.ySize + p_82606_) * this.zSize + p_82607_;
   }

   @Override
   public boolean isFull(int p_82676_, int p_82677_, int p_82678_) {
      return this.storage.get(this.getIndex(p_82676_, p_82677_, p_82678_));
   }

   private void fillUpdateBounds(int p_165943_, int p_165944_, int p_165945_, boolean p_165946_) {
      this.storage.set(this.getIndex(p_165943_, p_165944_, p_165945_));
      if (p_165946_) {
         this.xMin = Math.min(this.xMin, p_165943_);
         this.yMin = Math.min(this.yMin, p_165944_);
         this.zMin = Math.min(this.zMin, p_165945_);
         this.xMax = Math.max(this.xMax, p_165943_ + 1);
         this.yMax = Math.max(this.yMax, p_165944_ + 1);
         this.zMax = Math.max(this.zMax, p_165945_ + 1);
      }
   }

   @Override
   public void fill(int p_165987_, int p_165988_, int p_165989_) {
      this.fillUpdateBounds(p_165987_, p_165988_, p_165989_, true);
   }

   @Override
   public boolean isEmpty() {
      return this.storage.isEmpty();
   }

   @Override
   public int firstFull(Direction.Axis p_82674_) {
      return p_82674_.choose(this.xMin, this.yMin, this.zMin);
   }

   @Override
   public int lastFull(Direction.Axis p_82680_) {
      return p_82680_.choose(this.xMax, this.yMax, this.zMax);
   }

   static BitSetDiscreteVoxelShape join(
      DiscreteVoxelShape p_82642_, DiscreteVoxelShape p_82643_, IndexMerger p_82644_, IndexMerger p_82645_, IndexMerger p_82646_, BooleanOp p_82647_
   ) {
      BitSetDiscreteVoxelShape $$6 = new BitSetDiscreteVoxelShape(p_82644_.size() - 1, p_82645_.size() - 1, p_82646_.size() - 1);
      int[] $$7 = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
      p_82644_.forMergedIndexes((p_82670_, p_82671_, p_82672_) -> {
         boolean[] $$10 = new boolean[]{false};
         p_82645_.forMergedIndexes((p_165978_, p_165979_, p_165980_) -> {
            boolean[] $$13 = new boolean[]{false};
            p_82646_.forMergedIndexes((p_165960_, p_165961_, p_165962_) -> {
               if (p_82647_.apply(p_82642_.isFullWide(p_82670_, p_165978_, p_165960_), p_82643_.isFullWide(p_82671_, p_165979_, p_165961_))) {
                  $$6.storage.set($$6.getIndex(p_82672_, p_165980_, p_165962_));
                  $$7[2] = Math.min($$7[2], p_165962_);
                  $$7[5] = Math.max($$7[5], p_165962_);
                  $$13[0] = true;
               }

               return true;
            });
            if ($$13[0]) {
               $$7[1] = Math.min($$7[1], p_165980_);
               $$7[4] = Math.max($$7[4], p_165980_);
               $$10[0] = true;
            }

            return true;
         });
         if ($$10[0]) {
            $$7[0] = Math.min($$7[0], p_82672_);
            $$7[3] = Math.max($$7[3], p_82672_);
         }

         return true;
      });
      $$6.xMin = $$7[0];
      $$6.yMin = $$7[1];
      $$6.zMin = $$7[2];
      $$6.xMax = $$7[3] + 1;
      $$6.yMax = $$7[4] + 1;
      $$6.zMax = $$7[5] + 1;
      return $$6;
   }

   protected static void forAllBoxes(DiscreteVoxelShape p_165964_, DiscreteVoxelShape.IntLineConsumer p_165965_, boolean p_165966_) {
      BitSetDiscreteVoxelShape $$3 = new BitSetDiscreteVoxelShape(p_165964_);

      for (int $$4 = 0; $$4 < $$3.ySize; $$4++) {
         for (int $$5 = 0; $$5 < $$3.xSize; $$5++) {
            int $$6 = -1;

            for (int $$7 = 0; $$7 <= $$3.zSize; $$7++) {
               if ($$3.isFullWide($$5, $$4, $$7)) {
                  if (p_165966_) {
                     if ($$6 == -1) {
                        $$6 = $$7;
                     }
                  } else {
                     p_165965_.consume($$5, $$4, $$7, $$5 + 1, $$4 + 1, $$7 + 1);
                  }
               } else if ($$6 != -1) {
                  int $$8 = $$5;
                  int $$9 = $$4;
                  $$3.clearZStrip($$6, $$7, $$5, $$4);

                  while ($$3.isZStripFull($$6, $$7, $$8 + 1, $$4)) {
                     $$3.clearZStrip($$6, $$7, $$8 + 1, $$4);
                     $$8++;
                  }

                  while ($$3.isXZRectangleFull($$5, $$8 + 1, $$6, $$7, $$9 + 1)) {
                     for (int $$10 = $$5; $$10 <= $$8; $$10++) {
                        $$3.clearZStrip($$6, $$7, $$10, $$9 + 1);
                     }

                     $$9++;
                  }

                  p_165965_.consume($$5, $$4, $$6, $$8 + 1, $$9 + 1, $$7);
                  $$6 = -1;
               }
            }
         }
      }
   }

   private boolean isZStripFull(int p_82609_, int p_82610_, int p_82611_, int p_82612_) {
      return p_82611_ < this.xSize && p_82612_ < this.ySize
         ? this.storage.nextClearBit(this.getIndex(p_82611_, p_82612_, p_82609_)) >= this.getIndex(p_82611_, p_82612_, p_82610_)
         : false;
   }

   private boolean isXZRectangleFull(int p_165927_, int p_165928_, int p_165929_, int p_165930_, int p_165931_) {
      for (int $$5 = p_165927_; $$5 < p_165928_; $$5++) {
         if (!this.isZStripFull(p_165929_, p_165930_, $$5, p_165931_)) {
            return false;
         }
      }

      return true;
   }

   private void clearZStrip(int p_165982_, int p_165983_, int p_165984_, int p_165985_) {
      this.storage.clear(this.getIndex(p_165984_, p_165985_, p_165982_), this.getIndex(p_165984_, p_165985_, p_165983_));
   }

   public boolean isInterior(int p_333985_, int p_333721_, int p_333973_) {
      boolean $$3 = p_333985_ > 0 && p_333985_ < this.xSize - 1 && p_333721_ > 0 && p_333721_ < this.ySize - 1 && p_333973_ > 0 && p_333973_ < this.zSize - 1;
      return $$3
         && this.isFull(p_333985_, p_333721_, p_333973_)
         && this.isFull(p_333985_ - 1, p_333721_, p_333973_)
         && this.isFull(p_333985_ + 1, p_333721_, p_333973_)
         && this.isFull(p_333985_, p_333721_ - 1, p_333973_)
         && this.isFull(p_333985_, p_333721_ + 1, p_333973_)
         && this.isFull(p_333985_, p_333721_, p_333973_ - 1)
         && this.isFull(p_333985_, p_333721_, p_333973_ + 1);
   }
}
