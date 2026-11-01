package net.neoforged.neoforge.transfer.energy;

import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class LimitingEnergyHandler extends DelegatingEnergyHandler {
   protected int maxInsert;
   protected int maxExtract;

   public LimitingEnergyHandler(EnergyHandler delegate, int maxInsert, int maxExtract) {
      this(() -> delegate, maxInsert, maxExtract);
   }

   public LimitingEnergyHandler(Supplier<EnergyHandler> delegate, int maxInsert, int maxExtract) {
      super(delegate);
      TransferPreconditions.checkNonNegative(maxInsert);
      TransferPreconditions.checkNonNegative(maxExtract);
      this.maxInsert = maxInsert;
      this.maxExtract = maxExtract;
   }

   @Override
   public int insert(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      int toInsert = Math.min(amount, this.maxInsert);
      return toInsert <= 0 ? 0 : super.insert(toInsert, transaction);
   }

   @Override
   public int extract(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      int toExtract = Math.min(amount, this.maxExtract);
      return toExtract <= 0 ? 0 : super.extract(toExtract, transaction);
   }
}
