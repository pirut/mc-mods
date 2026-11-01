package net.neoforged.neoforge.fluids.capability;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
class FluidResourceHandlerAdapter implements IFluidHandler {
   private final ResourceHandler<FluidResource> handler;

   FluidResourceHandlerAdapter(ResourceHandler<FluidResource> handler) {
      this.handler = handler;
   }

   @Override
   public int getTanks() {
      return this.handler.size();
   }

   @Override
   public FluidStack getFluidInTank(int tank) {
      return FluidUtil.getStack(this.handler, tank);
   }

   @Override
   public int getTankCapacity(int tank) {
      return this.handler.getCapacityAsInt(tank, FluidResource.EMPTY);
   }

   @Override
   public boolean isFluidValid(int tank, FluidStack stack) {
      return this.handler.isValid(tank, FluidResource.of(stack));
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      if (resource.isEmpty()) {
         return 0;
      } else {
         int var5;
         try (Transaction tx = Transaction.openRoot()) {
            int inserted = this.handler.insert(FluidResource.of(resource), resource.getAmount(), tx);
            if (action.execute()) {
               tx.commit();
            }

            var5 = inserted;
         }

         return var5;
      }
   }

   @Override
   public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      if (resource.isEmpty()) {
         return FluidStack.EMPTY;
      } else {
         FluidStack var5;
         try (Transaction tx = Transaction.openRoot()) {
            int extracted = this.handler.extract(FluidResource.of(resource), resource.getAmount(), tx);
            if (action.execute()) {
               tx.commit();
            }

            var5 = extracted == 0 ? FluidStack.EMPTY : resource.copyWithAmount(extracted);
         }

         return var5;
      }
   }

   @Override
   public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      if (maxDrain <= 0) {
         return FluidStack.EMPTY;
      } else {
         FluidStack var5;
         try (Transaction tx = Transaction.openRoot()) {
            ResourceStack<FluidResource> extracted = ResourceHandlerUtil.extractFirst(this.handler, fr -> true, maxDrain, tx);
            if (action.execute()) {
               tx.commit();
            }

            var5 = extracted == null ? FluidStack.EMPTY : extracted.resource().toStack(extracted.amount());
         }

         return var5;
      }
   }
}
