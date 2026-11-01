package net.minecraft.world.entity.ai.goal.target;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.scores.Team;

public abstract class TargetGoal extends Goal {
   private static final int EMPTY_REACH_CACHE = 0;
   private static final int CAN_REACH_CACHE = 1;
   private static final int CANT_REACH_CACHE = 2;
   protected final Mob mob;
   protected final boolean mustSee;
   private final boolean mustReach;
   private int reachCache;
   private int reachCacheTime;
   private int unseenTicks;
   @Nullable
   protected LivingEntity targetMob;
   protected int unseenMemoryTicks = 60;

   public TargetGoal(Mob p_26140_, boolean p_26141_) {
      this(p_26140_, p_26141_, false);
   }

   public TargetGoal(Mob p_26143_, boolean p_26144_, boolean p_26145_) {
      this.mob = p_26143_;
      this.mustSee = p_26144_;
      this.mustReach = p_26145_;
   }

   @Override
   public boolean canContinueToUse() {
      LivingEntity $$0 = this.mob.getTarget();
      if ($$0 == null) {
         $$0 = this.targetMob;
      }

      if ($$0 == null) {
         return false;
      } else if (!this.mob.canAttack($$0)) {
         return false;
      } else {
         Team $$1 = this.mob.getTeam();
         Team $$2 = $$0.getTeam();
         if ($$1 != null && $$2 == $$1) {
            return false;
         } else {
            double $$3 = this.getFollowDistance();
            if (this.mob.distanceToSqr($$0) > $$3 * $$3) {
               return false;
            } else {
               if (this.mustSee) {
                  if (this.mob.getSensing().hasLineOfSight($$0)) {
                     this.unseenTicks = 0;
                  } else if (++this.unseenTicks > reducedTickDelay(this.unseenMemoryTicks)) {
                     return false;
                  }
               }

               this.mob.setTarget($$0);
               return true;
            }
         }
      }
   }

   protected double getFollowDistance() {
      return this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
   }

   @Override
   public void start() {
      this.reachCache = 0;
      this.reachCacheTime = 0;
      this.unseenTicks = 0;
   }

   @Override
   public void stop() {
      this.mob.setTarget(null);
      this.targetMob = null;
   }

   protected boolean canAttack(@Nullable LivingEntity p_26151_, TargetingConditions p_26152_) {
      if (p_26151_ == null) {
         return false;
      } else if (!p_26152_.test(this.mob, p_26151_)) {
         return false;
      } else if (!this.mob.isWithinRestriction(p_26151_.blockPosition())) {
         return false;
      } else {
         if (this.mustReach) {
            if (--this.reachCacheTime <= 0) {
               this.reachCache = 0;
            }

            if (this.reachCache == 0) {
               this.reachCache = this.canReach(p_26151_) ? 1 : 2;
            }

            if (this.reachCache == 2) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean canReach(LivingEntity p_26149_) {
      this.reachCacheTime = reducedTickDelay(10 + this.mob.getRandom().nextInt(5));
      Path $$1 = this.mob.getNavigation().createPath(p_26149_, 0);
      if ($$1 == null) {
         return false;
      } else {
         Node $$2 = $$1.getEndNode();
         if ($$2 == null) {
            return false;
         } else {
            int $$3 = $$2.x - p_26149_.getBlockX();
            int $$4 = $$2.z - p_26149_.getBlockZ();
            return $$3 * $$3 + $$4 * $$4 <= 2.25;
         }
      }
   }

   public TargetGoal setUnseenMemoryTicks(int p_26147_) {
      this.unseenMemoryTicks = p_26147_;
      return this;
   }
}
