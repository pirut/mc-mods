package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractMountInventoryMenu extends AbstractContainerMenu {
   protected final Container mountContainer;
   protected final LivingEntity mount;
   protected final int SLOT_SADDLE = 0;
   protected final int SLOT_BODY_ARMOR = 1;
   protected final int SLOT_INVENTORY_START = 2;
   protected static final int INVENTORY_ROWS = 3;

   protected AbstractMountInventoryMenu(int containerId, Inventory playerInventory, Container mountContainer, LivingEntity mount) {
      super(null, containerId);
      this.mountContainer = mountContainer;
      this.mount = mount;
      mountContainer.startOpen(playerInventory.player);
   }

   protected abstract boolean hasInventoryChanged(Container inventory);

   @Override
   public boolean stillValid(Player player) {
      return !this.hasInventoryChanged(this.mountContainer)
         && this.mountContainer.stillValid(player)
         && this.mount.isAlive()
         && player.isWithinEntityInteractionRange(this.mount, 4.0);
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      this.mountContainer.stopOpen(player);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot = this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack itemStack2 = slot.getItem();
         itemStack = itemStack2.copy();
         int i = 2 + this.mountContainer.getContainerSize();
         if (index < i) {
            if (!this.moveItemStackTo(itemStack2, i, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).mayPlace(itemStack2) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).mayPlace(itemStack2) && !this.getSlot(0).hasItem()) {
            if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.mountContainer.getContainerSize() == 0 || !this.moveItemStackTo(itemStack2, 2, i, false)) {
            int j = i + 27;
            int l = j + 9;
            if (index >= j && index < l) {
               if (!this.moveItemStackTo(itemStack2, i, j, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (index >= i && index < j) {
               if (!this.moveItemStackTo(itemStack2, j, l, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemStack2, j, j, false)) {
               return ItemStack.EMPTY;
            }

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

   public static int getInventorySize(int rows) {
      return rows * 3;
   }
}
