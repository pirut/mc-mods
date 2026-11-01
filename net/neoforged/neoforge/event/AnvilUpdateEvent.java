package net.neoforged.neoforge.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

public class AnvilUpdateEvent extends Event implements ICancellableEvent {
   private final ItemStack left;
   private final ItemStack right;
   @Nullable
   private final String name;
   private final AnvilUpdateEvent.VanillaResult vanillaResult;
   private final Player player;
   private ItemStack output;
   private int xpCost;
   private int materialCost;

   public AnvilUpdateEvent(ItemStack left, ItemStack right, @Nullable String name, ItemStack result, int xpCost, int materialCost, Player player) {
      this.left = left;
      this.right = right;
      this.name = name;
      this.vanillaResult = new AnvilUpdateEvent.VanillaResult(result, xpCost, materialCost);
      this.player = player;
      this.output = result.copy();
      this.xpCost = xpCost;
      this.materialCost = materialCost;
   }

   public ItemStack getLeft() {
      return this.left.copy();
   }

   public ItemStack getRight() {
      return this.right.copy();
   }

   @Nullable
   public String getName() {
      return this.name;
   }

   public AnvilUpdateEvent.VanillaResult getVanillaResult() {
      return this.vanillaResult;
   }

   public ItemStack getOutput() {
      return this.output;
   }

   public void setOutput(ItemStack output) {
      this.output = output;
   }

   public int getXpCost() {
      return this.xpCost;
   }

   public void setXpCost(int xpCost) {
      this.xpCost = xpCost;
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

   public void setCanceled(boolean canceled) {
      super.setCanceled(canceled);
   }

   public record VanillaResult(ItemStack output, int xpCost, int materialCost) {
      public ItemStack output() {
         return this.output.copy();
      }
   }
}
