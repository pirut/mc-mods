package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface ContainerExtension {
   private Container self() {
      return (Container)this;
   }

   @OverrideOnly
   default void setItem(int slot, ItemStack stack, boolean insideTransaction) {
      this.self().setItem(slot, stack);
   }

   @OverrideOnly
   default void onTransfer(int slot, int amountChange, TransactionContext transaction) {
   }
}
