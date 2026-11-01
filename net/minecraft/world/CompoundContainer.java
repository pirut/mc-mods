package net.minecraft.world;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CompoundContainer implements Container {
   private final Container container1;
   private final Container container2;

   public CompoundContainer(Container container1, Container container2) {
      this.container1 = container1;
      this.container2 = container2;
   }

   @Override
   public int getContainerSize() {
      return this.container1.getContainerSize() + this.container2.getContainerSize();
   }

   @Override
   public boolean isEmpty() {
      return this.container1.isEmpty() && this.container2.isEmpty();
   }

   public boolean contains(Container inventory) {
      return this.container1 == inventory || this.container2 == inventory;
   }

   @Override
   public ItemStack getItem(int slot) {
      return slot >= this.container1.getContainerSize() ? this.container2.getItem(slot - this.container1.getContainerSize()) : this.container1.getItem(slot);
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      return slot >= this.container1.getContainerSize()
         ? this.container2.removeItem(slot - this.container1.getContainerSize(), amount)
         : this.container1.removeItem(slot, amount);
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      return slot >= this.container1.getContainerSize()
         ? this.container2.removeItemNoUpdate(slot - this.container1.getContainerSize())
         : this.container1.removeItemNoUpdate(slot);
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      if (slot >= this.container1.getContainerSize()) {
         this.container2.setItem(slot - this.container1.getContainerSize(), stack);
      } else {
         this.container1.setItem(slot, stack);
      }
   }

   @Override
   public int getMaxStackSize() {
      return this.container1.getMaxStackSize();
   }

   @Override
   public void setChanged() {
      this.container1.setChanged();
      this.container2.setChanged();
   }

   @Override
   public boolean stillValid(Player player) {
      return this.container1.stillValid(player) && this.container2.stillValid(player);
   }

   @Override
   public void startOpen(ContainerUser user) {
      this.container1.startOpen(user);
      this.container2.startOpen(user);
   }

   @Override
   public void stopOpen(ContainerUser user) {
      this.container1.stopOpen(user);
      this.container2.stopOpen(user);
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return slot >= this.container1.getContainerSize()
         ? this.container2.canPlaceItem(slot - this.container1.getContainerSize(), stack)
         : this.container1.canPlaceItem(slot, stack);
   }

   @Override
   public void clearContent() {
      this.container1.clearContent();
      this.container2.clearContent();
   }
}
