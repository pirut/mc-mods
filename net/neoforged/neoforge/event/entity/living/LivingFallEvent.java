package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;

public class LivingFallEvent extends LivingEvent implements ICancellableEvent {
   private double distance;
   private float damageMultiplier;

   public LivingFallEvent(LivingEntity entity, double distance, float damageMultiplier) {
      super(entity);
      this.setDistance(distance);
      this.setDamageMultiplier(damageMultiplier);
   }

   public double getDistance() {
      return this.distance;
   }

   public void setDistance(double distance) {
      this.distance = distance;
   }

   public float getDamageMultiplier() {
      return this.damageMultiplier;
   }

   public void setDamageMultiplier(float damageMultiplier) {
      this.damageMultiplier = damageMultiplier;
   }
}
