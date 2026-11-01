package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AnvilRepairEvent extends PlayerEvent {
   private final ItemStack left;
   private final ItemStack right;
   private final ItemStack output;
   private float breakChance;

   public AnvilRepairEvent(Player player, ItemStack left, ItemStack right, ItemStack output) {
      super(player);
      this.output = output;
      this.left = left;
      this.right = right;
      this.setBreakChance(0.12F);
   }

   public ItemStack getOutput() {
      return this.output;
   }

   public ItemStack getLeft() {
      return this.left;
   }

   public ItemStack getRight() {
      return this.right;
   }

   public float getBreakChance() {
      return this.breakChance;
   }

   public void setBreakChance(float breakChance) {
      this.breakChance = breakChance;
   }
}
