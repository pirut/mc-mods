package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class HoverRandomPos {
   @Nullable
   public static Vec3 getPos(
      PathfinderMob p_148466_, int p_148467_, int p_148468_, double p_148469_, double p_148470_, float p_148471_, int p_148472_, int p_148473_
   ) {
      boolean $$8 = GoalUtils.mobRestricted(p_148466_, p_148467_);
      return RandomPos.generateRandomPos(
         p_148466_,
         () -> {
            BlockPos $$9 = RandomPos.generateRandomDirectionWithinRadians(p_148466_.getRandom(), p_148467_, p_148468_, 0, p_148469_, p_148470_, p_148471_);
            if ($$9 == null) {
               return null;
            } else {
               BlockPos $$10 = LandRandomPos.generateRandomPosTowardDirection(p_148466_, p_148467_, $$8, $$9);
               if ($$10 == null) {
                  return null;
               } else {
                  $$10 = RandomPos.moveUpToAboveSolid(
                     $$10,
                     p_148466_.getRandom().nextInt(p_148472_ - p_148473_ + 1) + p_148473_,
                     p_148466_.level().getMaxBuildHeight(),
                     p_148486_ -> GoalUtils.isSolid(p_148466_, p_148486_)
                  );
                  return !GoalUtils.isWater(p_148466_, $$10) && !GoalUtils.hasMalus(p_148466_, $$10) ? $$10 : null;
               }
            }
         }
      );
   }
}
