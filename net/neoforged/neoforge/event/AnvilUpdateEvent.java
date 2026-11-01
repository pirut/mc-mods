package net.neoforged.neoforge.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public class AnvilUpdateEvent extends Event implements ICancellableEvent {
   private final ItemStack left;
   private final ItemStack right;
   private final String name;
   private ItemStack output;
   private long cost;
   private int materialCost;
   private final Player player;

   public AnvilUpdateEvent(ItemStack left, ItemStack right, String name, long cost, Player player) {
      this.left = left;
      this.right = right;
      this.output = ItemStack.EMPTY;
      this.name = name;
      this.player = player;
      this.setCost(cost);
      this.setMaterialCost(0);
   }

   public ItemStack getLeft() {
      return this.left;
   }

   public ItemStack getRight() {
      return this.right;
   }

   @Nullable
   public String getName() {
      return this.name;
   }

   public ItemStack getOutput() {
      return this.output;
   }

   public void setOutput(ItemStack output) {
      this.output = output;
   }

   public long getCost() {
      return this.cost;
   }

   public void setCost(long cost) {
      this.cost = cost;
   }

   public int getMaterialCost() {
      return this.materialCost;
   }

   public void setMaterialCost(int materialCost) {
      this.materialCost = materialCost;
   }

   public Player getPlayer() {
      return this.player;
   }
}
