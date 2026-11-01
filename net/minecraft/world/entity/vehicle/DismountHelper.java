package net.minecraft.world.entity.vehicle;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DismountHelper {
   public static int[][] offsetsForDirection(Direction p_38468_) {
      Direction $$1 = p_38468_.getClockWise();
      Direction $$2 = $$1.getOpposite();
      Direction $$3 = p_38468_.getOpposite();
      return new int[][]{
         {$$1.getStepX(), $$1.getStepZ()},
         {$$2.getStepX(), $$2.getStepZ()},
         {$$3.getStepX() + $$1.getStepX(), $$3.getStepZ() + $$1.getStepZ()},
         {$$3.getStepX() + $$2.getStepX(), $$3.getStepZ() + $$2.getStepZ()},
         {p_38468_.getStepX() + $$1.getStepX(), p_38468_.getStepZ() + $$1.getStepZ()},
         {p_38468_.getStepX() + $$2.getStepX(), p_38468_.getStepZ() + $$2.getStepZ()},
         {$$3.getStepX(), $$3.getStepZ()},
         {p_38468_.getStepX(), p_38468_.getStepZ()}
      };
   }

   public static boolean isBlockFloorValid(double p_38440_) {
      return !Double.isInfinite(p_38440_) && p_38440_ < 1.0;
   }

   public static boolean canDismountTo(CollisionGetter p_38457_, LivingEntity p_38458_, AABB p_38459_) {
      for (VoxelShape $$4 : p_38457_.getBlockCollisions(p_38458_, p_38459_)) {
         if (!$$4.isEmpty()) {
            return false;
         }
      }

      return p_38457_.getWorldBorder().isWithinBounds(p_38459_);
   }

   public static boolean canDismountTo(CollisionGetter p_150280_, Vec3 p_150281_, LivingEntity p_150282_, Pose p_150283_) {
      return canDismountTo(p_150280_, p_150282_, p_150282_.getLocalBoundsForPose(p_150283_).move(p_150281_));
   }

   public static VoxelShape nonClimbableShape(BlockGetter p_38447_, BlockPos p_38448_) {
      BlockState $$2 = p_38447_.getBlockState(p_38448_);
      return !$$2.is(BlockTags.CLIMBABLE) && (!($$2.getBlock() instanceof TrapDoorBlock) || !$$2.getValue(TrapDoorBlock.OPEN))
         ? $$2.getCollisionShape(p_38447_, p_38448_)
         : Shapes.empty();
   }

   public static double findCeilingFrom(BlockPos p_38464_, int p_38465_, Function<BlockPos, VoxelShape> p_38466_) {
      BlockPos.MutableBlockPos $$3 = p_38464_.mutable();
      int $$4 = 0;

      while ($$4 < p_38465_) {
         VoxelShape $$5 = p_38466_.apply($$3);
         if (!$$5.isEmpty()) {
            return p_38464_.getY() + $$4 + $$5.min(Direction.Axis.Y);
         }

         $$4++;
         $$3.move(Direction.UP);
      }

      return Double.POSITIVE_INFINITY;
   }

   @Nullable
   public static Vec3 findSafeDismountLocation(EntityType<?> p_38442_, CollisionGetter p_38443_, BlockPos p_38444_, boolean p_38445_) {
      if (p_38445_ && p_38442_.isBlockDangerous(p_38443_.getBlockState(p_38444_))) {
         return null;
      } else {
         double $$4 = p_38443_.getBlockFloorHeight(nonClimbableShape(p_38443_, p_38444_), () -> nonClimbableShape(p_38443_, p_38444_.below()));
         if (!isBlockFloorValid($$4)) {
            return null;
         } else if (p_38445_ && $$4 <= 0.0 && p_38442_.isBlockDangerous(p_38443_.getBlockState(p_38444_.below()))) {
            return null;
         } else {
            Vec3 $$5 = Vec3.upFromBottomCenterOf(p_38444_, $$4);
            AABB $$6 = p_38442_.getDimensions().makeBoundingBox($$5);

            for (VoxelShape $$8 : p_38443_.getBlockCollisions(null, $$6)) {
               if (!$$8.isEmpty()) {
                  return null;
               }
            }

            if (p_38442_ != EntityType.PLAYER
               || !p_38443_.getBlockState(p_38444_).is(BlockTags.INVALID_SPAWN_INSIDE)
                  && !p_38443_.getBlockState(p_38444_.above()).is(BlockTags.INVALID_SPAWN_INSIDE)) {
               return !p_38443_.getWorldBorder().isWithinBounds($$6) ? null : $$5;
            } else {
               return null;
            }
         }
      }
   }
}
