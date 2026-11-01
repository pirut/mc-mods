package net.neoforged.neoforge.event.entity.living;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

public class LivingShieldBlockEvent extends LivingEvent {
   private final DamageContainer container;
   private float dmgBlocked;
   private final float originalDmgBlocked;
   private int shieldDamage = -1;
   private final boolean originalBlocked;
   private boolean newBlocked;

   public LivingShieldBlockEvent(LivingEntity blocker, DamageContainer container, float blockedDamage, boolean originalBlockedState) {
      super(blocker);
      this.container = container;
      this.dmgBlocked = blockedDamage;
      this.originalDmgBlocked = this.dmgBlocked;
      this.originalBlocked = originalBlockedState;
      this.newBlocked = originalBlockedState;
   }

   public DamageContainer getDamageContainer() {
      return this.container;
   }

   public DamageSource getDamageSource() {
      return this.getDamageContainer().getSource();
   }

   public float getOriginalBlockedDamage() {
      return this.originalDmgBlocked;
   }

   public float getBlockedDamage() {
      return Math.min(this.dmgBlocked, this.container.getNewDamage());
   }

   public int shieldDamage() {
      return this.newBlocked ? this.shieldDamage : 0;
   }

   public void setBlockedDamage(float blocked) {
      this.dmgBlocked = Mth.clamp(blocked, 0.0F, this.container.getNewDamage());
   }

   public void setShieldDamage(int damage) {
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
