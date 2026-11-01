package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class PlayerDestroyItemEvent extends PlayerEvent {
   private final ItemStack original;
   @Nullable
   private final InteractionHand hand;

   public PlayerDestroyItemEvent(Player player, ItemStack original, @Nullable InteractionHand hand) {
      super(player);
      this.original = original;
      this.hand = hand;
   }

   public ItemStack getOriginal() {
      return this.original;
   }

   @Nullable
   public InteractionHand getHand() {
      return this.hand;
   }
}
