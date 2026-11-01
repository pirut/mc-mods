package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;

public class LivingHealEvent extends LivingEvent implements ICancellableEvent {
   private float amount;

   public LivingHealEvent(LivingEntity entity, float amount) {
      super(entity);
      this.setAmount(amount);
   }

   public float getAmount() {
      return this.amount;
   }

   public void setAmount(float amount) {
      this.amount = amount;
   }
}
