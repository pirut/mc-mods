package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SmoothSwimmingMoveControl extends MoveControl {
   private static final float FULL_SPEED_TURN_THRESHOLD = 10.0F;
   private static final float STOP_TURN_THRESHOLD = 60.0F;
   private final int maxTurnX;
   private final int maxTurnY;
   private final float inWaterSpeedModifier;
   private final float outsideWaterSpeedModifier;
   private final boolean applyGravity;

   public SmoothSwimmingMoveControl(Mob p_148070_, int p_148071_, int p_148072_, float p_148073_, float p_148074_, boolean p_148075_) {
      super(p_148070_);
      this.maxTurnX = p_148071_;
      this.maxTurnY = p_148072_;
      this.inWaterSpeedModifier = p_148073_;
      this.outsideWaterSpeedModifier = p_148074_;
      this.applyGravity = p_148075_;
   }

   @Override
   public void tick() {
      if (this.applyGravity && this.mob.isInWater()) {
         this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
      }

      if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
         double $$0 = this.wantedX - this.mob.getX();
         double $$1 = this.wantedY - this.mob.getY();
         double $$2 = this.wantedZ - this.mob.getZ();
         double $$3 = $$0 * $$0 + $$1 * $$1 + $$2 * $$2;
         if ($$3 < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
         } else {
            float $$4 = (float)(Mth.atan2($$2, $$0) * 180.0F / (float)Math.PI) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), $$4, this.maxTurnY));
            this.mob.yBodyRot = this.mob.getYRot();
            this.mob.yHeadRot = this.mob.getYRot();
            float $$5 = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (this.mob.isInWater()) {
               this.mob.setSpeed($$5 * this.inWaterSpeedModifier);
               double $$6 = Math.sqrt($$0 * $$0 + $$2 * $$2);
               if (Math.abs($$1) > 1.0E-5F || Math.abs($$6) > 1.0E-5F) {
                  float $$7 = -((float)(Mth.atan2($$1, $$6) * 180.0F / (float)Math.PI));
                  $$7 = Mth.clamp(Mth.wrapDegrees($$7), (float)(-this.maxTurnX), (float)this.maxTurnX);
                  this.mob.setXRot(this.rotlerp(this.mob.getXRot(), $$7, 5.0F));
               }

               float $$8 = Mth.cos(this.mob.getXRot() * (float) (Math.PI / 180.0));
               float $$9 = Mth.sin(this.mob.getXRot() * (float) (Math.PI / 180.0));
               this.mob.zza = $$8 * $$5;
               this.mob.yya = -$$9 * $$5;
            } else {
               float $$10 = Math.abs(Mth.wrapDegrees(this.mob.getYRot() - $$4));
               float $$11 = getTurningSpeedFactor($$10);
               this.mob.setSpeed($$5 * this.outsideWaterSpeedModifier * $$11);
            }
         }
      } else {
         this.mob.setSpeed(0.0F);
         this.mob.setXxa(0.0F);
         this.mob.setYya(0.0F);
         this.mob.setZza(0.0F);
      }
   }

   private static float getTurningSpeedFactor(float p_249853_) {
      return 1.0F - Mth.clamp((p_249853_ - 10.0F) / 50.0F, 0.0F, 1.0F);
   }
}
