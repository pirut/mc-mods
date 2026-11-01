package net.neoforged.neoforge.transfer.access;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

class PlayerItemAccess implements ItemAccess {
   private final PlayerInventoryWrapper inventoryWrapper;
   private final ResourceHandler<ItemResource> slot;

   public PlayerItemAccess(PlayerInventoryWrapper inventoryWrapper, ResourceHandler<ItemResource> slot) {
      this.inventoryWrapper = inventoryWrapper;
      this.slot = slot;
   }

   @Override
   public ItemResource getResource() {
      return this.slot.getResource(0);
   }

   @Override
   public int getAmount() {
      return this.slot.getAmountAsInt(0);
   }

   @Override
   public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      int inserted = this.slot.insert(0, resource, amount, transaction);
      if (amount > inserted) {
         this.inventoryWrapper.placeItemBackInInventory(resource, amount - inserted, transaction);
      }

      return amount;
   }

   @Override
   public int extract(ItemResource resource, int amount, TransactionContext transaction) {
      return this.slot.extract(0, resource, amount, transaction);
   }
}
