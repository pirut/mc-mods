package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LookControl implements Control {
   protected final Mob mob;
   protected float yMaxRotSpeed;
   protected float xMaxRotAngle;
   protected int lookAtCooldown;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;

   public LookControl(Mob p_24945_) {
      this.mob = p_24945_;
   }

   public void setLookAt(Vec3 p_24965_) {
      this.setLookAt(p_24965_.x, p_24965_.y, p_24965_.z);
   }

   public void setLookAt(Entity p_148052_) {
      this.setLookAt(p_148052_.getX(), getWantedY(p_148052_), p_148052_.getZ());
   }

   public void setLookAt(Entity p_24961_, float p_24962_, float p_24963_) {
      this.setLookAt(p_24961_.getX(), getWantedY(p_24961_), p_24961_.getZ(), p_24962_, p_24963_);
   }

   public void setLookAt(double p_24947_, double p_24948_, double p_24949_) {
      this.setLookAt(p_24947_, p_24948_, p_24949_, this.mob.getHeadRotSpeed(), this.mob.getMaxHeadXRot());
   }

   public void setLookAt(double p_24951_, double p_24952_, double p_24953_, float p_24954_, float p_24955_) {
      this.wantedX = p_24951_;
      this.wantedY = p_24952_;
      this.wantedZ = p_24953_;
      this.yMaxRotSpeed = p_24954_;
      this.xMaxRotAngle = p_24955_;
      this.lookAtCooldown = 2;
   }

   public void tick() {
      if (this.resetXRotOnTick()) {
         this.mob.setXRot(0.0F);
      }

      if (this.lookAtCooldown > 0) {
         this.lookAtCooldown--;
         this.getYRotD().ifPresent(p_287447_ -> this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, p_287447_, this.yMaxRotSpeed));
         this.getXRotD().ifPresent(p_352768_ -> this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), p_352768_, this.xMaxRotAngle)));
      } else {
         this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
      }

      this.clampHeadRotationToBody();
   }

   protected void clampHeadRotationToBody() {
      if (!this.mob.getNavigation().isDone()) {
         this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
      }
   }

   protected boolean resetXRotOnTick() {
      return true;
   }

   public boolean isLookingAtTarget() {
      return this.lookAtCooldown > 0;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   protected Optional<Float> getXRotD() {
      double $$0 = this.wantedX - this.mob.getX();
      double $$1 = this.wantedY - this.mob.getEyeY();
      double $$2 = this.wantedZ - this.mob.getZ();
      double $$3 = Math.sqrt($$0 * $$0 + $$2 * $$2);
      return !(Math.abs($$1) > 1.0E-5F) && !(Math.abs($$3) > 1.0E-5F)
         ? Optional.empty()
         : Optional.of((float)(-(Mth.atan2($$1, $$3) * 180.0F / (float)Math.PI)));
   }

   protected Optional<Float> getYRotD() {
      double $$0 = this.wantedX - this.mob.getX();
      double $$1 = this.wantedZ - this.mob.getZ();
      return !(Math.abs($$1) > 1.0E-5F) && !(Math.abs($$0) > 1.0E-5F)
         ? Optional.empty()
         : Optional.of((float)(Mth.atan2($$1, $$0) * 180.0F / (float)Math.PI) - 90.0F);
   }

   protected float rotateTowards(float p_24957_, float p_24958_, float p_24959_) {
      float $$3 = Mth.degreesDifference(p_24957_, p_24958_);
      float $$4 = Mth.clamp($$3, -p_24959_, p_24959_);
      return p_24957_ + $$4;
   }

   private static double getWantedY(Entity p_24967_) {
      return p_24967_ instanceof LivingEntity ? p_24967_.getEyeY() : (p_24967_.getBoundingBox().minY + p_24967_.getBoundingBox().maxY) / 2.0;
   }
}
