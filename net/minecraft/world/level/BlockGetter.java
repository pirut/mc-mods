package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface BlockGetter extends LevelHeightAccessor {
   @Nullable
   BlockEntity getBlockEntity(BlockPos pos);

   default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> blockEntityType) {
      BlockEntity blockEntity = this.getBlockEntity(pos);
      return blockEntity != null && blockEntity.getType() == blockEntityType ? Optional.of((T)blockEntity) : Optional.empty();
   }

   BlockState getBlockState(BlockPos pos);

   FluidState getFluidState(BlockPos pos);

   default int getLightEmission(BlockPos pos) {
      return this.getBlockState(pos).getLightEmission();
   }

   default Stream<BlockState> getBlockStates(AABB area) {
      return BlockPos.betweenClosedStream(area).map(this::getBlockState);
   }

   default BlockHitResult isBlockInLine(ClipBlockStateContext context) {
      return traverseBlocks(
         context.getFrom(),
         context.getTo(),
         context,
         (clipBlockStateContext, blockPos) -> {
            BlockState blockState = this.getBlockState(blockPos);
            Vec3 vec3 = clipBlockStateContext.getFrom().subtract(clipBlockStateContext.getTo());
            return clipBlockStateContext.isTargetBlock().test(blockState)
               ? new BlockHitResult(
                  clipBlockStateContext.getTo(),
                  Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z),
                  BlockPos.containing(clipBlockStateContext.getTo()),
                  false
               )
               : null;
         },
         clipBlockStateContext -> {
            Vec3 vec3 = clipBlockStateContext.getFrom().subtract(clipBlockStateContext.getTo());
            return BlockHitResult.miss(
               clipBlockStateContext.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(clipBlockStateContext.getTo())
            );
         }
      );
   }

   default BlockHitResult clip(ClipContext context) {
      return traverseBlocks(context.getFrom(), context.getTo(), context, (clipContext, blockPos) -> {
         BlockState blockState = this.getBlockState(blockPos);
         FluidState fluidState = this.getFluidState(blockPos);
         Vec3 vec3 = clipContext.getFrom();
         Vec3 vec32 = clipContext.getTo();
         VoxelShape voxelShape = clipContext.getBlockShape(blockState, this, blockPos);
         BlockHitResult blockHitResult = this.clipWithInteractionOverride(vec3, vec32, blockPos, voxelShape, blockState);
         VoxelShape voxelShape2 = clipContext.getFluidShape(fluidState, this, blockPos);
         BlockHitResult blockHitResult2 = voxelShape2.clip(vec3, vec32, blockPos);
         double d = blockHitResult == null ? Double.MAX_VALUE : clipContext.getFrom().distanceToSqr(blockHitResult.getLocation());
         double e = blockHitResult2 == null ? Double.MAX_VALUE : clipContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
         return d <= e ? blockHitResult : blockHitResult2;
      }, clipContext -> {
         Vec3 vec3 = clipContext.getFrom().subtract(clipContext.getTo());
         return BlockHitResult.miss(clipContext.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(clipContext.getTo()));
      });
   }

   @Nullable
   default BlockHitResult clipWithInteractionOverride(Vec3 startVec, Vec3 endVec, BlockPos pos, VoxelShape shape, BlockState state) {
      BlockHitResult blockHitResult = shape.clip(startVec, endVec, pos);
      if (blockHitResult != null) {
         BlockHitResult blockHitResult2 = state.getInteractionShape(this, pos).clip(startVec, endVec, pos);
         if (blockHitResult2 != null
            && blockHitResult2.getLocation().subtract(startVec).lengthSqr() < blockHitResult.getLocation().subtract(startVec).lengthSqr()) {
            return blockHitResult.withDirection(blockHitResult2.getDirection());
         }
      }

      return blockHitResult;
   }

   default double getBlockFloorHeight(VoxelShape shape, Supplier<VoxelShape> belowShapeSupplier) {
      if (!shape.isEmpty()) {
         return shape.max(Direction.Axis.Y);
      } else {
         double d = belowShapeSupplier.get().max(Direction.Axis.Y);
         return d >= 1.0 ? d - 1.0 : Double.NEGATIVE_INFINITY;
      }
   }

   default double getBlockFloorHeight(BlockPos pos) {
      return this.getBlockFloorHeight(this.getBlockState(pos).getCollisionShape(this, pos), () -> {
         BlockPos blockPos2 = pos.below();
         return this.getBlockState(blockPos2).getCollisionShape(this, blockPos2);
      });
   }

   static <T, C> T traverseBlocks(Vec3 from, Vec3 to, C context, BiFunction<C, BlockPos, T> tester, Function<C, T> onFail) {
      if (from.equals(to)) {
         return onFail.apply(context);
      } else {
         double d = Mth.lerp(-1.0E-7, to.x, from.x);
         double e = Mth.lerp(-1.0E-7, to.y, from.y);
         double f = Mth.lerp(-1.0E-7, to.z, from.z);
         double g = Mth.lerp(-1.0E-7, from.x, to.x);
         double h = Mth.lerp(-1.0E-7, from.y, to.y);
         double i = Mth.lerp(-1.0E-7, from.z, to.z);
         int j = Mth.floor(g);
         int k = Mth.floor(h);
         int l = Mth.floor(i);
         BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(j, k, l);
         T object = tester.apply(context, mutableBlockPos);
         if (object != null) {
            return object;
         } else {
            double m = d - g;
            double n = e - h;
            double o = f - i;
            int p = Mth.sign(m);
            int q = Mth.sign(n);
            int r = Mth.sign(o);
            double s = p == 0 ? Double.MAX_VALUE : p / m;
            double t = q == 0 ? Double.MAX_VALUE : q / n;
            double u = r == 0 ? Double.MAX_VALUE : r / o;
            double v = s * (p > 0 ? 1.0 - Mth.frac(g) : Mth.frac(g));
            double w = t * (q > 0 ? 1.0 - Mth.frac(h) : Mth.frac(h));
            double x = u * (r > 0 ? 1.0 - Mth.frac(i) : Mth.frac(i));

            while (v <= 1.0 || w <= 1.0 || x <= 1.0) {
               if (v < w) {
                  if (v < x) {
                     j += p;
                     v += s;
                  } else {
                     l += r;
                     x += u;
                  }
               } else if (w < x) {
                  k += q;
                  w += t;
               } else {
                  l += r;
                  x += u;
               }

               T object2 = tester.apply(context, mutableBlockPos.set(j, k, l));
               if (object2 != null) {
                  return object2;
               }
            }

            return onFail.apply(context);
         }
      }
   }

   static boolean forEachBlockIntersectedBetween(Vec3 from, Vec3 to, AABB boundingBox, BlockGetter.BlockStepVisitor visitor) {
      Vec3 vec3 = to.subtract(from);
      if (vec3.lengthSqr() < Mth.square(1.0E-5F)) {
         for (BlockPos blockPos : BlockPos.betweenClosed(boundingBox)) {
            if (!visitor.visit(blockPos, 0)) {
               return false;
            }
         }

         return true;
      } else {
         LongSet longSet = new LongOpenHashSet();

         for (BlockPos blockPos2 : BlockPos.betweenCornersInDirection(boundingBox.move(vec3.scale(-1.0)), vec3)) {
            if (!visitor.visit(blockPos2, 0)) {
               return false;
            }

            longSet.add(blockPos2.asLong());
         }

         int i = addCollisionsAlongTravel(longSet, vec3, boundingBox, visitor);
         if (i < 0) {
            return false;
         } else {
            for (BlockPos blockPos3 : BlockPos.betweenCornersInDirection(boundingBox, vec3)) {
               if (longSet.add(blockPos3.asLong()) && !visitor.visit(blockPos3, i + 1)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private static int addCollisionsAlongTravel(LongSet visited, Vec3 travelVector, AABB boundingBox, BlockGetter.BlockStepVisitor visitor) {
      double d = boundingBox.getXsize();
      double e = boundingBox.getYsize();
      double f = boundingBox.getZsize();
      Vec3i vec3i = getFurthestCorner(travelVector);
      Vec3 vec3 = boundingBox.getCenter();
      Vec3 vec32 = new Vec3(vec3.x() + d * 0.5 * vec3i.getX(), vec3.y() + e * 0.5 * vec3i.getY(), vec3.z() + f * 0.5 * vec3i.getZ());
      Vec3 vec33 = vec32.subtract(travelVector);
      int i = Mth.floor(vec33.x);
      int j = Mth.floor(vec33.y);
      int k = Mth.floor(vec33.z);
      int l = Mth.sign(travelVector.x);
      int m = Mth.sign(travelVector.y);
      int n = Mth.sign(travelVector.z);
      double g = l == 0 ? Double.MAX_VALUE : l / travelVector.x;
      double h = m == 0 ? Double.MAX_VALUE : m / travelVector.y;
      double o = n == 0 ? Double.MAX_VALUE : n / travelVector.z;
      double p = g * (l > 0 ? 1.0 - Mth.frac(vec33.x) : Mth.frac(vec33.x));
      double q = h * (m > 0 ? 1.0 - Mth.frac(vec33.y) : Mth.frac(vec33.y));
      double r = o * (n > 0 ? 1.0 - Mth.frac(vec33.z) : Mth.frac(vec33.z));
      int s = 0;

      while (p <= 1.0 || q <= 1.0 || r <= 1.0) {
         if (p < q) {
            if (p < r) {
               i += l;
               p += g;
            } else {
               k += n;
               r += o;
            }
         } else if (q < r) {
            j += m;
            q += h;
         } else {
            k += n;
            r += o;
         }

         Optional<Vec3> optional = AABB.clip(i, j, k, i + 1, j + 1, k + 1, vec33, vec32);
         if (!optional.isEmpty()) {
            s++;
            Vec3 vec34 = optional.get();
            double t = Mth.clamp(vec34.x, i + 1.0E-5F, i + 1.0 - 1.0E-5F);
            double u = Mth.clamp(vec34.y, j + 1.0E-5F, j + 1.0 - 1.0E-5F);
            double v = Mth.clamp(vec34.z, k + 1.0E-5F, k + 1.0 - 1.0E-5F);
            int w = Mth.floor(t - d * vec3i.getX());
            int x = Mth.floor(u - e * vec3i.getY());
            int y = Mth.floor(v - f * vec3i.getZ());
            int z = s;

            for (BlockPos blockPos : BlockPos.betweenCornersInDirection(i, j, k, w, x, y, travelVector)) {
               if (visited.add(blockPos.asLong()) && !visitor.visit(blockPos, z)) {
                  return -1;
               }
            }
         }
      }

      return s;
   }

   private static Vec3i getFurthestCorner(Vec3 travelVector) {
      double d = Math.abs(Vec3.X_AXIS.dot(travelVector));
      double e = Math.abs(Vec3.Y_AXIS.dot(travelVector));
      double f = Math.abs(Vec3.Z_AXIS.dot(travelVector));
      int i = travelVector.x >= 0.0 ? 1 : -1;
      int j = travelVector.y >= 0.0 ? 1 : -1;
      int k = travelVector.z >= 0.0 ? 1 : -1;
      if (d <= e && d <= f) {
         return new Vec3i(-i, -k, j);
      } else {
         return e <= f ? new Vec3i(k, -j, -i) : new Vec3i(-j, i, -k);
      }
   }

   @FunctionalInterface
   public interface BlockStepVisitor {
      boolean visit(BlockPos blockPos, int i);
   }
}
