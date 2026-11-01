package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CollisionGetter extends BlockGetter {
   WorldBorder getWorldBorder();

   @Nullable
   BlockGetter getChunkForCollisions(int var1, int var2);

   default boolean isUnobstructed(@Nullable Entity p_45750_, VoxelShape p_45751_) {
      return true;
   }

   default boolean isUnobstructed(BlockState p_45753_, BlockPos p_45754_, CollisionContext p_45755_) {
      VoxelShape $$3 = p_45753_.getCollisionShape(this, p_45754_, p_45755_);
      return $$3.isEmpty() || this.isUnobstructed(null, $$3.move(p_45754_.getX(), p_45754_.getY(), p_45754_.getZ()));
   }

   default boolean isUnobstructed(Entity p_45785_) {
      return this.isUnobstructed(p_45785_, Shapes.create(p_45785_.getBoundingBox()));
   }

   default boolean noCollision(AABB p_45773_) {
      return this.noCollision(null, p_45773_);
   }

   default boolean noCollision(Entity p_45787_) {
      return this.noCollision(p_45787_, p_45787_.getBoundingBox());
   }

   default boolean noCollision(@Nullable Entity p_45757_, AABB p_45758_) {
      for (VoxelShape $$2 : this.getBlockCollisions(p_45757_, p_45758_)) {
         if (!$$2.isEmpty()) {
            return false;
         }
      }

      if (!this.getEntityCollisions(p_45757_, p_45758_).isEmpty()) {
         return false;
      } else if (p_45757_ == null) {
         return true;
      } else {
         VoxelShape $$3 = this.borderCollision(p_45757_, p_45758_);
         return $$3 == null || !Shapes.joinIsNotEmpty($$3, Shapes.create(p_45758_), BooleanOp.AND);
      }
   }

   default boolean noBlockCollision(@Nullable Entity p_295728_, AABB p_294209_) {
      for (VoxelShape $$2 : this.getBlockCollisions(p_295728_, p_294209_)) {
         if (!$$2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   List<VoxelShape> getEntityCollisions(@Nullable Entity var1, AABB var2);

   default Iterable<VoxelShape> getCollisions(@Nullable Entity p_186432_, AABB p_186433_) {
      List<VoxelShape> $$2 = this.getEntityCollisions(p_186432_, p_186433_);
      Iterable<VoxelShape> $$3 = this.getBlockCollisions(p_186432_, p_186433_);
      return $$2.isEmpty() ? $$3 : Iterables.concat($$2, $$3);
   }

   default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity p_186435_, AABB p_186436_) {
      return () -> new BlockCollisions<VoxelShape>(this, p_186435_, p_186436_, false, (p_286215_, p_286216_) -> p_286216_);
   }

   @Nullable
   private VoxelShape borderCollision(Entity p_186441_, AABB p_186442_) {
      WorldBorder $$2 = this.getWorldBorder();
      return $$2.isInsideCloseToBorder(p_186441_, p_186442_) ? $$2.getCollisionShape() : null;
   }

   default boolean collidesWithSuffocatingBlock(@Nullable Entity p_186438_, AABB p_186439_) {
      BlockCollisions<VoxelShape> $$2 = new BlockCollisions<>(this, p_186438_, p_186439_, true, (p_286211_, p_286212_) -> p_286212_);

      while ($$2.hasNext()) {
         if (!((VoxelShape)$$2.next()).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   default Optional<BlockPos> findSupportingBlock(Entity p_286468_, AABB p_286792_) {
      BlockPos $$2 = null;
      double $$3 = Double.MAX_VALUE;
      BlockCollisions<BlockPos> $$4 = new BlockCollisions<>(this, p_286468_, p_286792_, false, (p_286213_, p_286214_) -> p_286213_);

      while ($$4.hasNext()) {
         BlockPos $$5 = (BlockPos)$$4.next();
         double $$6 = $$5.distToCenterSqr(p_286468_.position());
         if ($$6 < $$3 || $$6 == $$3 && ($$2 == null || $$2.compareTo($$5) < 0)) {
            $$2 = $$5.immutable();
            $$3 = $$6;
         }
      }

      return Optional.ofNullable($$2);
   }

   default Optional<Vec3> findFreePosition(
      @Nullable Entity p_151419_, VoxelShape p_151420_, Vec3 p_151421_, double p_151422_, double p_151423_, double p_151424_
   ) {
      if (p_151420_.isEmpty()) {
         return Optional.empty();
      } else {
         AABB $$6 = p_151420_.bounds().inflate(p_151422_, p_151423_, p_151424_);
         VoxelShape $$7 = StreamSupport.stream(this.getBlockCollisions(p_151419_, $$6).spliterator(), false)
            .filter(p_186430_ -> this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(p_186430_.bounds()))
            .flatMap(p_186426_ -> p_186426_.toAabbs().stream())
            .map(p_186424_ -> p_186424_.inflate(p_151422_ / 2.0, p_151423_ / 2.0, p_151424_ / 2.0))
            .map(Shapes::create)
            .reduce(Shapes.empty(), Shapes::or);
         VoxelShape $$8 = Shapes.join(p_151420_, $$7, BooleanOp.ONLY_FIRST);
         return $$8.closestPointTo(p_151421_);
      }
   }
}
