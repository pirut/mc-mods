package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;

public class SweepAttackEvent extends PlayerEvent implements ICancellableEvent {
   private final Entity target;
   private final boolean isVanillaSweep;
   private boolean isSweeping;

   public SweepAttackEvent(Player player, Entity target, boolean isVanillaSweep) {
      super(player);
      this.target = target;
      this.isSweeping = this.isVanillaSweep = isVanillaSweep;
   }

   public Entity getTarget() {
      return this.target;
   }

   public boolean isVanillaSweep() {
      return this.isVanillaSweep;
   }

   public boolean isSweeping() {
      return this.isSweeping;
   }

   public void setSweeping(boolean sweep) {
      this.isSweeping = sweep;
   }

   public void setCanceled(boolean canceled) {
      super.setCanceled(canceled);
   }
}
