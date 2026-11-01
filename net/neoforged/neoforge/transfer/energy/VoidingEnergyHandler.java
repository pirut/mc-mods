package net.neoforged.neoforge.transfer.energy;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class VoidingEnergyHandler implements EnergyHandler {
   public static final VoidingEnergyHandler INSTANCE = new VoidingEnergyHandler();

   @Override
   public long getAmountAsLong() {
      return 0L;
   }

   @Override
   public long getCapacityAsLong() {
      return Long.MAX_VALUE;
   }

   @Override
   public int insert(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      return amount;
   }

   @Override
   public int extract(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      return 0;
   }
}
