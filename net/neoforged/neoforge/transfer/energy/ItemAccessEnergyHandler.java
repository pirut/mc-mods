package net.neoforged.neoforge.transfer.energy;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ItemAccessEnergyHandler implements EnergyHandler {
   protected final ItemAccess itemAccess;
   protected final Item validItem;
   protected final DataComponentType<Integer> energyComponent;
   protected final int capacity;
   protected final int maxInsert;
   protected final int maxExtract;

   public ItemAccessEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity) {
      this(itemAccess, energyComponent, capacity, capacity);
   }

   public ItemAccessEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity, int maxTransfer) {
      this(itemAccess, energyComponent, capacity, maxTransfer, maxTransfer);
   }

   public ItemAccessEnergyHandler(ItemAccess itemAccess, DataComponentType<Integer> energyComponent, int capacity, int maxInsert, int maxExtract) {
      TransferPreconditions.checkNonNegative(capacity);
      TransferPreconditions.checkNonNegative(maxInsert);
      TransferPreconditions.checkNonNegative(maxExtract);
      this.itemAccess = itemAccess;
      this.validItem = itemAccess.getResource().getItem();
      this.energyComponent = energyComponent;
      this.capacity = capacity;
      this.maxInsert = maxInsert;
      this.maxExtract = maxExtract;
   }

   protected int getAmountFrom(ItemResource accessResource) {
      return !accessResource.is(this.validItem) ? 0 : accessResource.getOrDefault(this.energyComponent, 0);
   }

   protected ItemResource update(ItemResource accessResource, int newAmount) {
      return accessResource.with(this.energyComponent, newAmount);
   }

   @Override
   public long getAmountAsLong() {
      return (long)this.itemAccess.getAmount() * this.getAmountFrom(this.itemAccess.getResource());
   }

   @Override
   public long getCapacityAsLong() {
      ItemResource accessResource = this.itemAccess.getResource();
      return !accessResource.is(this.validItem) ? 0L : (long)this.itemAccess.getAmount() * this.capacity;
   }

   @Override
   public int insert(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      int accessAmount = this.itemAccess.getAmount();
      if (accessAmount == 0) {
         return 0;
      } else {
         int amountPerItem = Math.min(this.maxInsert, amount / accessAmount);
         if (amountPerItem == 0) {
            return 0;
         } else {
            ItemResource accessResource = this.itemAccess.getResource();
            if (!accessResource.is(this.validItem)) {
               return 0;
            } else {
               int currentAmountPerItem = this.getAmountFrom(accessResource);
               int insertedPerItem = Math.min(amountPerItem, this.capacity - currentAmountPerItem);
               if (insertedPerItem > 0) {
                  ItemResource filledResource = this.update(accessResource, currentAmountPerItem + insertedPerItem);
                  if (!filledResource.isEmpty()) {
                     return insertedPerItem * this.itemAccess.exchange(filledResource, accessAmount, transaction);
                  }
               }

               return 0;
            }
         }
      }
   }

   @Override
   public int extract(int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonNegative(amount);
      int accessAmount = this.itemAccess.getAmount();
      if (accessAmount == 0) {
         return 0;
      } else {
         int amountPerItem = Math.min(this.maxExtract, amount / accessAmount);
         if (amountPerItem == 0) {
            return 0;
         } else {
            ItemResource accessResource = this.itemAccess.getResource();
            int currentAmountPerItem = this.getAmountFrom(accessResource);
            int extractedPerItem = Math.min(amountPerItem, currentAmountPerItem);
            if (extractedPerItem > 0) {
               ItemResource emptiedResource = this.update(accessResource, currentAmountPerItem - extractedPerItem);
               if (!emptiedResource.isEmpty()) {
                  return extractedPerItem * this.itemAccess.exchange(emptiedResource, accessAmount, transaction);
               }
            }

            return 0;
         }
      }
   }
}
