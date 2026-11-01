package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

public abstract class HitResult {
   protected final Vec3 location;

   protected HitResult(Vec3 p_82447_) {
      this.location = p_82447_;
   }

   public double distanceTo(Entity p_82449_) {
      double $$1 = this.location.x - p_82449_.getX();
      double $$2 = this.location.y - p_82449_.getY();
      double $$3 = this.location.z - p_82449_.getZ();
      return $$1 * $$1 + $$2 * $$2 + $$3 * $$3;
   }

   public abstract HitResult.Type getType();

   public Vec3 getLocation() {
      return this.location;
   }

   public static enum Type {
      MISS,
      BLOCK,
      ENTITY;
   }
}
