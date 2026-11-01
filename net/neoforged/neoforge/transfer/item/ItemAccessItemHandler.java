package net.neoforged.neoforge.transfer.item;

import com.google.common.base.Preconditions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ItemAccessItemHandler extends ItemAccessResourceHandler<ItemResource> {
   protected final Item validItem;
   protected final DataComponentType<ItemContainerContents> component;

   public ItemAccessItemHandler(ItemAccess itemAccess, DataComponentType<ItemContainerContents> component, int size) {
      super(itemAccess, size);
      this.validItem = itemAccess.getResource().getItem();
      this.component = component;
      Preconditions.checkArgument(size <= 256, "The max size of ItemContainerContents is 256 slots.");
   }

   protected ItemContainerContents getContents(ItemResource accessResource) {
      return accessResource.getOrDefault(this.component, ItemContainerContents.EMPTY);
   }

   protected ItemStack getStackFromContents(ItemContainerContents contents, int slot) {
      return slot < contents.getSlots() ? contents.getStackInSlot(slot) : ItemStack.EMPTY;
   }

   protected ItemResource getResourceFrom(ItemResource accessResource, int index) {
      return accessResource.is(this.validItem) ? ItemResource.of(this.getStackFromContents(this.getContents(accessResource), index)) : ItemResource.EMPTY;
   }

   @Override
   protected int getAmountFrom(ItemResource accessResource, int index) {
      return accessResource.is(this.validItem) ? this.getStackFromContents(this.getContents(accessResource), index).getCount() : 0;
   }

   protected ItemResource update(ItemResource accessResource, int index, ItemResource newResource, int newAmount) {
      ItemContainerContents contents = this.getContents(accessResource);
      NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.size), ItemStack.EMPTY);
      contents.copyInto(list);
      list.set(index, newResource.toStack(newAmount));
      return accessResource.with(this.component, ItemContainerContents.fromItems(list));
   }

   public boolean isValid(int index, ItemResource resource) {
      return this.itemAccess.getResource().is(this.validItem);
   }

   protected int getCapacity(int index, ItemResource resource) {
      return resource.isEmpty() ? 99 : Math.min(resource.getMaxStackSize(), 99);
   }
}
