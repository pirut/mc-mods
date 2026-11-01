package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public abstract class MinecartBehavior {
   protected final AbstractMinecart minecart;

   protected MinecartBehavior(AbstractMinecart p_478243_) {
      this.minecart = p_478243_;
   }

   public InterpolationHandler getInterpolation() {
      return null;
   }

   public void lerpMotion(Vec3 p_481193_) {
      this.setDeltaMovement(p_481193_);
   }

   public abstract void tick();

   public Level level() {
      return this.minecart.level();
   }

   public abstract void moveAlongTrack(ServerLevel var1);

   public abstract double stepAlongTrack(BlockPos var1, RailShape var2, double var3);

   public abstract boolean pushAndPickupEntities();

   public Vec3 getDeltaMovement() {
      return this.minecart.getDeltaMovement();
   }

   public void setDeltaMovement(Vec3 p_478208_) {
      this.minecart.setDeltaMovement(p_478208_);
   }

   public void setDeltaMovement(double p_478130_, double p_480535_, double p_480282_) {
      this.minecart.setDeltaMovement(p_478130_, p_480535_, p_480282_);
   }

   public Vec3 position() {
      return this.minecart.position();
   }

   public double getX() {
      return this.minecart.getX();
   }

   public double getY() {
      return this.minecart.getY();
   }

   public double getZ() {
      return this.minecart.getZ();
   }

   public void setPos(Vec3 p_480802_) {
      this.minecart.setPos(p_480802_);
   }

   public void setPos(double p_479415_, double p_478338_, double p_479470_) {
      this.minecart.setPos(p_479415_, p_478338_, p_479470_);
   }

   public float getXRot() {
      return this.minecart.getXRot();
   }

   public void setXRot(float p_481912_) {
      this.minecart.setXRot(p_481912_);
   }

   public float getYRot() {
      return this.minecart.getYRot();
   }

   public void setYRot(float p_480887_) {
      this.minecart.setYRot(p_480887_);
   }

   public Direction getMotionDirection() {
      return this.minecart.getDirection();
   }

   public Vec3 getKnownMovement(Vec3 p_481129_) {
      return p_481129_;
   }

   public abstract double getMaxSpeed(ServerLevel var1);

   public abstract double getSlowdownFactor();
}
