package net.neoforged.neoforge.transfer.item;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public abstract class ItemStackResourceHandler extends SnapshotJournal<ItemStack> implements ResourceHandler<ItemResource>, ValueIOSerializable {
   public static final String VALUE_IO_KEY = "stack";

   protected abstract ItemStack getStack();

   protected abstract void setStack(ItemStack var1);

   protected boolean isValid(ItemResource resource) {
      return true;
   }

   protected int getCapacity(ItemResource resource) {
      return resource.isEmpty() ? 99 : Math.min(resource.getMaxStackSize(), 99);
   }

   @Override
   public final int size() {
      return 1;
   }

   public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      ItemStack currentStack = this.getStack();
      if ((currentStack.isEmpty() || resource.matches(currentStack)) && this.isValid(resource)) {
         int insertedAmount = Math.min(amount, this.getCapacity(resource) - currentStack.getCount());
         if (insertedAmount > 0) {
            this.updateSnapshots(transaction);
            currentStack = this.getStack();
            if (currentStack.isEmpty()) {
               currentStack = resource.toStack(insertedAmount);
            } else {
               currentStack.grow(insertedAmount);
            }

            this.setStack(currentStack);
            return insertedAmount;
         }
      }

      return 0;
   }

   public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      ItemStack currentStack = this.getStack();
      if (resource.matches(currentStack)) {
         int extracted = Math.min(currentStack.getCount(), amount);
         if (extracted > 0) {
            this.updateSnapshots(transaction);
            currentStack = this.getStack();
            currentStack.shrink(extracted);
            this.setStack(currentStack);
            return extracted;
         }
      }

      return 0;
   }

   public ItemResource getResource(int index) {
      Objects.checkIndex(index, this.size());
      return ItemResource.of(this.getStack());
   }

   @Override
   public long getAmountAsLong(int index) {
      Objects.checkIndex(index, this.size());
      return this.getStack().getCount();
   }

   public long getCapacityAsLong(int index, ItemResource resource) {
      Objects.checkIndex(index, this.size());
      return !resource.isEmpty() && !this.isValid(resource) ? 0L : this.getCapacity(resource);
   }

   public boolean isValid(int index, ItemResource resource) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmpty(resource);
      return this.isValid(resource);
   }

   protected ItemStack createSnapshot() {
      ItemStack original = this.getStack();
      this.setStack(original.copy());
      return original;
   }

   protected void revertToSnapshot(ItemStack snapshot) {
      this.setStack(snapshot);
   }

   @Override
   public void serialize(ValueOutput output) {
      if (!this.getStack().isEmpty()) {
         output.store("stack", ItemStack.CODEC, this.getStack());
      }
   }

   @Override
   public void deserialize(ValueInput input) {
      this.setStack(input.<ItemStack>read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY));
   }

   @Override
   public String toString() {
      return this.getClass().getName() + "[" + this.getStack() + "]";
   }
}
