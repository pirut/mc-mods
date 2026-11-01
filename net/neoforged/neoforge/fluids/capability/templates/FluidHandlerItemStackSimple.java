package net.neoforged.neoforge.fluids.capability.templates;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class FluidHandlerItemStackSimple implements IFluidHandlerItem {
   protected final Supplier<DataComponentType<SimpleFluidContent>> componentType;
   protected ItemStack container;
   protected int capacity;

   public FluidHandlerItemStackSimple(Supplier<DataComponentType<SimpleFluidContent>> componentType, ItemStack container, int capacity) {
      this.componentType = componentType;
      this.container = container;
      this.capacity = capacity;
   }

   @Override
   public ItemStack getContainer() {
      return this.container;
   }

   public FluidStack getFluid() {
      return this.container.getOrDefault(this.componentType, SimpleFluidContent.EMPTY).copy();
   }

   protected void setFluid(FluidStack fluid) {
      this.container.set(this.componentType, SimpleFluidContent.copyOf(fluid));
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
      return this.capacity;
   }

   @Override
   public boolean isFluidValid(int tank, FluidStack stack) {
      return true;
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      if (this.container.getCount() == 1 && !resource.isEmpty() && this.canFillFluidType(resource)) {
         FluidStack contained = this.getFluid();
         if (contained.isEmpty()) {
            int fillAmount = Math.min(this.capacity, resource.getAmount());
            if (fillAmount == this.capacity) {
               if (action.execute()) {
                  this.setFluid(resource.copyWithAmount(fillAmount));
               }

               return fillAmount;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   @Override
   public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      return this.container.getCount() == 1 && !resource.isEmpty() && FluidStack.isSameFluidSameComponents(resource, this.getFluid())
         ? this.drain(resource.getAmount(), action)
         : FluidStack.EMPTY;
   }

   @Override
   public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      if (this.container.getCount() == 1 && maxDrain > 0) {
         FluidStack contained = this.getFluid();
         if (!contained.isEmpty() && this.canDrainFluidType(contained)) {
            int drainAmount = Math.min(contained.getAmount(), maxDrain);
            if (drainAmount == this.capacity) {
               FluidStack drained = contained.copy();
               if (action.execute()) {
                  this.setContainerToEmpty();
               }

               return drained;
            } else {
               return FluidStack.EMPTY;
            }
         } else {
            return FluidStack.EMPTY;
         }
      } else {
         return FluidStack.EMPTY;
      }
   }

   public boolean canFillFluidType(FluidStack fluid) {
      return true;
   }

   public boolean canDrainFluidType(FluidStack fluid) {
      return true;
   }

   protected void setContainerToEmpty() {
      this.container.remove(this.componentType);
   }

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   public static class Consumable extends FluidHandlerItemStackSimple {
      public Consumable(Supplier<DataComponentType<SimpleFluidContent>> componentType, ItemStack container, int capacity) {
         super(componentType, container, capacity);
      }

      @Override
      protected void setContainerToEmpty() {
         super.setContainerToEmpty();
         this.container.shrink(1);
      }
   }

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   public static class SwapEmpty extends FluidHandlerItemStackSimple {
      protected final ItemStack emptyContainer;

      public SwapEmpty(Supplier<DataComponentType<SimpleFluidContent>> componentType, ItemStack container, ItemStack emptyContainer, int capacity) {
         super(componentType, container, capacity);
         this.emptyContainer = emptyContainer;
      }

      @Override
      protected void setContainerToEmpty() {
         super.setContainerToEmpty();
         this.container = this.emptyContainer;
      }
   }
}
