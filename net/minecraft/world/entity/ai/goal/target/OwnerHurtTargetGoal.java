package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class OwnerHurtTargetGoal extends TargetGoal {
   private final TamableAnimal tameAnimal;
   private LivingEntity ownerLastHurt;
   private int timestamp;

   public OwnerHurtTargetGoal(TamableAnimal p_26114_) {
      super(p_26114_, false);
      this.tameAnimal = p_26114_;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      if (this.tameAnimal.isTame() && !this.tameAnimal.isOrderedToSit()) {
         LivingEntity $$0 = this.tameAnimal.getOwner();
         if ($$0 == null) {
            return false;
         } else {
            this.ownerLastHurt = $$0.getLastHurtMob();
            int $$1 = $$0.getLastHurtMobTimestamp();
            return $$1 != this.timestamp
               && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
               && this.tameAnimal.wantsToAttack(this.ownerLastHurt, $$0);
         }
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.mob.setTarget(this.ownerLastHurt);
      LivingEntity $$0 = this.tameAnimal.getOwner();
      if ($$0 != null) {
         this.timestamp = $$0.getLastHurtMobTimestamp();
      }

      super.start();
   }
}
