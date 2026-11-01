package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class RemotePlayer extends AbstractClientPlayer {
   private Vec3 lerpDeltaMovement = Vec3.ZERO;
   private int lerpDeltaMovementSteps;

   public RemotePlayer(ClientLevel clientLevel, GameProfile gameProfile) {
      super(clientLevel, gameProfile);
      this.noPhysics = true;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      double d = this.getBoundingBox().getSize() * 10.0;
      if (Double.isNaN(d)) {
         d = 1.0;
      }

      d *= 64.0 * getViewScale();
      return distance < d * d;
   }

   @Override
   public boolean hurtClient(DamageSource damageSource) {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      this.calculateEntityAnimation(false);
   }

   @Override
   public void aiStep() {
      if (this.isInterpolating()) {
         this.getInterpolation().interpolate();
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

      this.updateSwingTime();
      this.updateBob();

      try (Zone zone = Profiler.get().zone("push")) {
         this.pushEntities();
      }
   }

   @Override
   public void lerpMotion(Vec3 deltaMovement) {
      this.lerpDeltaMovement = deltaMovement;
      this.lerpDeltaMovementSteps = this.getType().updateInterval() + 1;
   }

   @Override
   protected void updatePlayerPose() {
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      this.setOldPosAndRot();
   }
}
