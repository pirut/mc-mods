package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public class DragonSittingScanningPhase extends AbstractDragonSittingPhase {
   private static final int SITTING_SCANNING_IDLE_TICKS = 100;
   private static final int SITTING_ATTACK_Y_VIEW_RANGE = 10;
   private static final int SITTING_ATTACK_VIEW_RANGE = 20;
   private static final int SITTING_CHARGE_VIEW_RANGE = 150;
   private static final TargetingConditions CHARGE_TARGETING = TargetingConditions.forCombat().range(150.0);
   private final TargetingConditions scanTargeting;
   private int scanningTime;

   public DragonSittingScanningPhase(EnderDragon p_31342_) {
      super(p_31342_);
      this.scanTargeting = TargetingConditions.forCombat().range(20.0).selector(p_352809_ -> Math.abs(p_352809_.getY() - p_31342_.getY()) <= 10.0);
   }

   @Override
   public void doServerTick() {
      this.scanningTime++;
      LivingEntity $$0 = this.dragon.level().getNearestPlayer(this.scanTargeting, this.dragon, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
      if ($$0 != null) {
         if (this.scanningTime > 25) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_ATTACKING);
         } else {
            Vec3 $$1 = new Vec3($$0.getX() - this.dragon.getX(), 0.0, $$0.getZ() - this.dragon.getZ()).normalize();
            Vec3 $$2 = new Vec3(Mth.sin(this.dragon.getYRot() * (float) (Math.PI / 180.0)), 0.0, -Mth.cos(this.dragon.getYRot() * (float) (Math.PI / 180.0)))
               .normalize();
            float $$3 = (float)$$2.dot($$1);
            float $$4 = (float)(Math.acos($$3) * 180.0F / (float)Math.PI) + 0.5F;
            if ($$4 < 0.0F || $$4 > 10.0F) {
               double $$5 = $$0.getX() - this.dragon.head.getX();
               double $$6 = $$0.getZ() - this.dragon.head.getZ();
               double $$7 = Mth.clamp(Mth.wrapDegrees(180.0 - Mth.atan2($$5, $$6) * 180.0F / (float)Math.PI - this.dragon.getYRot()), -100.0, 100.0);
               this.dragon.yRotA *= 0.8F;
               float $$8 = (float)Math.sqrt($$5 * $$5 + $$6 * $$6) + 1.0F;
               float $$9 = $$8;
               if ($$8 > 40.0F) {
                  $$8 = 40.0F;
               }

               this.dragon.yRotA += (float)$$7 * (0.7F / $$8 / $$9);
               this.dragon.setYRot(this.dragon.getYRot() + this.dragon.yRotA);
            }
         }
      } else if (this.scanningTime >= 100) {
         $$0 = this.dragon.level().getNearestPlayer(CHARGE_TARGETING, this.dragon, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
         this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
         if ($$0 != null) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
            this.dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(new Vec3($$0.getX(), $$0.getY(), $$0.getZ()));
         }
      }
   }

   @Override
   public void begin() {
      this.scanningTime = 0;
   }

   @Override
   public EnderDragonPhase<DragonSittingScanningPhase> getPhase() {
      return EnderDragonPhase.SITTING_SCANNING;
   }
}
