package net.neoforged.neoforge.transfer;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;

public abstract class ResourceStacksResourceHandler<R extends Resource> extends StacksResourceHandler<ResourceStack<R>, R> {
   public ResourceStacksResourceHandler(int size, R emptyResource, Codec<ResourceStack<R>> stackCodec) {
      super(size, new ResourceStack<>(emptyResource, 0), stackCodec);
   }

   public ResourceStacksResourceHandler(NonNullList<ResourceStack<R>> stacks, R emptyResource, Codec<ResourceStack<R>> stackCodec) {
      super(stacks, new ResourceStack<>(emptyResource, 0), stackCodec);
   }

   public R getResourceFrom(ResourceStack<R> stack) {
      return stack.resource();
   }

   public int getAmountFrom(ResourceStack<R> stack) {
      return stack.amount();
   }

   protected ResourceStack<R> getStackFrom(R resource, int amount) {
      return ResourceHandlerUtil.isEmpty(resource, amount) ? this.emptyStack : new ResourceStack<>(resource, amount);
   }

   protected ResourceStack<R> copyOf(ResourceStack<R> stack) {
      return stack;
   }

   public boolean matches(ResourceStack<R> stack, R resource) {
      return stack.resource().equals(resource);
   }
}
