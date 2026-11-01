package net.neoforged.neoforge.transfer.energy;

import com.google.common.primitives.Ints;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface EnergyHandler {
   long getAmountAsLong();

   @NonExtendable
   default int getAmountAsInt() {
      return Ints.saturatedCast(this.getAmountAsLong());
   }

   long getCapacityAsLong();

   @NonExtendable
   default int getCapacityAsInt() {
      return Ints.saturatedCast(this.getCapacityAsLong());
   }

   int insert(int var1, TransactionContext var2);

   int extract(int var1, TransactionContext var2);
}
