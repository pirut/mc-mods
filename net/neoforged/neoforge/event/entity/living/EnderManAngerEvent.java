package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;

public class EnderManAngerEvent extends LivingEvent implements ICancellableEvent {
   private final Player player;

   public EnderManAngerEvent(EnderMan enderman, Player player) {
      super(enderman);
      this.player = player;
   }

   public Player getPlayer() {
      return this.player;
   }

   public EnderMan getEntity() {
      return (EnderMan)super.getEntity();
   }
}
