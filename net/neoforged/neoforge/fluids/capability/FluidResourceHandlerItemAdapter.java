package net.neoforged.neoforge.fluids.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class FluidResourceHandlerItemAdapter extends FluidResourceHandlerAdapter implements IFluidHandlerItem {
   private final ItemAccess itemAccess;

   public FluidResourceHandlerItemAdapter(ResourceHandler<FluidResource> handler, ItemAccess itemAccess) {
      super(handler);
      this.itemAccess = itemAccess;
   }

   @Override
   public ItemStack getContainer() {
      return this.itemAccess.getResource().toStack(this.itemAccess.getAmount());
   }
}
