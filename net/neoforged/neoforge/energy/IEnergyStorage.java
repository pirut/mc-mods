package net.neoforged.neoforge.energy;

import java.util.Objects;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public interface IEnergyStorage {
   static IEnergyStorage of(EnergyHandler handler) {
      Objects.requireNonNull(handler, "handler");
      return new EnergyHandlerAdapter(handler);
   }

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int receiveEnergy(int var1, boolean var2);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int extractEnergy(int var1, boolean var2);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int getEnergyStored();

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int getMaxEnergyStored();

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   boolean canExtract();

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   boolean canReceive();
}
