package net.neoforged.neoforge.transfer;

import com.google.common.primitives.Ints;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface ResourceHandler<T extends Resource> {
   int size();

   T getResource(int var1);

   long getAmountAsLong(int var1);

   @NonExtendable
   default int getAmountAsInt(int index) {
      return Ints.saturatedCast(this.getAmountAsLong(index));
   }

   long getCapacityAsLong(int var1, T var2);

   @NonExtendable
   default int getCapacityAsInt(int index, T resource) {
      return Ints.saturatedCast(this.getCapacityAsLong(index, resource));
   }

   boolean isValid(int var1, T var2);

   int insert(int var1, T var2, int var3, TransactionContext var4);

   default int insert(T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int inserted = 0;
      int size = this.size();

      for (int index = 0; index < size; index++) {
         inserted += this.insert(index, resource, amount - inserted, transaction);
         if (inserted == amount) {
            break;
         }
      }

      return inserted;
   }

   int extract(int var1, T var2, int var3, TransactionContext var4);

   default int extract(T resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int extracted = 0;
      int size = this.size();

      for (int index = 0; index < size; index++) {
         extracted += this.extract(index, resource, amount - extracted, transaction);
         if (extracted == amount) {
            break;
         }
      }

      return extracted;
   }

   static <T extends Resource> Class<ResourceHandler<T>> asClass() {
      return ResourceHandler.class;
   }
}
