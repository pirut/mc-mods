package net.neoforged.neoforge.transfer.access;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

class StackItemAccess implements ItemAccess {
   private final Item item;
   private final ResourceHandler<ItemResource> wrapper;

   public StackItemAccess(ItemStack stack) {
      this.item = stack.getItem();
      this.wrapper = VanillaContainerWrapper.of(new SimpleContainer(stack) {
         @Override
         public void setItem(int slot, ItemStack stackx, boolean performSideEffects) {
            this.getItems().set(slot, stackx);
         }
      });
   }

   @Override
   public ItemResource getResource() {
      return this.wrapper.getResource(0);
   }

   @Override
   public int getAmount() {
      return this.wrapper.getAmountAsInt(0);
   }

   @Override
   public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      return !resource.is(this.item) ? 0 : this.wrapper.insert(resource, amount, transaction);
   }

   @Override
   public int extract(ItemResource resource, int amount, TransactionContext transaction) {
      return this.wrapper.extract(resource, amount, transaction);
   }
}
