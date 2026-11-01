package net.neoforged.neoforge.transfer;

import java.util.SequencedCollection;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class CombinedResourceHandler<T extends Resource> implements ResourceHandler<T> {
   private final ResourceHandler<T>[] handlers;
   private final int[] baseIndex;
   private final int sizeCache;

   public CombinedResourceHandler(SequencedCollection<? extends ResourceHandler<T>> handlers) {
      this(handlers.toArray(ResourceHandler[]::new));
   }

   @SafeVarargs
   public CombinedResourceHandler(ResourceHandler<T>... handlers) {
      this.handlers = handlers;
      this.baseIndex = new int[handlers.length];
      int index = 0;

      for (int i = 0; i < handlers.length; i++) {
         this.baseIndex[i] = index;
         index += handlers[i].size();
      }

      this.sizeCache = index;
   }

   protected int getHandlerIndex(int index) {
      if (index >= 0 && index < this.sizeCache) {
         for (int handlerIndex = 0; handlerIndex < this.baseIndex.length - 1; handlerIndex++) {
            if (index < this.baseIndex[handlerIndex + 1]) {
               return handlerIndex;
            }
         }

         return this.baseIndex.length - 1;
      } else {
         throw new IndexOutOfBoundsException("Index " + index + " is out-of-bounds for combined handler with size " + this.sizeCache);
      }
   }

   protected ResourceHandler<T> getHandlerFromIndex(int handlerIndex) {
      return this.handlers[handlerIndex];
   }

   protected int getSlotFromIndex(int index, int handlerIndex) {
      return index - this.baseIndex[handlerIndex];
   }

   @Override
   public final int size() {
      return this.sizeCache;
   }

   @Override
   public T getResource(int index) {
      int handlerIndex = this.getHandlerIndex(index);
      return this.getHandlerFromIndex(handlerIndex).getResource(this.getSlotFromIndex(index, handlerIndex));
   }

   @Override
   public long getAmountAsLong(int index) {
      int handlerIndex = this.getHandlerIndex(index);
      return this.getHandlerFromIndex(handlerIndex).getAmountAsLong(this.getSlotFromIndex(index, handlerIndex));
   }

   @Override
   public long getCapacityAsLong(int index, T resource) {
      int handlerIndex = this.getHandlerIndex(index);
      return this.getHandlerFromIndex(handlerIndex).getCapacityAsLong(this.getSlotFromIndex(index, handlerIndex), resource);
   }

   @Override
   public boolean isValid(int index, T resource) {
      int handlerIndex = this.getHandlerIndex(index);
      return resource.isEmpty() ? true : this.getHandlerFromIndex(handlerIndex).isValid(this.getSlotFromIndex(index, handlerIndex), resource);
   }

   @Override
   public int insert(int index, T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int handlerIndex = this.getHandlerIndex(index);
      return this.getHandlerFromIndex(handlerIndex).insert(this.getSlotFromIndex(index, handlerIndex), resource, amount, transaction);
   }

   @Override
   public int insert(T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int inserted = 0;

      for (ResourceHandler<T> resourceHandler : this.handlers) {
         inserted += resourceHandler.insert(resource, amount - inserted, transaction);
         if (inserted == amount) {
            break;
         }
      }

      return inserted;
   }

   @Override
   public int extract(int index, T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int handlerIndex = this.getHandlerIndex(index);
      return this.getHandlerFromIndex(handlerIndex).extract(this.getSlotFromIndex(index, handlerIndex), resource, amount, transaction);
   }

   @Override
   public int extract(T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int extracted = 0;

      for (ResourceHandler<T> resourceHandler : this.handlers) {
         extracted += resourceHandler.extract(resource, amount - extracted, transaction);
         if (extracted == amount) {
            break;
         }
      }

      return extracted;
   }
}
