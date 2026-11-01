package net.minecraft.world.entity.ai.goal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;

public class LandOnOwnersShoulderGoal extends Goal {
   private final ShoulderRidingEntity entity;
   private ServerPlayer owner;
   private boolean isSittingOnShoulder;

   public LandOnOwnersShoulderGoal(ShoulderRidingEntity p_25483_) {
      this.entity = p_25483_;
   }

   @Override
   public boolean canUse() {
      ServerPlayer $$0 = (ServerPlayer)this.entity.getOwner();
      boolean $$1 = $$0 != null && !$$0.isSpectator() && !$$0.getAbilities().flying && !$$0.isInWater() && !$$0.isInPowderSnow;
      return !this.entity.isOrderedToSit() && $$1 && this.entity.canSitOnShoulder();
   }

   @Override
   public boolean isInterruptable() {
      return !this.isSittingOnShoulder;
   }

   @Override
   public void start() {
      this.owner = (ServerPlayer)this.entity.getOwner();
      this.isSittingOnShoulder = false;
   }

   @Override
   public void tick() {
      if (!this.isSittingOnShoulder && !this.entity.isInSittingPose() && !this.entity.isLeashed()) {
         if (this.entity.getBoundingBox().intersects(this.owner.getBoundingBox())) {
            this.isSittingOnShoulder = this.entity.setEntityOnShoulder(this.owner);
         }
      }
   }
}
