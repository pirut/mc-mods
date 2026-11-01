package net.neoforged.neoforge.transfer.energy;

import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class DelegatingEnergyHandler implements EnergyHandler {
   protected final Supplier<EnergyHandler> delegate;

   public DelegatingEnergyHandler(EnergyHandler delegate) {
      this.delegate = () -> delegate;
   }

   public DelegatingEnergyHandler(Supplier<EnergyHandler> delegate) {
      this.delegate = delegate;
   }

   @Override
   public long getAmountAsLong() {
      return this.getDelegate().getAmountAsLong();
   }

   @Override
   public long getCapacityAsLong() {
      return this.getDelegate().getCapacityAsLong();
   }

   @Override
   public int insert(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      return this.getDelegate().insert(amount, transaction);
   }

   @Override
   public int extract(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      return this.getDelegate().extract(amount, transaction);
   }

   public EnergyHandler getDelegate() {
      return this.delegate.get();
   }
}
