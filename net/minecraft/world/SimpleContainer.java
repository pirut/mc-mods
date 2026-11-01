package net.minecraft.world;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SimpleContainer implements Container, StackedContentsCompatible {
   private final int size;
   private final NonNullList<ItemStack> items;
   @Nullable
   private List<ContainerListener> listeners;

   public SimpleContainer(int size) {
      this.size = size;
      this.items = NonNullList.withSize(size, ItemStack.EMPTY);
   }

   public SimpleContainer(ItemStack... items) {
      this.size = items.length;
      this.items = NonNullList.of(ItemStack.EMPTY, items);
   }

   public void addListener(ContainerListener listener) {
      if (this.listeners == null) {
         this.listeners = Lists.newArrayList();
      }

      this.listeners.add(listener);
   }

   public void removeListener(ContainerListener listener) {
      if (this.listeners != null) {
         this.listeners.remove(listener);
      }
   }

   @Override
   public ItemStack getItem(int slot) {
      return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
   }

   public List<ItemStack> removeAllItems() {
      List<ItemStack> list = this.items.stream().filter(itemStack -> !itemStack.isEmpty()).collect(Collectors.toList());
      this.clearContent();
      return list;
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      ItemStack itemStack = ContainerHelper.removeItem(this.items, slot, amount);
      if (!itemStack.isEmpty()) {
         this.setChanged();
      }

      return itemStack;
   }

   public ItemStack removeItemType(Item item, int amount) {
      ItemStack itemStack = new ItemStack(item, 0);

      for (int i = this.size - 1; i >= 0; i--) {
         ItemStack itemStack2 = this.getItem(i);
         if (itemStack2.getItem().equals(item)) {
            int j = amount - itemStack.getCount();
            ItemStack itemStack3 = itemStack2.split(j);
            itemStack.grow(itemStack3.getCount());
            if (itemStack.getCount() == amount) {
               break;
            }
         }
      }

      if (!itemStack.isEmpty()) {
         this.setChanged();
      }

      return itemStack;
   }

   public ItemStack addItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack itemStack = stack.copy();
         this.moveItemToOccupiedSlotsWithSameType(itemStack);
         if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            this.moveItemToEmptySlots(itemStack);
            return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
         }
      }
   }

   public boolean canAddItem(ItemStack stack) {
      boolean bl = false;

      for (ItemStack itemStack : this.items) {
         if (itemStack.isEmpty() || ItemStack.isSameItemSameComponents(itemStack, stack) && itemStack.getCount() < itemStack.getMaxStackSize()) {
            bl = true;
            break;
         }
      }

      return bl;
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      ItemStack itemStack = this.items.get(slot);
      if (itemStack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         this.items.set(slot, ItemStack.EMPTY);
         return itemStack;
      }
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      this.items.set(slot, stack);
      stack.limitSize(this.getMaxStackSize(stack));
      this.setChanged();
   }

   @Override
   public int getContainerSize() {
      return this.size;
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack itemStack : this.items) {
         if (!itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public void setChanged() {
      if (this.listeners != null) {
         for (ContainerListener containerListener : this.listeners) {
            containerListener.containerChanged(this);
         }
      }
   }

   @Override
   public boolean stillValid(Player player) {
      return true;
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   @Override
   public void fillStackedContents(StackedItemContents stackedItemContents) {
      for (ItemStack itemStack : this.items) {
         stackedItemContents.accountStack(itemStack);
      }
   }

   @Override
   public String toString() {
      return this.items.stream().filter(itemStack -> !itemStack.isEmpty()).collect(Collectors.toList()).toString();
   }

   private void moveItemToEmptySlots(ItemStack stack) {
      for (int i = 0; i < this.size; i++) {
         ItemStack itemStack = this.getItem(i);
         if (itemStack.isEmpty()) {
            this.setItem(i, stack.copyAndClear());
            return;
         }
      }
   }

   private void moveItemToOccupiedSlotsWithSameType(ItemStack stack) {
      for (int i = 0; i < this.size; i++) {
         ItemStack itemStack = this.getItem(i);
         if (ItemStack.isSameItemSameComponents(itemStack, stack)) {
            this.moveItemsBetweenStacks(stack, itemStack);
            if (stack.isEmpty()) {
               return;
            }
         }
      }
   }

   private void moveItemsBetweenStacks(ItemStack stack, ItemStack other) {
      int i = this.getMaxStackSize(other);
      int j = Math.min(stack.getCount(), i - other.getCount());
      if (j > 0) {
         other.grow(j);
         stack.shrink(j);
         this.setChanged();
      }
   }

   public void fromItemList(ValueInput.TypedInputList<ItemStack> input) {
      this.clearContent();

      for (ItemStack itemStack : input) {
         this.addItem(itemStack);
      }
   }

   public void storeAsItemList(ValueOutput.TypedOutputList<ItemStack> output) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack itemStack = this.getItem(i);
         if (!itemStack.isEmpty()) {
            output.add(itemStack);
         }
      }
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }
}
