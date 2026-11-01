package net.neoforged.neoforge.items;

import net.minecraft.world.item.ItemStack;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public interface IItemHandlerModifiable extends IItemHandler {
   void setStackInSlot(int var1, ItemStack var2);
}
