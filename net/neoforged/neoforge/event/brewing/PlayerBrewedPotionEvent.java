package net.neoforged.neoforge.event.brewing;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerBrewedPotionEvent extends PlayerEvent {
   private final ItemStack stack;

   public PlayerBrewedPotionEvent(Player player, ItemStack stack) {
      super(player);
      this.stack = stack;
   }

   public ItemStack getStack() {
      return this.stack;
   }
}
