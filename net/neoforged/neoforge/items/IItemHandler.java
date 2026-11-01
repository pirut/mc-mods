package net.neoforged.neoforge.items;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public interface IItemHandler {
   static IItemHandler of(ResourceHandler<ItemResource> handler) {
      Objects.requireNonNull(handler, "handler");
      return new ItemResourceHandlerAdapter(handler);
   }

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int getSlots();

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   ItemStack getStackInSlot(int var1);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   ItemStack insertItem(int var1, ItemStack var2, boolean var3);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   ItemStack extractItem(int var1, int var2, boolean var3);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int getSlotLimit(int var1);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   boolean isItemValid(int var1, ItemStack var2);
}
