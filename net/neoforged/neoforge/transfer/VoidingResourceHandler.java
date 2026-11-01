package net.neoforged.neoforge.transfer;

import java.util.Objects;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class VoidingResourceHandler<T extends Resource> implements ResourceHandler<T> {
   protected final T emptyResource;

   public VoidingResourceHandler(T emptyResource) {
      if (!emptyResource.isEmpty()) {
         throw new IllegalArgumentException("Resource is not empty: " + emptyResource);
      } else {
         this.emptyResource = emptyResource;
      }
   }

   @Override
   public int size() {
      return 1;
   }

   @Override
   public int insert(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return amount;
   }

   @Override
   public int extract(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return 0;
   }

   @Override
   public T getResource(int index) {
      Objects.checkIndex(index, this.size());
      return this.emptyResource;
   }

   @Override
   public long getAmountAsLong(int index) {
      Objects.checkIndex(index, this.size());
      return 0L;
   }

   @Override
   public long getCapacityAsLong(int index, T resource) {
      Objects.checkIndex(index, this.size());
      return Long.MAX_VALUE;
   }

   @Override
   public boolean isValid(int index, T resource) {
      Objects.checkIndex(index, this.size());
      return true;
   }
}
