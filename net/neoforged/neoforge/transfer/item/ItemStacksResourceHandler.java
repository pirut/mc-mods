package net.neoforged.neoforge.transfer.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;

public class ItemStacksResourceHandler extends StacksResourceHandler<ItemStack, ItemResource> {
   public ItemStacksResourceHandler(int size) {
      super(size, ItemStack.EMPTY, ItemStack.OPTIONAL_CODEC);
   }

   public ItemStacksResourceHandler(NonNullList<ItemStack> stacks) {
      super(stacks, ItemStack.EMPTY, ItemStack.OPTIONAL_CODEC);
   }

   public ItemResource getResourceFrom(ItemStack stack) {
      return ItemResource.of(stack);
   }

   public int getAmountFrom(ItemStack stack) {
      return stack.getCount();
   }

   protected ItemStack getStackFrom(ItemResource resource, int amount) {
      return resource.toStack(amount);
   }

   protected int getCapacity(int index, ItemResource resource) {
      return resource.isEmpty() ? 99 : Math.min(resource.getMaxStackSize(), 99);
   }

   protected ItemStack copyOf(ItemStack stack) {
      return stack.copy();
   }

   public boolean matches(ItemStack stack, ItemResource resource) {
      return resource.matches(stack);
   }
}
