package net.neoforged.neoforge.transfer.energy;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class EmptyEnergyHandler implements EnergyHandler {
   public static final EmptyEnergyHandler INSTANCE = new EmptyEnergyHandler();

   private EmptyEnergyHandler() {
   }

   @Override
   public long getAmountAsLong() {
      return 0L;
   }

   @Override
   public long getCapacityAsLong() {
      return 0L;
   }

   @Override
   public int insert(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      return 0;
   }

   @Override
   public int extract(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      return 0;
   }
}
