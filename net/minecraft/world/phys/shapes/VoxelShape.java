package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public abstract class VoxelShape {
   protected final DiscreteVoxelShape shape;
   @Nullable
   private VoxelShape[] faces;

   protected VoxelShape(DiscreteVoxelShape shape) {
      this.shape = shape;
   }

   public double min(Direction.Axis axis) {
      int i = this.shape.firstFull(axis);
      return i >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, i);
   }

   public double max(Direction.Axis axis) {
      int i = this.shape.lastFull(axis);
      return i <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, i);
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

   protected double get(Direction.Axis axis, int index) {
      return this.getCoords(axis).getDouble(index);
   }

   public abstract DoubleList getCoords(Direction.Axis axis);

   public boolean isEmpty() {
      return this.shape.isEmpty();
   }

   public VoxelShape move(Vec3 offset) {
      return this.move(offset.x, offset.y, offset.z);
   }

   public VoxelShape move(Vec3i offset) {
      return this.move(offset.getX(), offset.getY(), offset.getZ());
   }

   public VoxelShape move(double xOffset, double yOffset, double zOffset) {
      return (VoxelShape)(this.isEmpty()
         ? Shapes.empty()
         : new ArrayVoxelShape(
            this.shape,
            new OffsetDoubleList(this.getCoords(Direction.Axis.X), xOffset),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Y), yOffset),
            new OffsetDoubleList(this.getCoords(Direction.Axis.Z), zOffset)
         ));
   }

   public VoxelShape optimize() {
      VoxelShape[] voxelShapes = new VoxelShape[]{Shapes.empty()};
      this.forAllBoxes((d, e, f, g, h, i) -> voxelShapes[0] = Shapes.joinUnoptimized(voxelShapes[0], Shapes.box(d, e, f, g, h, i), BooleanOp.OR));
      return voxelShapes[0];
   }

   public void forAllEdges(Shapes.DoubleLineConsumer action) {
      this.shape
         .forAllEdges(
            (i, j, k, l, m, n) -> action.consume(
               this.get(Direction.Axis.X, i),
               this.get(Direction.Axis.Y, j),
               this.get(Direction.Axis.Z, k),
               this.get(Direction.Axis.X, l),
               this.get(Direction.Axis.Y, m),
               this.get(Direction.Axis.Z, n)
            ),
            true
         );
   }

   public void forAllBoxes(Shapes.DoubleLineConsumer action) {
      DoubleList doubleList = this.getCoords(Direction.Axis.X);
      DoubleList doubleList2 = this.getCoords(Direction.Axis.Y);
      DoubleList doubleList3 = this.getCoords(Direction.Axis.Z);
      this.shape
         .forAllBoxes(
            (i, j, k, l, m, n) -> action.consume(
               doubleList.getDouble(i),
               doubleList2.getDouble(j),
               doubleList3.getDouble(k),
               doubleList.getDouble(l),
               doubleList2.getDouble(m),
               doubleList3.getDouble(n)
            ),
            true
         );
   }

   public List<AABB> toAabbs() {
      List<AABB> list = Lists.newArrayList();
      this.forAllBoxes((d, e, f, g, h, i) -> list.add(new AABB(d, e, f, g, h, i)));
      return list;
   }

   public double min(Direction.Axis axis, double primaryPosition, double secondaryPosition) {
      Direction.Axis axis2 = AxisCycle.FORWARD.cycle(axis);
      Direction.Axis axis3 = AxisCycle.BACKWARD.cycle(axis);
      int i = this.findIndex(axis2, primaryPosition);
      int j = this.findIndex(axis3, secondaryPosition);
      int k = this.shape.firstFull(axis, i, j);
      return k >= this.shape.getSize(axis) ? Double.POSITIVE_INFINITY : this.get(axis, k);
   }

   public double max(Direction.Axis axis, double primaryPosition, double secondaryPosition) {
      Direction.Axis axis2 = AxisCycle.FORWARD.cycle(axis);
      Direction.Axis axis3 = AxisCycle.BACKWARD.cycle(axis);
      int i = this.findIndex(axis2, primaryPosition);
      int j = this.findIndex(axis3, secondaryPosition);
      int k = this.shape.lastFull(axis, i, j);
      return k <= 0 ? Double.NEGATIVE_INFINITY : this.get(axis, k);
   }

   protected int findIndex(Direction.Axis axis, double position) {
      return Mth.binarySearch(0, this.shape.getSize(axis) + 1, i -> position < this.get(axis, i)) - 1;
   }

   @Nullable
   public BlockHitResult clip(Vec3 startVec, Vec3 endVec, BlockPos pos) {
      if (this.isEmpty()) {
         return null;
      } else {
         Vec3 vec3 = endVec.subtract(startVec);
         if (vec3.lengthSqr() < 1.0E-7) {
            return null;
         } else {
            Vec3 vec32 = startVec.add(vec3.scale(0.001));
            return this.shape
                  .isFullWide(
                     this.findIndex(Direction.Axis.X, vec32.x - pos.getX()),
                     this.findIndex(Direction.Axis.Y, vec32.y - pos.getY()),
                     this.findIndex(Direction.Axis.Z, vec32.z - pos.getZ())
                  )
               ? new BlockHitResult(vec32, Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z).getOpposite(), pos, true)
               : AABB.clip(this.toAabbs(), startVec, endVec, pos);
         }
      }
   }

   public Optional<Vec3> closestPointTo(Vec3 point) {
      if (this.isEmpty()) {
         return Optional.empty();
      } else {
         MutableObject<Vec3> mutableObject = new MutableObject();
         this.forAllBoxes((d, e, f, g, h, i) -> {
            double j = Mth.clamp(point.x(), d, g);
            double k = Mth.clamp(point.y(), e, h);
            double l = Mth.clamp(point.z(), f, i);
            Vec3 vec32 = (Vec3)mutableObject.get();
            if (vec32 == null || point.distanceToSqr(j, k, l) < point.distanceToSqr(vec32)) {
               mutableObject.setValue(new Vec3(j, k, l));
            }
         });
         return Optional.of(Objects.requireNonNull((Vec3)mutableObject.get()));
      }
   }

   public VoxelShape getFaceShape(Direction side) {
      if (!this.isEmpty() && this != Shapes.block()) {
         if (this.faces != null) {
            VoxelShape voxelShape = this.faces[side.ordinal()];
            if (voxelShape != null) {
               return voxelShape;
            }
         } else {
            this.faces = new VoxelShape[6];
         }

         VoxelShape voxelShape = this.calculateFace(side);
         this.faces[side.ordinal()] = voxelShape;
         return voxelShape;
      } else {
         return this;
      }
   }

   private VoxelShape calculateFace(Direction side) {
      Direction.Axis axis = side.getAxis();
      if (this.isCubeLikeAlong(axis)) {
         return this;
      } else {
         Direction.AxisDirection axisDirection = side.getAxisDirection();
         int i = this.findIndex(axis, axisDirection == Direction.AxisDirection.POSITIVE ? 0.9999999 : 1.0E-7);
         SliceShape sliceShape = new SliceShape(this, axis, i);
         if (sliceShape.isEmpty()) {
            return Shapes.empty();
         } else {
            return (VoxelShape)(sliceShape.isCubeLike() ? Shapes.block() : sliceShape);
         }
      }
   }

   protected boolean isCubeLike() {
      for (Direction.Axis axis : Direction.Axis.VALUES) {
         if (!this.isCubeLikeAlong(axis)) {
            return false;
         }
      }

      return true;
   }

   private boolean isCubeLikeAlong(Direction.Axis axis) {
      DoubleList doubleList = this.getCoords(axis);
      return doubleList.size() == 2
         && DoubleMath.fuzzyEquals(doubleList.getDouble(0), 0.0, 1.0E-7)
         && DoubleMath.fuzzyEquals(doubleList.getDouble(1), 1.0, 1.0E-7);
   }

   public double collide(Direction.Axis movementAxis, AABB collisionBox, double desiredOffset) {
      return this.collideX(AxisCycle.between(movementAxis, Direction.Axis.X), collisionBox, desiredOffset);
   }

   protected double collideX(AxisCycle movementAxis, AABB collisionBox, double desiredOffset) {
      if (this.isEmpty()) {
         return desiredOffset;
      } else if (Math.abs(desiredOffset) < 1.0E-7) {
         return 0.0;
      } else {
         AxisCycle axisCycle = movementAxis.inverse();
         Direction.Axis axis = axisCycle.cycle(Direction.Axis.X);
         Direction.Axis axis2 = axisCycle.cycle(Direction.Axis.Y);
         Direction.Axis axis3 = axisCycle.cycle(Direction.Axis.Z);
         double d = collisionBox.max(axis);
         double e = collisionBox.min(axis);
         int i = this.findIndex(axis, e + 1.0E-7);
         int j = this.findIndex(axis, d - 1.0E-7);
         int k = Math.max(0, this.findIndex(axis2, collisionBox.min(axis2) + 1.0E-7));
         int l = Math.min(this.shape.getSize(axis2), this.findIndex(axis2, collisionBox.max(axis2) - 1.0E-7) + 1);
         int m = Math.max(0, this.findIndex(axis3, collisionBox.min(axis3) + 1.0E-7));
         int n = Math.min(this.shape.getSize(axis3), this.findIndex(axis3, collisionBox.max(axis3) - 1.0E-7) + 1);
         int o = this.shape.getSize(axis);
         if (desiredOffset > 0.0) {
            for (int p = j + 1; p < o; p++) {
               for (int q = k; q < l; q++) {
                  for (int r = m; r < n; r++) {
                     if (this.shape.isFullWide(axisCycle, p, q, r)) {
                        double f = this.get(axis, p) - d;
                        if (f >= -1.0E-7) {
                           desiredOffset = Math.min(desiredOffset, f);
                        }

                        return desiredOffset;
                     }
                  }
               }
            }
         } else if (desiredOffset < 0.0) {
            for (int p = i - 1; p >= 0; p--) {
               for (int q = k; q < l; q++) {
                  for (int rx = m; rx < n; rx++) {
                     if (this.shape.isFullWide(axisCycle, p, q, rx)) {
                        double f = this.get(axis, p + 1) - e;
                        if (f <= 1.0E-7) {
                           desiredOffset = Math.max(desiredOffset, f);
                        }

                        return desiredOffset;
                     }
                  }
               }
            }
         }

         return desiredOffset;
      }
   }

   @Override
   public boolean equals(Object object) {
      return super.equals(object);
   }

   @Override
   public String toString() {
      return this.isEmpty() ? "EMPTY" : "VoxelShape[" + this.bounds() + "]";
   }
}
