package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class AnvilCraftEvent extends PlayerEvent {
   private final AnvilMenu menu;
   private final ItemStack left;
   private final ItemStack right;
   private final ItemStack output;

   public AnvilCraftEvent(AnvilMenu menu, Player player, ItemStack left, ItemStack right, ItemStack output) {
      super(player);
      this.menu = menu;
      this.output = output;
      this.left = left;
      this.right = right;
   }

   public AnvilMenu getMenu() {
      return this.menu;
   }

   public ItemStack getOutput() {
      return this.output.copy();
   }

   public ItemStack getLeft() {
      return this.left.copy();
   }

   public ItemStack getRight() {
      return this.right.copy();
   }

   public static class Post extends AnvilCraftEvent {
      public Post(AnvilMenu menu, Player player, ItemStack left, ItemStack right, ItemStack output) {
         super(menu, player, left, right, output);
      }
   }

   public static class Pre extends AnvilCraftEvent implements ICancellableEvent {
      public Pre(AnvilMenu menu, Player player, ItemStack left, ItemStack right, ItemStack output) {
         super(menu, player, left, right, output);
      }

      public void setCanceled(boolean canceled) {
         super.setCanceled(canceled);
      }
   }
}
