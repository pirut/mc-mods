package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class RangedAttackGoal extends Goal {
   private final Mob mob;
   private final RangedAttackMob rangedAttackMob;
   @Nullable
   private LivingEntity target;
   private int attackTime = -1;
   private final double speedModifier;
   private int seeTime;
   private final int attackIntervalMin;
   private final int attackIntervalMax;
   private final float attackRadius;
   private final float attackRadiusSqr;

   public RangedAttackGoal(RangedAttackMob p_25768_, double p_25769_, int p_25770_, float p_25771_) {
      this(p_25768_, p_25769_, p_25770_, p_25770_, p_25771_);
   }

   public RangedAttackGoal(RangedAttackMob p_25773_, double p_25774_, int p_25775_, int p_25776_, float p_25777_) {
      if (!(p_25773_ instanceof LivingEntity)) {
         throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
      } else {
         this.rangedAttackMob = p_25773_;
         this.mob = (Mob)p_25773_;
         this.speedModifier = p_25774_;
         this.attackIntervalMin = p_25775_;
         this.attackIntervalMax = p_25776_;
         this.attackRadius = p_25777_;
         this.attackRadiusSqr = p_25777_ * p_25777_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }
   }

   @Override
   public boolean canUse() {
      LivingEntity $$0 = this.mob.getTarget();
      if ($$0 != null && $$0.isAlive()) {
         this.target = $$0;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse() || this.target.isAlive() && !this.mob.getNavigation().isDone();
   }

   @Override
   public void stop() {
      this.target = null;
      this.seeTime = 0;
      this.attackTime = -1;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      double $$0 = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
      boolean $$1 = this.mob.getSensing().hasLineOfSight(this.target);
      if ($$1) {
         this.seeTime++;
      } else {
         this.seeTime = 0;
      }

      if (!($$0 > this.attackRadiusSqr) && this.seeTime >= 5) {
         this.mob.getNavigation().stop();
      } else {
         this.mob.getNavigation().moveTo(this.target, this.speedModifier);
      }

      this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
      if (--this.attackTime == 0) {
         if (!$$1) {
            return;
         }

         float $$2 = (float)Math.sqrt($$0) / this.attackRadius;
         float $$3 = Mth.clamp($$2, 0.1F, 1.0F);
         this.rangedAttackMob.performRangedAttack(this.target, $$3);
         this.attackTime = Mth.floor($$2 * (this.attackIntervalMax - this.attackIntervalMin) + this.attackIntervalMin);
      } else if (this.attackTime < 0) {
         this.attackTime = Mth.floor(Mth.lerp(Math.sqrt($$0) / this.attackRadius, (double)this.attackIntervalMin, (double)this.attackIntervalMax));
      }
   }
}
