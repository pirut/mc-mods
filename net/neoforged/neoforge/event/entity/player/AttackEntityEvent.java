package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;

public class AttackEntityEvent extends PlayerEvent implements ICancellableEvent {
   private final Entity target;

   public AttackEntityEvent(Player player, Entity target) {
      super(player);
      this.target = target;
   }

   public Entity getTarget() {
      return this.target;
   }
}
