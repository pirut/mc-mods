package net.neoforged.neoforge.transfer.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class ItemUtil {
   private ItemUtil() {
   }

   public static ItemStack getStack(ResourceHandler<ItemResource> handler, int index) {
      ItemResource resource = handler.getResource(index);
      return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(index));
   }

   public static ItemStack insertItemReturnRemaining(
      ResourceHandler<ItemResource> handler, ItemStack stack, boolean simulate, @Nullable TransactionContext transaction
   ) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack var7;
         try (Transaction tx = Transaction.open(transaction)) {
            int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) {
               tx.commit();
            }

            int leftover = stack.getCount() - inserted;
            var7 = leftover == 0 ? ItemStack.EMPTY : stack.copyWithCount(leftover);
         }

         return var7;
      }
   }

   public static ItemStack insertItemReturnRemaining(
      ResourceHandler<ItemResource> handler, int index, ItemStack stack, boolean simulate, @Nullable TransactionContext transaction
   ) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack var8;
         try (Transaction tx = Transaction.open(transaction)) {
            int inserted = handler.insert(index, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) {
               tx.commit();
            }

            int leftover = stack.getCount() - inserted;
            var8 = leftover == 0 ? ItemStack.EMPTY : stack.copyWithCount(leftover);
         }

         return var8;
      }
   }
}
