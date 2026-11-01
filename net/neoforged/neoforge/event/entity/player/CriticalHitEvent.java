package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CriticalHitEvent extends PlayerEvent {
   private final Entity target;
   private final float vanillaDmgMultiplier;
   private final boolean isVanillaCritical;
   private float dmgMultiplier;
   private boolean isCriticalHit;
   private boolean disableSweep = true;

   public CriticalHitEvent(Player player, Entity target, float dmgMultiplier, boolean isCriticalHit) {
      super(player);
      this.target = target;
      this.dmgMultiplier = this.vanillaDmgMultiplier = dmgMultiplier;
      this.isCriticalHit = this.isVanillaCritical = isCriticalHit;
   }

   public Entity getTarget() {
      return this.target;
   }

   public float getDamageMultiplier() {
      return this.dmgMultiplier;
   }

   public void setDamageMultiplier(float dmgMultiplier) {
      if (dmgMultiplier < 0.0F) {
         throw new UnsupportedOperationException("Attempted to set a negative damage multiplier: " + dmgMultiplier);
      } else {
         this.dmgMultiplier = dmgMultiplier;
      }
   }

   public boolean isCriticalHit() {
      return this.isCriticalHit;
   }

   public void setCriticalHit(boolean isCriticalHit) {
      this.isCriticalHit = isCriticalHit;
   }

   public float getVanillaMultiplier() {
      return this.vanillaDmgMultiplier;
   }

   public boolean isVanillaCritical() {
      return this.isVanillaCritical;
   }

   public void setDisableSweep(boolean disableSweep) {
      this.disableSweep = disableSweep;
   }

   public boolean disableSweep() {
      return this.disableSweep;
   }
}
