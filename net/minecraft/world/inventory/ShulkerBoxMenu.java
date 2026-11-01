package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShulkerBoxMenu extends AbstractContainerMenu {
   private static final int CONTAINER_SIZE = 27;
   private final Container container;

   public ShulkerBoxMenu(int containerId, Inventory playerInventory) {
      this(containerId, playerInventory, new SimpleContainer(27));
   }

   public ShulkerBoxMenu(int containerId, Inventory playerInventory, Container container) {
      super(MenuType.SHULKER_BOX, containerId);
      checkContainerSize(container, 27);
      this.container = container;
      container.startOpen(playerInventory.player);
      int i = 3;
      int j = 9;

      for (int k = 0; k < 3; k++) {
         for (int l = 0; l < 9; l++) {
            this.addSlot(new ShulkerBoxSlot(container, l + k * 9, 8 + l * 18, 18 + k * 18));
         }
      }

      this.addStandardInventorySlots(playerInventory, 8, 84);
   }

   @Override
   public boolean stillValid(Player player) {
      return this.container.stillValid(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot = this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack itemStack2 = slot.getItem();
         itemStack = itemStack2.copy();
         if (index < this.container.getContainerSize()) {
            if (!this.moveItemStackTo(itemStack2, this.container.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemStack2, 0, this.container.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }

         if (itemStack2.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return itemStack;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }
}
