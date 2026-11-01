package net.neoforged.neoforge.items;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
class ItemResourceHandlerAdapter implements IItemHandler {
   private final ResourceHandler<ItemResource> handler;

   ItemResourceHandlerAdapter(ResourceHandler<ItemResource> handler) {
      this.handler = handler;
   }

   @Override
   public int getSlots() {
      return this.handler.size();
   }

   @Override
   public ItemStack getStackInSlot(int slot) {
      return ItemUtil.getStack(this.handler, slot);
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return ItemUtil.insertItemReturnRemaining(this.handler, slot, stack, simulate, null);
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (amount <= 0) {
         return ItemStack.EMPTY;
      } else {
         ItemResource resource = this.handler.getResource(slot);
         if (resource.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            amount = Math.min(amount, resource.getMaxStackSize());

            ItemStack var7;
            try (Transaction tx = Transaction.openRoot()) {
               int extracted = this.handler.extract(slot, resource, amount, tx);
               if (!simulate) {
                  tx.commit();
               }

               var7 = resource.toStack(extracted);
            }

            return var7;
         }
      }
   }

   @Override
   public int getSlotLimit(int slot) {
      return this.handler.getCapacityAsInt(slot, ItemResource.EMPTY);
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      return this.handler.isValid(slot, ItemResource.of(stack));
   }
}
