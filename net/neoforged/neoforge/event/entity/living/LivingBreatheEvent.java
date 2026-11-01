package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;

public class LivingBreatheEvent extends LivingEvent {
   private boolean canBreathe;
   private int consumeAirAmount;
   private int refillAirAmount;

   public LivingBreatheEvent(LivingEntity entity, boolean canBreathe, int consumeAirAmount, int refillAirAmount) {
      super(entity);
      this.canBreathe = canBreathe;
      this.consumeAirAmount = Math.max(consumeAirAmount, 0);
      this.refillAirAmount = Math.max(refillAirAmount, 0);
   }

   public boolean canBreathe() {
      return this.canBreathe;
   }

   public void setCanBreathe(boolean canBreathe) {
      this.canBreathe = canBreathe;
   }

   public int getConsumeAirAmount() {
      return this.consumeAirAmount;
   }

   public void setConsumeAirAmount(int consumeAirAmount) {
      this.consumeAirAmount = Math.max(consumeAirAmount, 0);
   }

   public int getRefillAirAmount() {
      return this.refillAirAmount;
   }

   public void setRefillAirAmount(int refillAirAmount) {
      this.refillAirAmount = Math.max(refillAirAmount, 0);
   }
}
