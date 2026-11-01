package net.neoforged.neoforge.fluids.capability.wrappers;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class FluidBucketWrapper implements IFluidHandlerItem {
   protected ItemStack container;

   public FluidBucketWrapper(ItemStack container) {
      this.container = container;
   }

   @Override
   public ItemStack getContainer() {
      return this.container;
   }

   public boolean canFillFluidType(FluidStack fluid) {
      return !fluid.is(Fluids.WATER) && !fluid.is(Fluids.LAVA) ? !fluid.getFluidType().getBucket(fluid).isEmpty() : true;
   }

   public FluidStack getFluid() {
      Item item = this.container.getItem();
      if (item instanceof BucketItem) {
         return new FluidStack(((BucketItem)item).content, 1000);
      } else {
         return item == Items.MILK_BUCKET && NeoForgeMod.MILK.isBound() ? new FluidStack(NeoForgeMod.MILK.get(), 1000) : FluidStack.EMPTY;
      }
   }

   protected void setFluid(FluidStack fluidStack) {
      if (fluidStack.isEmpty()) {
         this.container = new ItemStack(Items.BUCKET);
      } else {
         this.container = FluidUtil.getFilledBucket(fluidStack);
      }
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
      return 1000;
   }

   @Override
   public boolean isFluidValid(int tank, FluidStack stack) {
      return true;
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      if (this.container.getCount() == 1
         && resource.getAmount() >= 1000
         && this.container.getItem() != Items.MILK_BUCKET
         && this.getFluid().isEmpty()
         && this.canFillFluidType(resource)) {
         if (action.execute()) {
            this.setFluid(resource);
         }

         return 1000;
      } else {
         return 0;
      }
   }

   @Override
   public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      if (this.container.getCount() == 1 && resource.getAmount() >= 1000) {
         FluidStack fluidStack = this.getFluid();
         if (!fluidStack.isEmpty() && FluidStack.isSameFluidSameComponents(fluidStack, resource)) {
            if (action.execute()) {
               this.setFluid(FluidStack.EMPTY);
            }

            return fluidStack;
         } else {
            return FluidStack.EMPTY;
         }
      } else {
         return FluidStack.EMPTY;
      }
   }

   @Override
   public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      if (this.container.getCount() == 1 && maxDrain >= 1000) {
         FluidStack fluidStack = this.getFluid();
         if (!fluidStack.isEmpty()) {
            if (action.execute()) {
               this.setFluid(FluidStack.EMPTY);
            }

            return fluidStack;
         } else {
            return FluidStack.EMPTY;
         }
      } else {
         return FluidStack.EMPTY;
      }
   }
}
