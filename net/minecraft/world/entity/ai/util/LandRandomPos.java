package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LandRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob mob, int radius, int verticalRange) {
      return getPos(mob, radius, verticalRange, mob::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 getPos(PathfinderMob mob, int radius, int yRange, ToDoubleFunction<BlockPos> toDoubleFunction) {
      boolean bl = GoalUtils.mobRestricted(mob, radius);
      return RandomPos.generateRandomPos(() -> {
         BlockPos blockPos = RandomPos.generateRandomDirection(mob.getRandom(), radius, yRange);
         BlockPos blockPos2 = generateRandomPosTowardDirection(mob, radius, bl, blockPos);
         return blockPos2 == null ? null : movePosUpOutOfSolid(mob, blockPos2);
      }, toDoubleFunction);
   }

   @Nullable
   public static Vec3 getPosTowards(PathfinderMob mob, int radius, int yRange, Vec3 pos) {
      Vec3 vec3 = pos.subtract(mob.getX(), mob.getY(), mob.getZ());
      boolean bl = GoalUtils.mobRestricted(mob, radius);
      return getPosInDirection(mob, 0.0, radius, yRange, vec3, bl);
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob mob, int radius, int yRange, Vec3 pos) {
      return getPosAway(mob, 0.0, radius, yRange, pos);
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob mob, double minDistance, double maxDistance, int yRange, Vec3 pos) {
      Vec3 vec3 = mob.position().subtract(pos);
      if (vec3.length() == 0.0) {
         vec3 = new Vec3(mob.getRandom().nextDouble() - 0.5, 0.0, mob.getRandom().nextDouble() - 0.5);
      }

      boolean bl = GoalUtils.mobRestricted(mob, maxDistance);
      return getPosInDirection(mob, minDistance, maxDistance, yRange, vec3, bl);
   }

   @Nullable
   private static Vec3 getPosInDirection(PathfinderMob mob, double minDistance, double maxDistance, int yRange, Vec3 pos, boolean shortCircuit) {
      return RandomPos.generateRandomPos(
         mob,
         () -> {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(
               mob.getRandom(), minDistance, maxDistance, yRange, 0, pos.x, pos.z, (float) (Math.PI / 2)
            );
            if (blockPos == null) {
               return null;
            } else {
               BlockPos blockPos2 = generateRandomPosTowardDirection(mob, maxDistance, shortCircuit, blockPos);
               return blockPos2 == null ? null : movePosUpOutOfSolid(mob, blockPos2);
            }
         }
      );
   }

   @Nullable
   public static BlockPos movePosUpOutOfSolid(PathfinderMob mob, BlockPos pos) {
      pos = RandomPos.moveUpOutOfSolid(pos, mob.level().getMaxY(), blockPos -> GoalUtils.isSolid(mob, blockPos));
      return !GoalUtils.isWater(mob, pos) && !GoalUtils.hasMalus(mob, pos) ? pos : null;
   }

   @Nullable
   public static BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double radius, boolean shortCircuit, BlockPos pos) {
      BlockPos blockPos = RandomPos.generateRandomPosTowardDirection(mob, radius, mob.getRandom(), pos);
      return !GoalUtils.isOutsideLimits(blockPos, mob)
            && !GoalUtils.isRestricted(shortCircuit, mob, blockPos)
            && !GoalUtils.isNotStable(mob.getNavigation(), blockPos)
         ? blockPos
         : null;
   }
}
