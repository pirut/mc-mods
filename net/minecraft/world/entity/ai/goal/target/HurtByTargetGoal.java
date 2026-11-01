package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

public class HurtByTargetGoal extends TargetGoal {
   private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
   private static final int ALERT_RANGE_Y = 10;
   private boolean alertSameType;
   private int timestamp;
   private final Class<?>[] toIgnoreDamage;
   @Nullable
   private Class<?>[] toIgnoreAlert;

   public HurtByTargetGoal(PathfinderMob p_26039_, Class<?>... p_26040_) {
      super(p_26039_, true);
      this.toIgnoreDamage = p_26040_;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      int $$0 = this.mob.getLastHurtByMobTimestamp();
      LivingEntity $$1 = this.mob.getLastHurtByMob();
      if ($$0 != this.timestamp && $$1 != null) {
         if ($$1.getType() == EntityType.PLAYER && this.mob.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
            return false;
         } else {
            for (Class<?> $$2 : this.toIgnoreDamage) {
               if ($$2.isAssignableFrom($$1.getClass())) {
                  return false;
               }
            }

            return this.canAttack($$1, HURT_BY_TARGETING);
         }
      } else {
         return false;
      }
   }

   public HurtByTargetGoal setAlertOthers(Class<?>... p_26045_) {
      this.alertSameType = true;
      this.toIgnoreAlert = p_26045_;
      return this;
   }

   @Override
   public void start() {
      this.mob.setTarget(this.mob.getLastHurtByMob());
      this.targetMob = this.mob.getTarget();
      this.timestamp = this.mob.getLastHurtByMobTimestamp();
      this.unseenMemoryTicks = 300;
      if (this.alertSameType) {
         this.alertOthers();
      }

      super.start();
   }

   protected void alertOthers() {
      double $$0 = this.getFollowDistance();
      AABB $$1 = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate($$0, 10.0, $$0);
      List<? extends Mob> $$2 = this.mob.level().getEntitiesOfClass((Class<? extends Mob>)this.mob.getClass(), $$1, EntitySelector.NO_SPECTATORS);
      Iterator var5 = $$2.iterator();

      while (true) {
         Mob $$3;
         while (true) {
            if (!var5.hasNext()) {
               return;
            }

            $$3 = (Mob)var5.next();
            if (this.mob != $$3
               && $$3.getTarget() == null
               && (!(this.mob instanceof TamableAnimal) || ((TamableAnimal)this.mob).getOwner() == ((TamableAnimal)$$3).getOwner())
               && !$$3.isAlliedTo(this.mob.getLastHurtByMob())) {
               if (this.toIgnoreAlert == null) {
                  break;
               }

               boolean $$4 = false;

               for (Class<?> $$5 : this.toIgnoreAlert) {
                  if ($$3.getClass() == $$5) {
                     $$4 = true;
                     break;
                  }
               }

               if (!$$4) {
                  break;
               }
            }
         }

         this.alertOther($$3, this.mob.getLastHurtByMob());
      }
   }

   protected void alertOther(Mob p_26042_, LivingEntity p_26043_) {
      p_26042_.setTarget(p_26043_);
   }
}
