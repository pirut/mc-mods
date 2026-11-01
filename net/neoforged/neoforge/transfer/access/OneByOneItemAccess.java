package net.neoforged.neoforge.transfer.access;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

record OneByOneItemAccess(ItemAccess delegate) implements ItemAccess {
   @Override
   public ItemResource getResource() {
      return this.delegate.getResource();
   }

   @Override
   public int getAmount() {
      return Math.min(1, this.delegate.getAmount());
   }

   @Override
   public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      return this.delegate.insert(resource, Math.min(amount, 1), transaction);
   }

   @Override
   public int extract(ItemResource resource, int amount, TransactionContext transaction) {
      return this.delegate.extract(resource, Math.min(amount, 1), transaction);
   }
}
