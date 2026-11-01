package net.neoforged.neoforge.transfer.item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class BundleItemHandler implements ResourceHandler<ItemResource> {
   protected final ItemAccess itemAccess;
   protected final Item validItem;
   protected final DataComponentType<BundleContents> component;

   public BundleItemHandler(ItemAccess itemAccess, DataComponentType<BundleContents> component) {
      this.itemAccess = itemAccess;
      this.validItem = itemAccess.getResource().getItem();
      this.component = component;
   }

   @Override
   public int size() {
      BundleContents contents = this.itemAccess.getResource().get(this.component);
      return contents == null ? 0 : contents.size() + (contents.weight().intValue() < 1 ? 1 : 0);
   }

   protected ItemStack getStack(int index) {
      BundleContents contents = this.itemAccess.getResource().get(this.component);
      return contents != null && index < contents.size() ? contents.getItemUnsafe(index) : ItemStack.EMPTY;
   }

   public ItemResource getResource(int index) {
      return this.itemAccess.getResource().is(this.validItem) ? ItemResource.of(this.getStack(index)) : ItemResource.EMPTY;
   }

   @Override
   public long getAmountAsLong(int index) {
      return this.itemAccess.getResource().is(this.validItem) ? (long)this.itemAccess.getAmount() * this.getStack(index).getCount() : 0L;
   }

   public long getCapacityAsLong(int index, ItemResource resource) {
      return this.isValid(index, resource) ? (long)this.itemAccess.getAmount() * (resource.isEmpty() ? 99 : Math.min(resource.getMaxStackSize(), 99)) : 0L;
   }

   public boolean isValid(int index, ItemResource resource) {
      return this.itemAccess.getResource().is(this.validItem) && BundleContents.canItemBeInBundle(resource.toStack());
   }

   public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      TransferPreconditions.checkNonNegative(index);
      int accessAmount = this.itemAccess.getAmount();
      if (accessAmount == 0) {
         return 0;
      } else {
         int extractedPerItem = amount / accessAmount;
         ItemResource accessResource = this.itemAccess.getResource();
         BundleContents contents = accessResource.get(this.component);
         if (contents != null && index <= contents.size()) {
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            int inserted = 0;
            if (index == contents.size()) {
               inserted = mutable.tryInsert(resource.toStack(extractedPerItem));
            } else {
               ItemStack existing = contents.getItemUnsafe(index);
               if (!resource.matches(existing)) {
                  return 0;
               }

               inserted += mutable.tryInsert(resource.toStack(extractedPerItem));
            }

            return inserted * this.itemAccess.exchange(accessResource.with(this.component, mutable.toImmutable()), accessAmount, transaction);
         } else {
            return 0;
         }
      }
   }

   public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      TransferPreconditions.checkNonNegative(index);
      int accessAmount = this.itemAccess.getAmount();
      if (accessAmount == 0) {
         return 0;
      } else {
         int extractedPerItem = amount / accessAmount;
         ItemResource accessResource = this.itemAccess.getResource();
         BundleContents contents = accessResource.get(this.component);
         if (contents != null && index < contents.size()) {
            ItemStack stack = contents.getItemUnsafe(index);
            if (!stack.isEmpty() && resource.matches(stack)) {
               int toExtract = Math.min(extractedPerItem, stack.getCount());
               if (toExtract <= 0) {
                  return 0;
               } else {
                  List<ItemStack> items = new ObjectArrayList(contents.size());
                  contents.items().forEach(items::add);
                  if (toExtract == stack.getCount()) {
                     items.remove(index);
                  } else {
                     items.set(index, stack.copy().split(toExtract));
                  }

                  return toExtract * this.itemAccess.exchange(accessResource.with(this.component, new BundleContents(items)), accessAmount, transaction);
               }
            } else {
               return 0;
            }
         } else {
            return 0;
         }
      }
   }
}
