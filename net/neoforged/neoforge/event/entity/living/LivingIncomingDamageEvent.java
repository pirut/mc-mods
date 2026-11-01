package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.damagesource.IReductionFunction;

public class LivingIncomingDamageEvent extends LivingEvent implements ICancellableEvent {
   private final DamageContainer container;

   public LivingIncomingDamageEvent(LivingEntity entity, DamageContainer container) {
      super(entity);
      this.container = container;
   }

   public DamageContainer getContainer() {
      return this.container;
   }

   public DamageSource getSource() {
      return this.container.getSource();
   }

   public float getAmount() {
      return this.container.getNewDamage();
   }

   public float getOriginalAmount() {
      return this.container.getOriginalDamage();
   }

   public void setAmount(float newDamage) {
      this.container.setNewDamage(newDamage);
   }

   public void addReductionModifier(DamageContainer.Reduction type, IReductionFunction reductionFunc) {
      this.container.addModifier(type, reductionFunc);
   }

   public void setInvulnerabilityTicks(int ticks) {
      this.container.setPostAttackInvulnerabilityTicks(ticks);
   }
}
