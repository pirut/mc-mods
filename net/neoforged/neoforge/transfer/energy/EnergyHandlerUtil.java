package net.neoforged.neoforge.transfer.energy;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class EnergyHandlerUtil {
   public static boolean isFull(EnergyHandler handler) {
      return handler.getAmountAsLong() >= handler.getCapacityAsLong();
   }

   public static int getRedstoneSignalFromEnergyHandler(EnergyHandler handler) {
      long amount = handler.getAmountAsLong();
      if (amount == 0L) {
         return 0;
      } else {
         long capacity = handler.getCapacityAsLong();
         return capacity == 0L ? 0 : Mth.lerpDiscrete(Math.min(1.0F, (float)amount / (float)capacity), 0, 15);
      }
   }

   public static int move(@Nullable EnergyHandler from, @Nullable EnergyHandler to, int amount, @Nullable TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      if (from != null && to != null && amount != 0) {
         try {
            int var7;
            try (Transaction subTransaction = Transaction.open(transaction)) {
               int maxExtracted;
               try (Transaction simulatedExtract = Transaction.open(subTransaction)) {
                  maxExtracted = from.extract(amount, simulatedExtract);
               }

               if (maxExtracted == 0) {
                  return 0;
               }

               int inserted = to.insert(maxExtracted, subTransaction);
               if (inserted != from.extract(inserted, subTransaction)) {
                  return 0;
               }

               subTransaction.commit();
               var7 = inserted;
            }

            return var7;
         } catch (Exception var13) {
            CrashReport report = CrashReport.forThrowable(var13, "Moving energy between handlers");
            report.addCategory("Move details")
               .setDetail("Input", from::toString)
               .setDetail("Output", to::toString)
               .setDetail("Amount", amount)
               .setDetail("Transaction", transaction);
            throw new ReportedException(report);
         }
      } else {
         return 0;
      }
   }

   private EnergyHandlerUtil() {
   }
}
