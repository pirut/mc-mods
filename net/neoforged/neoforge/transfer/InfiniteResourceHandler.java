package net.neoforged.neoforge.transfer;

import java.util.Objects;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class InfiniteResourceHandler<T extends Resource> implements ResourceHandler<T> {
   protected final T infiniteResource;

   public InfiniteResourceHandler(T infiniteResource) {
      TransferPreconditions.checkNonEmpty(infiniteResource);
      this.infiniteResource = infiniteResource;
   }

   @Override
   public int size() {
      return 1;
   }

   @Override
   public int insert(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return resource.equals(this.infiniteResource) ? amount : 0;
   }

   @Override
   public int extract(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return resource.equals(this.infiniteResource) ? amount : 0;
   }

   @Override
   public T getResource(int index) {
      Objects.checkIndex(index, this.size());
      return this.infiniteResource;
   }

   @Override
   public long getAmountAsLong(int index) {
      Objects.checkIndex(index, this.size());
      return Long.MAX_VALUE;
   }

   @Override
   public long getCapacityAsLong(int index, T resource) {
      Objects.checkIndex(index, this.size());
      return Long.MAX_VALUE;
   }

   @Override
   public boolean isValid(int index, T resource) {
      Objects.checkIndex(index, this.size());
      return resource.equals(this.infiniteResource);
   }
}
