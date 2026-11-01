package net.neoforged.neoforge.energy;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class ComponentEnergyStorage implements IEnergyStorage {
   protected final MutableDataComponentHolder parent;
   protected final DataComponentType<Integer> energyComponent;
   protected final int capacity;
   protected final int maxReceive;
   protected final int maxExtract;

   public ComponentEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxReceive, int maxExtract) {
      this.parent = parent;
      this.energyComponent = energyComponent;
      this.capacity = capacity;
      this.maxReceive = maxReceive;
      this.maxExtract = maxExtract;
   }

   public ComponentEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
      this(parent, energyComponent, capacity, maxTransfer, maxTransfer);
   }

   public ComponentEnergyStorage(MutableDataComponentHolder parent, DataComponentType<Integer> energyComponent, int capacity) {
      this(parent, energyComponent, capacity, capacity);
   }

   @Override
   public int receiveEnergy(int toReceive, boolean simulate) {
      if (this.canReceive() && toReceive > 0) {
         int energy = this.getEnergyStored();
         int energyReceived = Mth.clamp(this.capacity - energy, 0, Math.min(this.maxReceive, toReceive));
         if (!simulate && energyReceived > 0) {
            this.setEnergy(energy + energyReceived);
         }

         return energyReceived;
      } else {
         return 0;
      }
   }

   @Override
   public int extractEnergy(int toExtract, boolean simulate) {
      if (this.canExtract() && toExtract > 0) {
         int energy = this.getEnergyStored();
         int energyExtracted = Math.min(energy, Math.min(this.maxExtract, toExtract));
         if (!simulate && energyExtracted > 0) {
            this.setEnergy(energy - energyExtracted);
         }

         return energyExtracted;
      } else {
         return 0;
      }
   }

   @Override
   public int getEnergyStored() {
      int rawEnergy = this.parent.getOrDefault(this.energyComponent, 0);
      return Mth.clamp(rawEnergy, 0, this.capacity);
   }

   @Override
   public int getMaxEnergyStored() {
      return this.capacity;
   }

   @Override
   public boolean canExtract() {
      return this.maxExtract > 0;
   }

   @Override
   public boolean canReceive() {
      return this.maxReceive > 0;
   }

   protected void setEnergy(int energy) {
      int realEnergy = Mth.clamp(energy, 0, this.capacity);
      this.parent.set(this.energyComponent, realEnergy);
   }
}
