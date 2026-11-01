package net.neoforged.neoforge.event.entity.living;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

public class LivingShieldBlockEvent extends LivingEvent implements ICancellableEvent {
   private final DamageContainer container;
   private float dmgBlocked;
   private float shieldDamage = -1.0F;
   private final boolean originalBlocked;
   private boolean newBlocked;

   public LivingShieldBlockEvent(LivingEntity blocker, DamageContainer container, boolean originalBlockedState) {
      super(blocker);
      this.container = container;
      this.dmgBlocked = container.getNewDamage();
      this.originalBlocked = originalBlockedState;
      this.newBlocked = originalBlockedState;
      this.shieldDamage = container.getNewDamage();
   }

   public DamageContainer getDamageContainer() {
      return this.container;
   }

   public DamageSource getDamageSource() {
      return this.getDamageContainer().getSource();
   }

   public float getOriginalBlockedDamage() {
      return this.getDamageContainer().getNewDamage();
   }

   public float getBlockedDamage() {
      return Math.min(this.dmgBlocked, this.container.getNewDamage());
   }

   public float shieldDamage() {
      if (this.newBlocked) {
         return this.shieldDamage >= 0.0F ? this.shieldDamage : this.getBlockedDamage();
      } else {
         return 0.0F;
      }
   }

   public void setBlockedDamage(float blocked) {
      this.dmgBlocked = Mth.clamp(blocked, 0.0F, this.getOriginalBlockedDamage());
   }

   public void setShieldDamage(float damage) {
      this.shieldDamage = damage;
   }

   public boolean getOriginalBlock() {
      return this.originalBlocked;
   }

   public boolean getBlocked() {
      return this.newBlocked;
   }

   public void setBlocked(boolean isBlocked) {
      this.newBlocked = isBlocked;
   }
}
