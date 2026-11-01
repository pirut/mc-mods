package net.neoforged.neoforge.event.entity.player;

import net.minecraft.util.TriState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public abstract class ItemEntityPickupEvent extends Event {
   private final Player player;
   private final ItemEntity item;

   public ItemEntityPickupEvent(Player player, ItemEntity item) {
      this.player = player;
      this.item = item;
   }

   public Player getPlayer() {
      return this.player;
   }

   public ItemEntity getItemEntity() {
      return this.item;
   }

   public static class Post extends ItemEntityPickupEvent {
      private final ItemStack originalStack;

      public Post(Player player, ItemEntity item, ItemStack originalStack) {
         super(player, item);
         this.originalStack = originalStack;
      }

      public ItemStack getOriginalStack() {
         return this.originalStack.copy();
      }

      public ItemStack getCurrentStack() {
         return this.getItemEntity().getItem();
      }
   }

   public static class Pre extends ItemEntityPickupEvent {
      private TriState canPickup = TriState.DEFAULT;

      public Pre(Player player, ItemEntity item) {
         super(player, item);
      }

      public void setCanPickup(TriState state) {
         this.canPickup = state;
      }

      public TriState canPickup() {
         return this.canPickup;
      }
   }
}
