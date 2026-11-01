package net.neoforged.neoforge.transfer.fluid;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemAccessFluidHandler extends ItemAccessResourceHandler<FluidResource> {
   protected final Item validItem;
   protected final DataComponentType<SimpleFluidContent> component;
   protected int capacity;

   public ItemAccessFluidHandler(ItemAccess itemAccess, DataComponentType<SimpleFluidContent> component, int capacity) {
      super(itemAccess, 1);
      this.validItem = itemAccess.getResource().getItem();
      this.component = component;
      this.capacity = capacity;
   }

   protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
      return accessResource.is(this.validItem)
         ? FluidResource.of(accessResource.getOrDefault(this.component, SimpleFluidContent.EMPTY).copy())
         : FluidResource.EMPTY;
   }

   @Override
   protected int getAmountFrom(ItemResource accessResource, int index) {
      return accessResource.is(this.validItem) ? accessResource.getOrDefault(this.component, SimpleFluidContent.EMPTY).getAmount() : 0;
   }

   protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
      return accessResource.with(this.component, SimpleFluidContent.copyOf(newResource.toStack(newAmount)));
   }

   public boolean isValid(int index, FluidResource resource) {
      return this.itemAccess.getResource().is(this.validItem);
   }

   protected int getCapacity(int index, FluidResource resource) {
      return this.capacity;
   }
}
