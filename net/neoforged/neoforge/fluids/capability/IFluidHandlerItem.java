package net.neoforged.neoforge.fluids.capability;

import net.minecraft.world.item.ItemStack;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public interface IFluidHandlerItem extends IFluidHandler {
   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   ItemStack getContainer();
}
