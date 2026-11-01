package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;

public class LivingDrownEvent extends LivingEvent implements ICancellableEvent {
   private boolean isDrowning;
   private float damageAmount;
   private int bubbleCount;

   public LivingDrownEvent(LivingEntity entity, boolean isDrowning, float damageAmount, int bubbleCount) {
      super(entity);
      this.isDrowning = isDrowning;
      this.damageAmount = damageAmount;
      this.bubbleCount = bubbleCount;
   }

   public LivingDrownEvent(LivingEntity entity) {
      this(entity, entity.getAirSupply() <= -20, 2.0F, 8);
   }

   public boolean isDrowning() {
      return this.isDrowning;
   }

   public void setDrowning(boolean isDrowning) {
      this.isDrowning = isDrowning;
   }

   public float getDamageAmount() {
      return this.damageAmount;
   }

   public void setDamageAmount(float damageAmount) {
      this.damageAmount = damageAmount;
   }

   public int getBubbleCount() {
      return this.bubbleCount;
   }

   public void setBubbleCount(int bubbleCount) {
      this.bubbleCount = bubbleCount;
   }

   public void setCanceled(boolean cancel) {
      super.setCanceled(cancel);
   }
}
