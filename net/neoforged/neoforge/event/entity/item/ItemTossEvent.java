package net.neoforged.neoforge.event.entity.item;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;

public class ItemTossEvent extends ItemEvent implements ICancellableEvent {
   private final Player player;

   public ItemTossEvent(ItemEntity entityItem, Player player) {
      super(entityItem);
      this.player = player;
   }

   public Player getPlayer() {
      return this.player;
   }
}
