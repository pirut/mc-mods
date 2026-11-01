package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LandRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob p_148489_, int p_148490_, int p_148491_) {
      return getPos(p_148489_, p_148490_, p_148491_, p_148489_::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 getPos(PathfinderMob p_148504_, int p_148505_, int p_148506_, ToDoubleFunction<BlockPos> p_148507_) {
      boolean flag = GoalUtils.mobRestricted(p_148504_, p_148505_);
      return RandomPos.generateRandomPos(() -> {
         BlockPos blockpos = RandomPos.generateRandomDirection(p_148504_.getRandom(), p_148505_, p_148506_);
         BlockPos blockpos1 = generateRandomPosTowardDirection(p_148504_, p_148505_, flag, blockpos);
         return blockpos1 == null ? null : movePosUpOutOfSolid(p_148504_, blockpos1);
      }, p_148507_);
   }

   @Nullable
   public static Vec3 getPosTowards(PathfinderMob p_148493_, int p_148494_, int p_148495_, Vec3 p_148496_) {
      Vec3 vec3 = p_148496_.subtract(p_148493_.getX(), p_148493_.getY(), p_148493_.getZ());
      boolean flag = GoalUtils.mobRestricted(p_148493_, p_148494_);
      return getPosInDirection(p_148493_, 0.0, p_148494_, p_148495_, vec3, flag);
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob p_148522_, int p_148523_, int p_148524_, Vec3 p_148525_) {
      return getPosAway(p_148522_, 0.0, p_148523_, p_148524_, p_148525_);
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob p_454884_, double p_455017_, double p_454865_, int p_455205_, Vec3 p_454680_) {
      Vec3 vec3 = p_454884_.position().subtract(p_454680_);
      if (vec3.length() == 0.0) {
         vec3 = new Vec3(p_454884_.getRandom().nextDouble() - 0.5, 0.0, p_454884_.getRandom().nextDouble() - 0.5);
      }

      boolean flag = GoalUtils.mobRestricted(p_454884_, p_454865_);
      return getPosInDirection(p_454884_, p_455017_, p_454865_, p_455205_, vec3, flag);
   }

   @Nullable
   private static Vec3 getPosInDirection(PathfinderMob p_148498_, double p_454677_, double p_455212_, int p_148499_, Vec3 p_148501_, boolean p_148502_) {
      return RandomPos.generateRandomPos(
         p_148498_,
         () -> {
            BlockPos blockpos = RandomPos.generateRandomDirectionWithinRadians(
               p_148498_.getRandom(), p_454677_, p_455212_, p_148499_, 0, p_148501_.x, p_148501_.z, (float) (Math.PI / 2)
            );
            if (blockpos == null) {
               return null;
            } else {
               BlockPos blockpos1 = generateRandomPosTowardDirection(p_148498_, p_455212_, p_148502_, blockpos);
               return blockpos1 == null ? null : movePosUpOutOfSolid(p_148498_, blockpos1);
            }
         }
      );
   }

   @Nullable
   public static BlockPos movePosUpOutOfSolid(PathfinderMob p_148519_, BlockPos p_148520_) {
      p_148520_ = RandomPos.moveUpOutOfSolid(p_148520_, p_148519_.level().getMaxY(), p_148534_ -> GoalUtils.isSolid(p_148519_, p_148534_));
      return !GoalUtils.isWater(p_148519_, p_148520_) && !GoalUtils.hasMalus(p_148519_, p_148520_) ? p_148520_ : null;
   }

   @Nullable
   public static BlockPos generateRandomPosTowardDirection(PathfinderMob p_148514_, double p_455937_, boolean p_148516_, BlockPos p_148517_) {
      BlockPos blockpos = RandomPos.generateRandomPosTowardDirection(p_148514_, p_455937_, p_148514_.getRandom(), p_148517_);
      return !GoalUtils.isOutsideLimits(blockpos, p_148514_)
            && !GoalUtils.isRestricted(p_148516_, p_148514_, blockpos)
            && !GoalUtils.isNotStable(p_148514_.getNavigation(), blockpos)
         ? blockpos
         : null;
   }
}
