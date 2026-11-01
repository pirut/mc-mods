package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RemotePlayer extends AbstractClientPlayer {
   private Vec3 lerpDeltaMovement = Vec3.ZERO;
   private int lerpDeltaMovementSteps;

   public RemotePlayer(ClientLevel p_252213_, GameProfile p_250471_) {
      super(p_252213_, p_250471_);
      this.noPhysics = true;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_108770_) {
      double $$1 = this.getBoundingBox().getSize() * 10.0;
      if (Double.isNaN($$1)) {
         $$1 = 1.0;
      }

      $$1 *= 64.0 * getViewScale();
      return p_108770_ < $$1 * $$1;
   }

   @Override
   public boolean hurt(DamageSource p_108772_, float p_108773_) {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      this.calculateEntityAnimation(false);
   }

   @Override
   public void aiStep() {
      if (this.lerpSteps > 0) {
         this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
         this.lerpSteps--;
      }

      if (this.lerpHeadSteps > 0) {
         this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
         this.lerpHeadSteps--;
      }

      if (this.lerpDeltaMovementSteps > 0) {
         this.addDeltaMovement(
            new Vec3(
               (this.lerpDeltaMovement.x - this.getDeltaMovement().x) / this.lerpDeltaMovementSteps,
               (this.lerpDeltaMovement.y - this.getDeltaMovement().y) / this.lerpDeltaMovementSteps,
               (this.lerpDeltaMovement.z - this.getDeltaMovement().z) / this.lerpDeltaMovementSteps
            )
         );
         this.lerpDeltaMovementSteps--;
      }

      this.oBob = this.bob;
      this.updateSwingTime();
      float $$1;
      if (this.onGround() && !this.isDeadOrDying()) {
         $$1 = (float)Math.min(0.1, this.getDeltaMovement().horizontalDistance());
      } else {
         $$1 = 0.0F;
      }

      this.bob = this.bob + ($$1 - this.bob) * 0.4F;
      this.level().getProfiler().push("push");
      this.pushEntities();
      this.level().getProfiler().pop();
   }

   @Override
   public void lerpMotion(double p_273090_, double p_272647_, double p_273555_) {
      this.lerpDeltaMovement = new Vec3(p_273090_, p_272647_, p_273555_);
      this.lerpDeltaMovementSteps = this.getType().updateInterval() + 1;
   }

   @Override
   protected void updatePlayerPose() {
   }

   @Override
   public void sendSystemMessage(Component p_234163_) {
      Minecraft $$1 = Minecraft.getInstance();
      $$1.gui.getChat().addMessage(p_234163_);
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_302238_) {
      super.recreateFromPacket(p_302238_);
      this.setOldPosAndRot();
   }
}
