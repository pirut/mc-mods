package net.neoforged.neoforge.transfer.access;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

class InfiniteMaterialsItemAccess implements ItemAccess {
   private final ResourceHandler<ItemResource> mainSlots;
   private final ItemResource resource;
   private final int amount;

   InfiniteMaterialsItemAccess(Player player, ItemResource resource, int amount) {
      this.mainSlots = PlayerInventoryWrapper.of(player).getMainSlots();
      this.resource = resource;
      this.amount = amount;
   }

   @Override
   public ItemResource getResource() {
      return this.resource;
   }

   @Override
   public int getAmount() {
      return this.amount;
   }

   @Override
   public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      if (amount > 0 && !ResourceHandlerUtil.contains(this.mainSlots, resource)) {
         this.mainSlots.insert(resource, 1, transaction);
      }

      return amount;
   }

   @Override
   public int extract(ItemResource resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      return amount;
   }
}
