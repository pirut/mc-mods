package net.neoforged.neoforge.fluids.capability.templates;

import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidTank implements IFluidHandler, IFluidTank {
   protected Predicate<FluidStack> validator;
   protected FluidStack fluid = FluidStack.EMPTY;
   protected int capacity;

   public FluidTank(int capacity) {
      this(capacity, e -> true);
   }

   public FluidTank(int capacity, Predicate<FluidStack> validator) {
      this.capacity = capacity;
      this.validator = validator;
   }

   public FluidTank setCapacity(int capacity) {
      this.capacity = capacity;
      return this;
   }

   public FluidTank setValidator(Predicate<FluidStack> validator) {
      if (validator != null) {
         this.validator = validator;
      }

      return this;
   }

   @Override
   public boolean isFluidValid(FluidStack stack) {
      return this.validator.test(stack);
   }

   @Override
   public int getCapacity() {
      return this.capacity;
   }

   @Override
   public FluidStack getFluid() {
      return this.fluid;
   }

   @Override
   public int getFluidAmount() {
      return this.fluid.getAmount();
   }

   public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
      this.fluid = FluidStack.parseOptional(lookupProvider, nbt.getCompound("Fluid"));
      return this;
   }

   public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
      if (!this.fluid.isEmpty()) {
         nbt.put("Fluid", this.fluid.save(lookupProvider));
      }

      return nbt;
   }

   @Override
   public int getTanks() {
      return 1;
   }

   @Override
   public FluidStack getFluidInTank(int tank) {
      return this.getFluid();
   }

   @Override
   public int getTankCapacity(int tank) {
      return this.getCapacity();
   }

   @Override
   public boolean isFluidValid(int tank, FluidStack stack) {
      return this.isFluidValid(stack);
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      if (resource.isEmpty() || !this.isFluidValid(resource)) {
         return 0;
      } else if (action.simulate()) {
         if (this.fluid.isEmpty()) {
            return Math.min(this.capacity, resource.getAmount());
         } else {
            return !FluidStack.isSameFluidSameComponents(this.fluid, resource) ? 0 : Math.min(this.capacity - this.fluid.getAmount(), resource.getAmount());
         }
      } else if (this.fluid.isEmpty()) {
         this.fluid = resource.copyWithAmount(Math.min(this.capacity, resource.getAmount()));
         this.onContentsChanged();
         return this.fluid.getAmount();
      } else if (!FluidStack.isSameFluidSameComponents(this.fluid, resource)) {
         return 0;
      } else {
         int filled = this.capacity - this.fluid.getAmount();
         if (resource.getAmount() < filled) {
            this.fluid.grow(resource.getAmount());
            filled = resource.getAmount();
         } else {
            this.fluid.setAmount(this.capacity);
         }

         if (filled > 0) {
            this.onContentsChanged();
         }

         return filled;
      }
   }

   @Override
   public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      return !resource.isEmpty() && FluidStack.isSameFluidSameComponents(resource, this.fluid) ? this.drain(resource.getAmount(), action) : FluidStack.EMPTY;
   }

   @Override
   public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      int drained = maxDrain;
      if (this.fluid.getAmount() < maxDrain) {
         drained = this.fluid.getAmount();
      }

      FluidStack stack = this.fluid.copyWithAmount(drained);
      if (action.execute() && drained > 0) {
         this.fluid.shrink(drained);
         this.onContentsChanged();
      }

      return stack;
   }

   protected void onContentsChanged() {
   }

   public void setFluid(FluidStack stack) {
      this.fluid = stack;
   }

   public boolean isEmpty() {
      return this.fluid.isEmpty();
   }

   public int getSpace() {
      return Math.max(0, this.capacity - this.fluid.getAmount());
   }
}
