package net.neoforged.neoforge.transfer.access;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.RangedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.CarriedSlotWrapper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

public interface ItemAccess {
   static ItemAccess forPlayerInteraction(Player player, InteractionHand hand) {
      if (player.hasInfiniteMaterials()) {
         return forInfiniteMaterials(player, player.getItemInHand(hand));
      } else {
         return forPlayerSlot(player, switch (hand) {
            case MAIN_HAND -> player.getInventory().getSelectedSlot();
            case OFF_HAND -> 40;
         });
      }
   }

   static ItemAccess forInfiniteMaterials(Player player, ItemStack contents) {
      if (!player.hasInfiniteMaterials()) {
         throw new IllegalArgumentException("Player " + player + " does not have infinite materials");
      } else {
         return new InfiniteMaterialsItemAccess(player, ItemResource.of(contents), contents.getCount());
      }
   }

   static ItemAccess forPlayerCursor(Player player, AbstractContainerMenu menu) {
      return new PlayerItemAccess(PlayerInventoryWrapper.of(player), CarriedSlotWrapper.of(menu));
   }

   static ItemAccess forPlayerSlot(Player player, int slot) {
      PlayerInventoryWrapper inventoryWrapper = PlayerInventoryWrapper.of(player);
      return new PlayerItemAccess(inventoryWrapper, RangedResourceHandler.ofSingleIndex(inventoryWrapper, slot));
   }

   static ItemAccess forHandlerIndex(ResourceHandler<ItemResource> handler, int index) {
      return new HandlerItemAccess(handler, index);
   }

   static ItemAccess forHandlerIndexStrict(ResourceHandler<ItemResource> handler, int index) {
      return new HandlerItemAccess(RangedResourceHandler.ofSingleIndex(handler, index), 0);
   }

   static ItemAccess forStack(ItemStack stack) {
      if (stack.isEmpty()) {
         throw new IllegalArgumentException("Expected stack to be non-empty.");
      } else {
         return new StackItemAccess(stack);
      }
   }

   @NonExtendable
   default ItemAccess oneByOne() {
      return new OneByOneItemAccess(this);
   }

   @NonExtendable
   @Nullable
   default <T> T getCapability(ItemCapability<T, ItemAccess> capability) {
      return capability.getCapability(this.getResource().toStack(), this);
   }

   ItemResource getResource();

   int getAmount();

   int insert(ItemResource var1, int var2, TransactionContext var3);

   int extract(ItemResource var1, int var2, TransactionContext var3);

   @NonExtendable
   default int exchange(ItemResource newResource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(newResource, amount);
      ItemResource currentResource = this.getResource();
      TransferPreconditions.checkNonEmpty(currentResource);

      try (Transaction subTransaction = Transaction.open(transaction)) {
         int extracted = this.extract(currentResource, amount, subTransaction);
         if (extracted > 0) {
            int inserted = this.insert(newResource, extracted, subTransaction);
            if (inserted == extracted) {
               subTransaction.commit();
               return extracted;
            }
         }
      }

      return 0;
   }
}
