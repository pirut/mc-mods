package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class VoxelShape {
   protected final DiscreteVoxelShape shape;
   @Nullable
   private VoxelShape[] faces;

   protected VoxelShape(DiscreteVoxelShape p_83214_) {
      this.shape = p_83214_;
   }

   public double min(Direction.Axis p_83289_) {
      int $$1 = this.shape.firstFull(p_83289_);
      return $$1 >= this.shape.getSize(p_83289_) ? Double.POSITIVE_INFINITY : this.get(p_83289_, $$1);
   }

   public double max(Direction.Axis p_83298_) {
      int $$1 = this.shape.lastFull(p_83298_);
      return $$1 <= 0 ? Double.NEGATIVE_INFINITY : this.get(p_83298_, $$1);
   }

   public AABB bounds() {
      if (this.isEmpty()) {
         throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("No bounds for empty shape."));
      } else {
         return new AABB(
            this.min(Direction.Axis.X),
            this.min(Direction.Axis.Y),
            this.min(Direction.Axis.Z),
            this.max(Direction.Axis.X),
            this.max(Direction.Axis.Y),
            this.max(Direction.Axis.Z)
         );
      }
   }

   public VoxelShape singleEncompassing() {
      return this.isEmpty()
         ? Shapes.empty()
         : Shapes.box(
            this.min(Direction.Axis.X),
            this.min(Direction.Axis.Y),
            this.min(Direction.Axis.Z),
            this.max(Direction.Axis.X),
            this.max(Direction.Axis.Y),
            this.max(Direction.Axis.Z)
         );
   }

   protected double get(Direction.Axis p_83257_, int p_83258_) {
      return this.getCoords(p_83257_).getDouble(p_83258_);
   }

   public abstract DoubleList getCoords(Direction.Axis var1);

   public boolean isEmpty() {
      return this.shape.isEmpty();
   }

   public VoxelShape move(double p_83217_, double p_83218_, double p_83219_) {
      return (VoxelShape)(this.isEmpty()
         ? Shapes.empty()
         : new ArrayVoxelShape(
            this.shape,
            new OffsetDoubleList(this.getCoords(Direction.Axis.X), p_83217_),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Y), p_83218_),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Z), p_83219_)
         ));
   }

   public VoxelShape optimize() {
      VoxelShape[] $$0 = new VoxelShape[]{Shapes.empty()};
      this.forAllBoxes(
         (p_83275_, p_83276_, p_83277_, p_83278_, p_83279_, p_83280_) -> $$0[0] = Shapes.joinUnoptimized(
            $$0[0], Shapes.box(p_83275_, p_83276_, p_83277_, p_83278_, p_83279_, p_83280_), BooleanOp.OR
         )
      );
      return $$0[0];
   }

   public void forAllEdges(Shapes.DoubleLineConsumer p_83225_) {
      this.shape
         .forAllEdges(
            (p_83228_, p_83229_, p_83230_, p_83231_, p_83232_, p_83233_) -> p_83225_.consume(
               this.get(Direction.Axis.X, p_83228_),
               this.get(Direction.Axis.Y, p_83229_),
               this.get(Direction.Axis.Z, p_83230_),
               this.get(Direction.Axis.X, p_83231_),
               this.get(Direction.Axis.Y, p_83232_),
               this.get(Direction.Axis.Z, p_83233_)
            ),
            true
         );
   }

   public void forAllBoxes(Shapes.DoubleLineConsumer p_83287_) {
      DoubleList $$1 = this.getCoords(Direction.Axis.X);
      DoubleList $$2 = this.getCoords(Direction.Axis.Y);
      DoubleList $$3 = this.getCoords(Direction.Axis.Z);
      this.shape
         .forAllBoxes(
            (p_83239_, p_83240_, p_83241_, p_83242_, p_83243_, p_83244_) -> p_83287_.consume(
               $$1.getDouble(p_83239_),
               $$2.getDouble(p_83240_),
               $$3.getDouble(p_83241_),
               $$1.getDouble(p_83242_),
               $$2.getDouble(p_83243_),
               $$3.getDouble(p_83244_)
            ),
            true
         );
   }

   public List<AABB> toAabbs() {
      List<AABB> $$0 = Lists.newArrayList();
      this.forAllBoxes(
         (p_83267_, p_83268_, p_83269_, p_83270_, p_83271_, p_83272_) -> $$0.add(new AABB(p_83267_, p_83268_, p_83269_, p_83270_, p_83271_, p_83272_))
      );
      return $$0;
   }

   public double min(Direction.Axis p_166079_, double p_166080_, double p_166081_) {
      Direction.Axis $$3 = AxisCycle.FORWARD.cycle(p_166079_);
      Direction.Axis $$4 = AxisCycle.BACKWARD.cycle(p_166079_);
      int $$5 = this.findIndex($$3, p_166080_);
      int $$6 = this.findIndex($$4, p_166081_);
      int $$7 = this.shape.firstFull(p_166079_, $$5, $$6);
      return $$7 >= this.shape.getSize(p_166079_) ? Double.POSITIVE_INFINITY : this.get(p_166079_, $$7);
   }

   public double max(Direction.Axis p_83291_, double p_83292_, double p_83293_) {
      Direction.Axis $$3 = AxisCycle.FORWARD.cycle(p_83291_);
      Direction.Axis $$4 = AxisCycle.BACKWARD.cycle(p_83291_);
      int $$5 = this.findIndex($$3, p_83292_);
      int $$6 = this.findIndex($$4, p_83293_);
      int $$7 = this.shape.lastFull(p_83291_, $$5, $$6);
      return $$7 <= 0 ? Double.NEGATIVE_INFINITY : this.get(p_83291_, $$7);
   }

   protected int findIndex(Direction.Axis p_83250_, double p_83251_) {
      return Mth.binarySearch(0, this.shape.getSize(p_83250_) + 1, p_166066_ -> p_83251_ < this.get(p_83250_, p_166066_)) - 1;
   }

   @Nullable
   public BlockHitResult clip(Vec3 p_83221_, Vec3 p_83222_, BlockPos p_83223_) {
      if (this.isEmpty()) {
         return null;
      } else {
         Vec3 $$3 = p_83222_.subtract(p_83221_);
         if ($$3.lengthSqr() < 1.0E-7) {
            return null;
         } else {
            Vec3 $$4 = p_83221_.add($$3.scale(0.001));
            return this.shape
                  .isFullWide(
                     this.findIndex(Direction.Axis.X, $$4.x - p_83223_.getX()),
                     this.findIndex(Direction.Axis.Y, $$4.y - p_83223_.getY()),
                     this.findIndex(Direction.Axis.Z, $$4.z - p_83223_.getZ())
                  )
               ? new BlockHitResult($$4, Direction.getNearest($$3.x, $$3.y, $$3.z).getOpposite(), p_83223_, true)
               : AABB.clip(this.toAabbs(), p_83221_, p_83222_, p_83223_);
         }
      }
   }

   public Optional<Vec3> closestPointTo(Vec3 p_166068_) {
      if (this.isEmpty()) {
         return Optional.empty();
      } else {
         Vec3[] $$1 = new Vec3[1];
         this.forAllBoxes((p_166072_, p_166073_, p_166074_, p_166075_, p_166076_, p_166077_) -> {
            double $$8 = Mth.clamp(p_166068_.x(), p_166072_, p_166075_);
            double $$9 = Mth.clamp(p_166068_.y(), p_166073_, p_166076_);
            double $$10 = Mth.clamp(p_166068_.z(), p_166074_, p_166077_);
            if ($$1[0] == null || p_166068_.distanceToSqr($$8, $$9, $$10) < p_166068_.distanceToSqr($$1[0])) {
               $$1[0] = new Vec3($$8, $$9, $$10);
            }
         });
         return Optional.of($$1[0]);
      }
   }

   public VoxelShape getFaceShape(Direction p_83264_) {
      if (!this.isEmpty() && this != Shapes.block()) {
         if (this.faces != null) {
            VoxelShape $$1 = this.faces[p_83264_.ordinal()];
            if ($$1 != null) {
               return $$1;
            }
         } else {
            this.faces = new VoxelShape[6];
         }

         VoxelShape $$2 = this.calculateFace(p_83264_);
         this.faces[p_83264_.ordinal()] = $$2;
         return $$2;
      } else {
         return this;
      }
   }

   private VoxelShape calculateFace(Direction p_83295_) {
      Direction.Axis $$1 = p_83295_.getAxis();
      DoubleList $$2 = this.getCoords($$1);
      if ($$2.size() == 2 && DoubleMath.fuzzyEquals($$2.getDouble(0), 0.0, 1.0E-7) && DoubleMath.fuzzyEquals($$2.getDouble(1), 1.0, 1.0E-7)) {
         return this;
      } else {
         Direction.AxisDirection $$3 = p_83295_.getAxisDirection();
         int $$4 = this.findIndex($$1, $$3 == Direction.AxisDirection.POSITIVE ? 0.9999999 : 1.0E-7);
         return new SliceShape(this, $$1, $$4);
      }
   }

   public double collide(Direction.Axis p_83260_, AABB p_83261_, double p_83262_) {
      return this.collideX(AxisCycle.between(p_83260_, Direction.Axis.X), p_83261_, p_83262_);
   }

   protected double collideX(AxisCycle p_83246_, AABB p_83247_, double p_83248_) {
      if (this.isEmpty()) {
         return p_83248_;
      } else if (Math.abs(p_83248_) < 1.0E-7) {
         return 0.0;
      } else {
         AxisCycle $$3 = p_83246_.inverse();
         Direction.Axis $$4 = $$3.cycle(Direction.Axis.X);
         Direction.Axis $$5 = $$3.cycle(Direction.Axis.Y);
         Direction.Axis $$6 = $$3.cycle(Direction.Axis.Z);
         double $$7 = p_83247_.max($$4);
         double $$8 = p_83247_.min($$4);
         int $$9 = this.findIndex($$4, $$8 + 1.0E-7);
         int $$10 = this.findIndex($$4, $$7 - 1.0E-7);
         int $$11 = Math.max(0, this.findIndex($$5, p_83247_.min($$5) + 1.0E-7));
         int $$12 = Math.min(this.shape.getSize($$5), this.findIndex($$5, p_83247_.max($$5) - 1.0E-7) + 1);
         int $$13 = Math.max(0, this.findIndex($$6, p_83247_.min($$6) + 1.0E-7));
         int $$14 = Math.min(this.shape.getSize($$6), this.findIndex($$6, p_83247_.max($$6) - 1.0E-7) + 1);
         int $$15 = this.shape.getSize($$4);
         if (p_83248_ > 0.0) {
            for (int $$16 = $$10 + 1; $$16 < $$15; $$16++) {
               for (int $$17 = $$11; $$17 < $$12; $$17++) {
                  for (int $$18 = $$13; $$18 < $$14; $$18++) {
                     if (this.shape.isFullWide($$3, $$16, $$17, $$18)) {
                        double $$19 = this.get($$4, $$16) - $$7;
                        if ($$19 >= -1.0E-7) {
                           p_83248_ = Math.min(p_83248_, $$19);
                        }

                        return p_83248_;
                     }
                  }
               }
            }
         } else if (p_83248_ < 0.0) {
            for (int $$20 = $$9 - 1; $$20 >= 0; $$20--) {
               for (int $$21 = $$11; $$21 < $$12; $$21++) {
                  for (int $$22 = $$13; $$22 < $$14; $$22++) {
                     if (this.shape.isFullWide($$3, $$20, $$21, $$22)) {
                        double $$23 = this.get($$4, $$20 + 1) - $$8;
                        if ($$23 <= 1.0E-7) {
                           p_83248_ = Math.max(p_83248_, $$23);
                        }

                        return p_83248_;
                     }
                  }
               }
            }
         }

         return p_83248_;
      }
   }

   @Override
   public String toString() {
      return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.bounds() + "]";
   }
}
