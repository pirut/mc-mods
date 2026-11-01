package net.minecraft.client.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class ClientAvatarState {
   private Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
   private float walkDist;
   private float walkDistO;
   private double xCloak;
   private double yCloak;
   private double zCloak;
   private double xCloakO;
   private double yCloakO;
   private double zCloakO;
   private float bob;
   private float bobO;

   public void tick(Vec3 position, Vec3 deltaMovement) {
      this.walkDistO = this.walkDist;
      this.deltaMovementOnPreviousTick = deltaMovement;
      this.moveCloak(position);
   }

   public void addWalkDistance(float distance) {
      this.walkDist += distance;
   }

   public Vec3 deltaMovementOnPreviousTick() {
      return this.deltaMovementOnPreviousTick;
   }

   private void moveCloak(Vec3 position) {
      this.xCloakO = this.xCloak;
      this.yCloakO = this.yCloak;
      this.zCloakO = this.zCloak;
      double d = position.x() - this.xCloak;
      double e = position.y() - this.yCloak;
      double f = position.z() - this.zCloak;
      double g = 10.0;
      if (!(d > 10.0) && !(d < -10.0)) {
         this.xCloak += d * 0.25;
      } else {
         this.xCloak = position.x();
         this.xCloakO = this.xCloak;
      }

      if (!(e > 10.0) && !(e < -10.0)) {
         this.yCloak += e * 0.25;
      } else {
         this.yCloak = position.y();
         this.yCloakO = this.yCloak;
      }

      if (!(f > 10.0) && !(f < -10.0)) {
         this.zCloak += f * 0.25;
      } else {
         this.zCloak = position.z();
         this.zCloakO = this.zCloak;
      }
   }

   public double getInterpolatedCloakX(float partialTick) {
      return Mth.lerp((double)partialTick, this.xCloakO, this.xCloak);
   }

   public double getInterpolatedCloakY(float partialTick) {
      return Mth.lerp((double)partialTick, this.yCloakO, this.yCloak);
   }

   public double getInterpolatedCloakZ(float partialTick) {
      return Mth.lerp((double)partialTick, this.zCloakO, this.zCloak);
   }

   public void updateBob(float bob) {
      this.bobO = this.bob;
      this.bob = this.bob + (bob - this.bob) * 0.4F;
   }

   public void resetBob() {
      this.bobO = this.bob;
      this.bob = 0.0F;
   }

   public float getInterpolatedBob(float partialTick) {
      return Mth.lerp(partialTick, this.bobO, this.bob);
   }

   public float getBackwardsInterpolatedWalkDistance(float partialTick) {
      float f = this.walkDist - this.walkDistO;
      return -(this.walkDist + f * partialTick);
   }

   public float getInterpolatedWalkDistance(float partialTick) {
      return Mth.lerp(partialTick, this.walkDistO, this.walkDist);
   }
}
