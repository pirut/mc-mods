package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HoverRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob mob, int radius, int yRange, double x, double z, float amplifier, int maxSwimUp, int minSwimUp) {
      boolean bl = GoalUtils.mobRestricted(mob, radius);
      return RandomPos.generateRandomPos(
         mob,
         () -> {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), 0.0, radius, yRange, 0, x, z, amplifier);
            if (blockPos == null) {
               return null;
            } else {
               BlockPos blockPos2 = LandRandomPos.generateRandomPosTowardDirection(mob, radius, bl, blockPos);
               if (blockPos2 == null) {
                  return null;
               } else {
                  blockPos2 = RandomPos.moveUpToAboveSolid(
                     blockPos2,
                     mob.getRandom().nextInt(maxSwimUp - minSwimUp + 1) + minSwimUp,
                     mob.level().getMaxY(),
                     blockPosx -> GoalUtils.isSolid(mob, blockPosx)
                  );
                  return !GoalUtils.isWater(mob, blockPos2) && !GoalUtils.hasMalus(mob, blockPos2) ? blockPos2 : null;
               }
            }
         }
      );
   }
}
