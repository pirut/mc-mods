package net.neoforged.neoforge.transfer.access;

import java.util.Objects;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class HandlerItemAccess implements ItemAccess {
   protected final ResourceHandler<ItemResource> handler;
   protected final int index;

   public HandlerItemAccess(ResourceHandler<ItemResource> handler, int index) {
      Objects.checkIndex(index, handler.size());
      this.handler = handler;
      this.index = index;
   }

   @Override
   public ItemResource getResource() {
      return this.handler.getResource(this.index);
   }

   @Override
   public int getAmount() {
      return this.handler.getAmountAsInt(this.index);
   }

   @Override
   public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int inserted = this.handler.insert(this.index, resource, amount, transaction);
      if (inserted < amount) {
         inserted += this.handler.insert(resource, amount - inserted, transaction);
      }

      return inserted;
   }

   @Override
   public int extract(ItemResource resource, int amount, TransactionContext transaction) {
      return this.handler.extract(this.index, resource, amount, transaction);
   }
}
