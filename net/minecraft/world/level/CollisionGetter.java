package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface CollisionGetter extends BlockGetter {
   WorldBorder getWorldBorder();

   @Nullable
   BlockGetter getChunkForCollisions(int chunkX, int chunkZ);

   default boolean isUnobstructed(@Nullable Entity entity, VoxelShape shape) {
      return true;
   }

   default boolean isUnobstructed(BlockState state, BlockPos pos, CollisionContext context) {
      VoxelShape voxelShape = state.getCollisionShape(this, pos, context);
      return voxelShape.isEmpty() || this.isUnobstructed(null, voxelShape.move(pos));
   }

   default boolean isUnobstructed(Entity entity) {
      return this.isUnobstructed(entity, Shapes.create(entity.getBoundingBox()));
   }

   default boolean noCollision(AABB collisionBox) {
      return this.noCollision(null, collisionBox);
   }

   default boolean noCollision(Entity entity) {
      return this.noCollision(entity, entity.getBoundingBox());
   }

   default boolean noCollision(@Nullable Entity entity, AABB collisionBox) {
      return this.noCollision(entity, collisionBox, false);
   }

   default boolean noCollision(@Nullable Entity entity, AABB collisionBox, boolean checkLiquid) {
      return this.noBlockCollision(entity, collisionBox, checkLiquid)
         && this.noEntityCollision(entity, collisionBox)
         && this.noBorderCollision(entity, collisionBox);
   }

   default boolean noBlockCollision(@Nullable Entity entity, AABB boundingBox) {
      return this.noBlockCollision(entity, boundingBox, false);
   }

   default boolean noBlockCollision(@Nullable Entity entity, AABB boundingBox, boolean checkLiquid) {
      for (VoxelShape voxelShape : checkLiquid ? this.getBlockAndLiquidCollisions(entity, boundingBox) : this.getBlockCollisions(entity, boundingBox)) {
         if (!voxelShape.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   default boolean noEntityCollision(@Nullable Entity entity, AABB boundIngBox) {
      return this.getEntityCollisions(entity, boundIngBox).isEmpty();
   }

   default boolean noBorderCollision(@Nullable Entity entity, AABB boundingBox) {
      if (entity == null) {
         return true;
      } else {
         VoxelShape voxelShape = this.borderCollision(entity, boundingBox);
         return voxelShape == null || !Shapes.joinIsNotEmpty(voxelShape, Shapes.create(boundingBox), BooleanOp.AND);
      }
   }

   List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB collisionBox);

   default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, AABB collisionBox) {
      List<VoxelShape> list = this.getEntityCollisions(entity, collisionBox);
      Iterable<VoxelShape> iterable = this.getBlockCollisions(entity, collisionBox);
      return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
   }

   default Iterable<VoxelShape> getPreMoveCollisions(@Nullable Entity entity, AABB collisionBox, Vec3 pos) {
      List<VoxelShape> list = this.getEntityCollisions(entity, collisionBox);
      Iterable<VoxelShape> iterable = this.getBlockCollisionsFromContext(CollisionContext.withPosition(entity, pos.y), collisionBox);
      return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
   }

   default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AABB collisionBox) {
      return this.getBlockCollisionsFromContext(entity == null ? CollisionContext.empty() : CollisionContext.of(entity), collisionBox);
   }

   default Iterable<VoxelShape> getBlockAndLiquidCollisions(@Nullable Entity entity, AABB collisionBox) {
      return this.getBlockCollisionsFromContext(entity == null ? CollisionContext.emptyWithFluidCollisions() : CollisionContext.of(entity, true), collisionBox);
   }

   private Iterable<VoxelShape> getBlockCollisionsFromContext(CollisionContext context, AABB collisionBox) {
      return () -> new BlockCollisions<VoxelShape>(this, context, collisionBox, false, (mutableBlockPos, voxelShape) -> voxelShape);
   }

   @Nullable
   private VoxelShape borderCollision(Entity entity, AABB box) {
      WorldBorder worldBorder = this.getWorldBorder();
      return worldBorder.isInsideCloseToBorder(entity, box) ? worldBorder.getCollisionShape() : null;
   }

   default BlockHitResult clipIncludingBorder(ClipContext clipContext) {
      BlockHitResult blockHitResult = this.clip(clipContext);
      WorldBorder worldBorder = this.getWorldBorder();
      if (worldBorder.isWithinBounds(clipContext.getFrom()) && !worldBorder.isWithinBounds(blockHitResult.getLocation())) {
         Vec3 vec3 = blockHitResult.getLocation().subtract(clipContext.getFrom());
         Direction direction = Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z);
         Vec3 vec32 = worldBorder.clampVec3ToBound(blockHitResult.getLocation());
         return new BlockHitResult(vec32, direction, BlockPos.containing(vec32), false, true);
      } else {
         return blockHitResult;
      }
   }

   default boolean collidesWithSuffocatingBlock(@Nullable Entity entity, AABB box) {
      BlockCollisions<VoxelShape> blockCollisions = new BlockCollisions<>(this, entity, box, true, (mutableBlockPos, voxelShape) -> voxelShape);

      while (blockCollisions.hasNext()) {
         if (!((VoxelShape)blockCollisions.next()).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   default Optional<BlockPos> findSupportingBlock(Entity entity, AABB box) {
      BlockPos blockPos = null;
      double d = Double.MAX_VALUE;
      BlockCollisions<BlockPos> blockCollisions = new BlockCollisions<>(this, entity, box, false, (mutableBlockPos, voxelShape) -> mutableBlockPos);

      while (blockCollisions.hasNext()) {
         BlockPos blockPos2 = (BlockPos)blockCollisions.next();
         double e = blockPos2.distToCenterSqr(entity.position());
         if (e < d || e == d && (blockPos == null || blockPos.compareTo(blockPos2) < 0)) {
            blockPos = blockPos2.immutable();
            d = e;
         }
      }

      return Optional.ofNullable(blockPos);
   }

   default Optional<Vec3> findFreePosition(@Nullable Entity entity, VoxelShape shape, Vec3 pos, double x, double y, double z) {
      if (shape.isEmpty()) {
         return Optional.empty();
      } else {
         AABB aABB = shape.bounds().inflate(x, y, z);
         VoxelShape voxelShape = StreamSupport.stream(this.getBlockCollisions(entity, aABB).spliterator(), false)
            .filter(voxelShapex -> this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(voxelShapex.bounds()))
            .flatMap(voxelShapex -> voxelShapex.toAabbs().stream())
            .map(aABBx -> aABBx.inflate(x / 2.0, y / 2.0, z / 2.0))
            .map(Shapes::create)
            .reduce(Shapes.empty(), Shapes::or);
         VoxelShape voxelShape2 = Shapes.join(shape, voxelShape, BooleanOp.ONLY_FIRST);
         return voxelShape2.closestPointTo(pos);
      }
   }
}
