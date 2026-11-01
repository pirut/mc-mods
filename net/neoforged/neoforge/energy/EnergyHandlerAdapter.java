package net.neoforged.neoforge.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
class EnergyHandlerAdapter implements IEnergyStorage {
   private final EnergyHandler handler;

   EnergyHandlerAdapter(EnergyHandler handler) {
      this.handler = handler;
   }

   @Override
   public int receiveEnergy(int toReceive, boolean simulate) {
      int var5;
      try (Transaction tx = Transaction.openRoot()) {
         int inserted = this.handler.insert(toReceive, tx);
         if (!simulate) {
            tx.commit();
         }

         var5 = inserted;
      }

      return var5;
   }

   @Override
   public int extractEnergy(int toExtract, boolean simulate) {
      int var5;
      try (Transaction tx = Transaction.openRoot()) {
         int extracted = this.handler.extract(toExtract, tx);
         if (!simulate) {
            tx.commit();
         }

         var5 = extracted;
      }

      return var5;
   }

   @Override
   public int getEnergyStored() {
      return this.handler.getAmountAsInt();
   }

   @Override
   public int getMaxEnergyStored() {
      return this.handler.getCapacityAsInt();
   }

   @Override
   public boolean canExtract() {
      return this.handler.getCapacityAsLong() > 0L;
   }

   @Override
   public boolean canReceive() {
      return this.handler.getCapacityAsLong() > 0L;
   }
}
