package net.neoforged.neoforge.transfer;

import java.util.Objects;
import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class RangedResourceHandler<T extends Resource> extends DelegatingResourceHandler<T> {
   protected int start;
   protected int end;

   public static <T extends Resource> RangedResourceHandler<T> of(ResourceHandler<T> delegate, int start, int end) {
      return new RangedResourceHandler<>(delegate, start, end);
   }

   public static <T extends Resource> RangedResourceHandler<T> of(Supplier<ResourceHandler<T>> delegate, int start, int end) {
      return new RangedResourceHandler<>(delegate, start, end);
   }

   public static <T extends Resource> RangedResourceHandler<T> ofSingleIndex(ResourceHandler<T> delegate, int index) {
      return new RangedResourceHandler<>(delegate, index, index + 1);
   }

   public static <T extends Resource> RangedResourceHandler<T> ofSingleIndex(Supplier<ResourceHandler<T>> delegate, int index) {
      return new RangedResourceHandler<>(delegate, index, index + 1);
   }

   protected RangedResourceHandler(ResourceHandler<T> delegate, int start, int end) {
      this(() -> delegate, start, end);
   }

   protected RangedResourceHandler(Supplier<ResourceHandler<T>> delegate, int start, int end) {
      super(delegate);
      if (start >= 0 && start < end) {
         int delegateSize = delegate.get().size();
         if (end > delegateSize) {
            throw new IndexOutOfBoundsException("Invalid range: end " + end + " is larger than the size of the handler " + delegateSize);
         } else {
            this.start = start;
            this.end = end;
         }
      } else {
         throw new IndexOutOfBoundsException("Invalid range: start=" + start + ", end=" + end);
      }
   }

   @Override
   public int size() {
      return this.end - this.start;
   }

   @Override
   protected int convertIndex(int index) {
      Objects.checkIndex(index, this.size());
      return index + this.start;
   }

   @Override
   public int extract(T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int extracted = 0;
      ResourceHandler<T> handler = this.getDelegate();

      for (int index = this.start; index < this.end; index++) {
         extracted += handler.extract(index, resource, amount - extracted, transaction);
         if (extracted == amount) {
            return extracted;
         }
      }

      return extracted;
   }

   @Override
   public int insert(T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int inserted = 0;
      ResourceHandler<T> handler = this.getDelegate();

      for (int index = this.start; index < this.end; index++) {
         inserted += handler.insert(index, resource, amount - inserted, transaction);
         if (inserted == amount) {
            return inserted;
         }
      }

      return inserted;
   }
}
