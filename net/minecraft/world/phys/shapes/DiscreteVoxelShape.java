package net.minecraft.world.phys.shapes;

import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;

public abstract class DiscreteVoxelShape {
   private static final Direction.Axis[] AXIS_VALUES = Direction.Axis.values();
   protected final int xSize;
   protected final int ySize;
   protected final int zSize;

   protected DiscreteVoxelShape(int p_82787_, int p_82788_, int p_82789_) {
      if (p_82787_ >= 0 && p_82788_ >= 0 && p_82789_ >= 0) {
         this.xSize = p_82787_;
         this.ySize = p_82788_;
         this.zSize = p_82789_;
      } else {
         throw new IllegalArgumentException("Need all positive sizes: x: " + p_82787_ + ", y: " + p_82788_ + ", z: " + p_82789_);
      }
   }

   public boolean isFullWide(AxisCycle p_82823_, int p_82824_, int p_82825_, int p_82826_) {
      return this.isFullWide(
         p_82823_.cycle(p_82824_, p_82825_, p_82826_, Direction.Axis.X),
         p_82823_.cycle(p_82824_, p_82825_, p_82826_, Direction.Axis.Y),
         p_82823_.cycle(p_82824_, p_82825_, p_82826_, Direction.Axis.Z)
      );
   }

   public boolean isFullWide(int p_82847_, int p_82848_, int p_82849_) {
      if (p_82847_ < 0 || p_82848_ < 0 || p_82849_ < 0) {
         return false;
      } else {
         return p_82847_ < this.xSize && p_82848_ < this.ySize && p_82849_ < this.zSize ? this.isFull(p_82847_, p_82848_, p_82849_) : false;
      }
   }

   public boolean isFull(AxisCycle p_82836_, int p_82837_, int p_82838_, int p_82839_) {
      return this.isFull(
         p_82836_.cycle(p_82837_, p_82838_, p_82839_, Direction.Axis.X),
         p_82836_.cycle(p_82837_, p_82838_, p_82839_, Direction.Axis.Y),
         p_82836_.cycle(p_82837_, p_82838_, p_82839_, Direction.Axis.Z)
      );
   }

   public abstract boolean isFull(int var1, int var2, int var3);

   public abstract void fill(int var1, int var2, int var3);

   public boolean isEmpty() {
      for (Direction.Axis $$0 : AXIS_VALUES) {
         if (this.firstFull($$0) >= this.lastFull($$0)) {
            return true;
         }
      }

      return false;
   }

   public abstract int firstFull(Direction.Axis var1);

   public abstract int lastFull(Direction.Axis var1);

   public int firstFull(Direction.Axis p_165995_, int p_165996_, int p_165997_) {
      int $$3 = this.getSize(p_165995_);
      if (p_165996_ >= 0 && p_165997_ >= 0) {
         Direction.Axis $$4 = AxisCycle.FORWARD.cycle(p_165995_);
         Direction.Axis $$5 = AxisCycle.BACKWARD.cycle(p_165995_);
         if (p_165996_ < this.getSize($$4) && p_165997_ < this.getSize($$5)) {
            AxisCycle $$6 = AxisCycle.between(Direction.Axis.X, p_165995_);

            for (int $$7 = 0; $$7 < $$3; $$7++) {
               if (this.isFull($$6, $$7, p_165996_, p_165997_)) {
                  return $$7;
               }
            }

            return $$3;
         } else {
            return $$3;
         }
      } else {
         return $$3;
      }
   }

   public int lastFull(Direction.Axis p_82842_, int p_82843_, int p_82844_) {
      if (p_82843_ >= 0 && p_82844_ >= 0) {
         Direction.Axis $$3 = AxisCycle.FORWARD.cycle(p_82842_);
         Direction.Axis $$4 = AxisCycle.BACKWARD.cycle(p_82842_);
         if (p_82843_ < this.getSize($$3) && p_82844_ < this.getSize($$4)) {
            int $$5 = this.getSize(p_82842_);
            AxisCycle $$6 = AxisCycle.between(Direction.Axis.X, p_82842_);

            for (int $$7 = $$5 - 1; $$7 >= 0; $$7--) {
               if (this.isFull($$6, $$7, p_82843_, p_82844_)) {
                  return $$7 + 1;
               }
            }

            return 0;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public int getSize(Direction.Axis p_82851_) {
      return p_82851_.choose(this.xSize, this.ySize, this.zSize);
   }

   public int getXSize() {
      return this.getSize(Direction.Axis.X);
   }

   public int getYSize() {
      return this.getSize(Direction.Axis.Y);
   }

   public int getZSize() {
      return this.getSize(Direction.Axis.Z);
   }

   public void forAllEdges(DiscreteVoxelShape.IntLineConsumer p_82820_, boolean p_82821_) {
      this.forAllAxisEdges(p_82820_, AxisCycle.NONE, p_82821_);
      this.forAllAxisEdges(p_82820_, AxisCycle.FORWARD, p_82821_);
      this.forAllAxisEdges(p_82820_, AxisCycle.BACKWARD, p_82821_);
   }

   private void forAllAxisEdges(DiscreteVoxelShape.IntLineConsumer p_82816_, AxisCycle p_82817_, boolean p_82818_) {
      AxisCycle $$3 = p_82817_.inverse();
      int $$4 = this.getSize($$3.cycle(Direction.Axis.X));
      int $$5 = this.getSize($$3.cycle(Direction.Axis.Y));
      int $$6 = this.getSize($$3.cycle(Direction.Axis.Z));

      for (int $$7 = 0; $$7 <= $$4; $$7++) {
         for (int $$8 = 0; $$8 <= $$5; $$8++) {
            int $$9 = -1;

            for (int $$10 = 0; $$10 <= $$6; $$10++) {
               int $$11 = 0;
               int $$12 = 0;

               for (int $$13 = 0; $$13 <= 1; $$13++) {
                  for (int $$14 = 0; $$14 <= 1; $$14++) {
                     if (this.isFullWide($$3, $$7 + $$13 - 1, $$8 + $$14 - 1, $$10)) {
                        $$11++;
                        $$12 ^= $$13 ^ $$14;
                     }
                  }
               }

               if ($$11 == 1 || $$11 == 3 || $$11 == 2 && ($$12 & 1) == 0) {
                  if (p_82818_) {
                     if ($$9 == -1) {
                        $$9 = $$10;
                     }
                  } else {
                     p_82816_.consume(
                        $$3.cycle($$7, $$8, $$10, Direction.Axis.X),
                        $$3.cycle($$7, $$8, $$10, Direction.Axis.Y),
                        $$3.cycle($$7, $$8, $$10, Direction.Axis.Z),
                        $$3.cycle($$7, $$8, $$10 + 1, Direction.Axis.X),
                        $$3.cycle($$7, $$8, $$10 + 1, Direction.Axis.Y),
                        $$3.cycle($$7, $$8, $$10 + 1, Direction.Axis.Z)
                     );
                  }
               } else if ($$9 != -1) {
                  p_82816_.consume(
                     $$3.cycle($$7, $$8, $$9, Direction.Axis.X),
                     $$3.cycle($$7, $$8, $$9, Direction.Axis.Y),
                     $$3.cycle($$7, $$8, $$9, Direction.Axis.Z),
                     $$3.cycle($$7, $$8, $$10, Direction.Axis.X),
                     $$3.cycle($$7, $$8, $$10, Direction.Axis.Y),
                     $$3.cycle($$7, $$8, $$10, Direction.Axis.Z)
                  );
                  $$9 = -1;
               }
            }
         }
      }
   }

   public void forAllBoxes(DiscreteVoxelShape.IntLineConsumer p_82833_, boolean p_82834_) {
      BitSetDiscreteVoxelShape.forAllBoxes(this, p_82833_, p_82834_);
   }

   public void forAllFaces(DiscreteVoxelShape.IntFaceConsumer p_82811_) {
      this.forAllAxisFaces(p_82811_, AxisCycle.NONE);
      this.forAllAxisFaces(p_82811_, AxisCycle.FORWARD);
      this.forAllAxisFaces(p_82811_, AxisCycle.BACKWARD);
   }

   private void forAllAxisFaces(DiscreteVoxelShape.IntFaceConsumer p_82813_, AxisCycle p_82814_) {
      AxisCycle $$2 = p_82814_.inverse();
      Direction.Axis $$3 = $$2.cycle(Direction.Axis.Z);
      int $$4 = this.getSize($$2.cycle(Direction.Axis.X));
      int $$5 = this.getSize($$2.cycle(Direction.Axis.Y));
      int $$6 = this.getSize($$3);
      Direction $$7 = Direction.fromAxisAndDirection($$3, Direction.AxisDirection.NEGATIVE);
      Direction $$8 = Direction.fromAxisAndDirection($$3, Direction.AxisDirection.POSITIVE);

      for (int $$9 = 0; $$9 < $$4; $$9++) {
         for (int $$10 = 0; $$10 < $$5; $$10++) {
            boolean $$11 = false;

            for (int $$12 = 0; $$12 <= $$6; $$12++) {
               boolean $$13 = $$12 != $$6 && this.isFull($$2, $$9, $$10, $$12);
               if (!$$11 && $$13) {
                  p_82813_.consume(
                     $$7,
                     $$2.cycle($$9, $$10, $$12, Direction.Axis.X),
                     $$2.cycle($$9, $$10, $$12, Direction.Axis.Y),
                     $$2.cycle($$9, $$10, $$12, Direction.Axis.Z)
                  );
               }

               if ($$11 && !$$13) {
                  p_82813_.consume(
                     $$8,
                     $$2.cycle($$9, $$10, $$12 - 1, Direction.Axis.X),
                     $$2.cycle($$9, $$10, $$12 - 1, Direction.Axis.Y),
                     $$2.cycle($$9, $$10, $$12 - 1, Direction.Axis.Z)
                  );
               }

               $$11 = $$13;
            }
         }
      }
   }

   public interface IntFaceConsumer {
      void consume(Direction var1, int var2, int var3, int var4);
   }

   public interface IntLineConsumer {
      void consume(int var1, int var2, int var3, int var4, int var5, int var6);
   }
}
