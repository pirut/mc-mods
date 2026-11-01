package net.neoforged.neoforge.transfer;

import java.util.Objects;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public abstract class ItemAccessResourceHandler<T extends Resource> implements ResourceHandler<T> {
   protected final ItemAccess itemAccess;
   protected final int size;

   protected ItemAccessResourceHandler(ItemAccess itemAccess, int size) {
      this.itemAccess = itemAccess;
      this.size = size;
   }

   protected abstract T getResourceFrom(ItemResource var1, int var2);

   protected abstract int getAmountFrom(ItemResource var1, int var2);

   protected abstract ItemResource update(ItemResource var1, int var2, T var3, int var4);

   @Override
   public boolean isValid(int index, T resource) {
      return true;
   }

   protected abstract int getCapacity(int var1, T var2);

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public T getResource(int index) {
      Objects.checkIndex(index, this.size());
      return this.getResourceFrom(this.itemAccess.getResource(), index);
   }

   @Override
   public long getAmountAsLong(int index) {
      Objects.checkIndex(index, this.size());
      return (long)this.itemAccess.getAmount() * this.getAmountFrom(this.itemAccess.getResource(), index);
   }

   @Override
   public long getCapacityAsLong(int index, T resource) {
      Objects.checkIndex(index, this.size());
      return !resource.isEmpty() && !this.isValid(index, resource) ? 0L : (long)this.itemAccess.getAmount() * this.getCapacity(index, resource);
   }

   @Override
   public int insert(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int accessAmount = this.itemAccess.getAmount();
      if (accessAmount == 0) {
         return 0;
      } else {
         int amountPerItem = amount / accessAmount;
         ItemResource accessResource = this.itemAccess.getResource();
         int currentAmountPerItem = this.getAmountFrom(accessResource, index);
         if ((currentAmountPerItem == 0 || resource.equals(this.getResourceFrom(accessResource, index))) && this.isValid(index, resource)) {
            int insertedPerItem = Math.min(amountPerItem, this.getCapacity(index, resource) - currentAmountPerItem);
            if (insertedPerItem > 0) {
               ItemResource filledResource = this.update(accessResource, index, resource, insertedPerItem + currentAmountPerItem);
               if (!filledResource.isEmpty()) {
                  return insertedPerItem * this.itemAccess.exchange(filledResource, accessAmount, transaction);
               }
            }
         }

         return 0;
      }
   }

   @Override
   public int extract(int index, T resource, int amount, TransactionContext transaction) {
      Objects.checkIndex(index, this.size());
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int accessAmount = this.itemAccess.getAmount();
      if (accessAmount == 0) {
         return 0;
      } else {
         ItemResource accessResource = this.itemAccess.getResource();
         T currentResource = this.getResourceFrom(accessResource, index);
         if (resource.equals(currentResource)) {
            int currentAmountPerItem = this.getAmountFrom(accessResource, index);
            int extractedPerItem = Math.min(amount / accessAmount, currentAmountPerItem);
            if (extractedPerItem > 0) {
               ItemResource emptiedResource = this.update(accessResource, index, resource, currentAmountPerItem - extractedPerItem);
               if (!emptiedResource.isEmpty()) {
                  return extractedPerItem * this.itemAccess.exchange(emptiedResource, accessAmount, transaction);
               }
            }
         }

         return 0;
      }
   }
}
