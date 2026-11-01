package net.neoforged.neoforge.transfer.fluid;

import java.util.Objects;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

public final class BucketResourceHandler extends ItemAccessResourceHandler<FluidResource> {
   public BucketResourceHandler(ItemAccess itemAccess) {
      super(itemAccess, 1);
   }

   protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
      if (accessResource.getItem() instanceof BucketItem bucketItem) {
         return FluidResource.of(bucketItem.content);
      } else {
         return accessResource.is(Items.MILK_BUCKET) && NeoForgeMod.MILK.isBound() ? FluidResource.of(NeoForgeMod.MILK.get()) : FluidResource.EMPTY;
      }
   }

   @Override
   protected int getAmountFrom(ItemResource accessResource, int index) {
      FluidResource resource = this.getResourceFrom(accessResource, index);
      return resource.isEmpty() ? 0 : 1000;
   }

   protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
      if (newAmount == 0) {
         return ItemResource.of(Items.BUCKET);
      } else if (newAmount != 1000) {
         return ItemResource.EMPTY;
      } else {
         FluidStack newStack = newResource.toStack(newAmount);
         return ItemResource.of(newStack.getFluidType().getBucket(newStack));
      }
   }

   protected int getCapacity(int index, FluidResource resource) {
      Objects.checkIndex(index, this.size());
      return 1000;
   }
}
