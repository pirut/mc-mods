package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

public class SitWhenOrderedToGoal extends Goal {
   private final TamableAnimal mob;

   public SitWhenOrderedToGoal(TamableAnimal p_25898_) {
      this.mob = p_25898_;
      this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   @Override
   public boolean canContinueToUse() {
      return this.mob.isOrderedToSit();
   }

   @Override
   public boolean canUse() {
      if (!this.mob.isTame()) {
         return false;
      } else if (this.mob.isInWaterOrBubble()) {
         return false;
      } else if (!this.mob.onGround()) {
         return false;
      } else {
         LivingEntity $$0 = this.mob.getOwner();
         if ($$0 == null) {
            return true;
         } else {
            return this.mob.distanceToSqr($$0) < 144.0 && $$0.getLastHurtByMob() != null ? false : this.mob.isOrderedToSit();
         }
      }
   }

   @Override
   public void start() {
      this.mob.getNavigation().stop();
      this.mob.setInSittingPose(true);
   }

   @Override
   public void stop() {
      this.mob.setInSittingPose(false);
   }
}
