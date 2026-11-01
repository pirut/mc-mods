package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;

public class ArrowNockEvent extends PlayerEvent implements ICancellableEvent {
   private final ItemStack bow;
   private final InteractionHand hand;
   private final Level level;
   private final boolean hasAmmo;
   private InteractionResult action;

   public ArrowNockEvent(Player player, ItemStack item, InteractionHand hand, Level level, boolean hasAmmo) {
      super(player);
      this.bow = item;
      this.hand = hand;
      this.level = level;
      this.hasAmmo = hasAmmo;
   }

   public ItemStack getBow() {
      return this.bow;
   }

   public Level getLevel() {
      return this.level;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public boolean hasAmmo() {
      return this.hasAmmo;
   }

   public InteractionResult getAction() {
      return this.action;
   }

   public void setAction(InteractionResult action) {
      this.action = action;
   }
}
