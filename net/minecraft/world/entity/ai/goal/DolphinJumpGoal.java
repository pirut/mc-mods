package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class DolphinJumpGoal extends JumpGoal {
   private static final int[] STEPS_TO_CHECK = new int[]{0, 1, 4, 5, 6, 7};
   private final Dolphin dolphin;
   private final int interval;
   private boolean breached;

   public DolphinJumpGoal(Dolphin p_25168_, int p_25169_) {
      this.dolphin = p_25168_;
      this.interval = reducedTickDelay(p_25169_);
   }

   @Override
   public boolean canUse() {
      if (this.dolphin.getRandom().nextInt(this.interval) != 0) {
         return false;
      } else {
         Direction $$0 = this.dolphin.getMotionDirection();
         int $$1 = $$0.getStepX();
         int $$2 = $$0.getStepZ();
         BlockPos $$3 = this.dolphin.blockPosition();

         for (int $$4 : STEPS_TO_CHECK) {
            if (!this.waterIsClear($$3, $$1, $$2, $$4) || !this.surfaceIsClear($$3, $$1, $$2, $$4)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean waterIsClear(BlockPos p_25173_, int p_25174_, int p_25175_, int p_25176_) {
      BlockPos $$4 = p_25173_.offset(p_25174_ * p_25176_, 0, p_25175_ * p_25176_);
      return this.dolphin.level().getFluidState($$4).is(FluidTags.WATER) && !this.dolphin.level().getBlockState($$4).blocksMotion();
   }

   private boolean surfaceIsClear(BlockPos p_25179_, int p_25180_, int p_25181_, int p_25182_) {
      return this.dolphin.level().getBlockState(p_25179_.offset(p_25180_ * p_25182_, 1, p_25181_ * p_25182_)).isAir()
         && this.dolphin.level().getBlockState(p_25179_.offset(p_25180_ * p_25182_, 2, p_25181_ * p_25182_)).isAir();
   }

   @Override
   public boolean canContinueToUse() {
      double $$0 = this.dolphin.getDeltaMovement().y;
      return (!($$0 * $$0 < 0.03F) || this.dolphin.getXRot() == 0.0F || !(Math.abs(this.dolphin.getXRot()) < 10.0F) || !this.dolphin.isInWater())
         && !this.dolphin.onGround();
   }

   @Override
   public boolean isInterruptable() {
      return false;
   }

   @Override
   public void start() {
      Direction $$0 = this.dolphin.getMotionDirection();
      this.dolphin.setDeltaMovement(this.dolphin.getDeltaMovement().add($$0.getStepX() * 0.6, 0.7, $$0.getStepZ() * 0.6));
      this.dolphin.getNavigation().stop();
   }

   @Override
   public void stop() {
      this.dolphin.setXRot(0.0F);
   }

   @Override
   public void tick() {
      boolean $$0 = this.breached;
      if (!$$0) {
         FluidState $$1 = this.dolphin.level().getFluidState(this.dolphin.blockPosition());
         this.breached = $$1.is(FluidTags.WATER);
      }

      if (this.breached && !$$0) {
         this.dolphin.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
      }

      Vec3 $$2 = this.dolphin.getDeltaMovement();
      if ($$2.y * $$2.y < 0.03F && this.dolphin.getXRot() != 0.0F) {
         this.dolphin.setXRot(Mth.rotLerp(0.2F, this.dolphin.getXRot(), 0.0F));
      } else if ($$2.length() > 1.0E-5F) {
         double $$3 = $$2.horizontalDistance();
         double $$4 = Math.atan2(-$$2.y, $$3) * 180.0F / (float)Math.PI;
         this.dolphin.setXRot((float)$$4);
      }
   }
}
