package net.neoforged.neoforge.transfer.item;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.RangedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class PlayerInventoryWrapper extends VanillaContainerWrapper {
   private final PlayerInventoryWrapper.DroppedItems droppedItems = new PlayerInventoryWrapper.DroppedItems();
   private final Inventory inventory;

   public static PlayerInventoryWrapper of(Player player) {
      return of(player.getInventory());
   }

   public static PlayerInventoryWrapper of(Inventory inventory) {
      return (PlayerInventoryWrapper)VanillaContainerWrapper.of(inventory);
   }

   PlayerInventoryWrapper(Inventory inventory) {
      super(inventory);
      this.inventory = inventory;
   }

   @Override
   void resize() {
      this.size = 41;

      while (this.slotWrappers.size() < this.size) {
         int index = this.slotWrappers.size();
         if (36 <= index && index < 40) {
            EquipmentSlot equipmentSlot = (EquipmentSlot)Inventory.EQUIPMENT_SLOT_MAPPING.get(index);
            this.slotWrappers.add(new PlayerInventoryWrapper.ArmorSlotWrapper(index, equipmentSlot));
         } else {
            this.slotWrappers.add(new VanillaContainerWrapper.SlotWrapper(index));
         }
      }
   }

   @Override
   void onRootCommit() {
      super.onRootCommit();
      if (!this.inventory.player.level().isClientSide()) {
         this.inventory.player.containerMenu.broadcastChanges();
      }
   }

   public ResourceHandler<ItemResource> getSlot(int slot) {
      return this.getSlotWrapper(slot);
   }

   public ResourceHandler<ItemResource> getMainHandSlot() {
      if (Inventory.isHotbarSlot(this.inventory.getSelectedSlot())) {
         return this.getSlot(this.inventory.getSelectedSlot());
      } else {
         throw new RuntimeException("Unexpected player selected slot: " + this.inventory.getSelectedSlot());
      }
   }

   public ResourceHandler<ItemResource> getHandSlot(InteractionHand hand) {
      return switch (hand) {
         case MAIN_HAND -> this.getMainHandSlot();
         case OFF_HAND -> this.getSlot(40);
      };
   }

   public ResourceHandler<ItemResource> getHandSlots() {
      return new CombinedResourceHandler<>(this.getMainHandSlot(), this.getHandSlot(InteractionHand.OFF_HAND));
   }

   public ResourceHandler<ItemResource> getMainSlots() {
      return RangedResourceHandler.of(this, 0, 36);
   }

   public ResourceHandler<ItemResource> getArmorSlot(EquipmentSlot slot) {
      if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
         throw new IllegalArgumentException("EquipmentSlot is not an armor slot: " + slot);
      } else {
         return this.getSlot(slot.getIndex(36));
      }
   }

   public ResourceHandler<ItemResource> getArmorSlots() {
      return RangedResourceHandler.of(this, 36, 40);
   }

   public int insert(ItemResource resource, int amount, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      int inserted = 0;

      for (InteractionHand hand : InteractionHand.values()) {
         ResourceHandler<ItemResource> handSlot = this.getHandSlot(hand);
         if (handSlot.getResource(0).equals(resource)) {
            inserted += handSlot.insert(resource, amount - inserted, transaction);
            if (inserted == amount) {
               return inserted;
            }
         }
      }

      return inserted + ResourceHandlerUtil.insertStacking(this.getMainSlots(), resource, amount - inserted, transaction);
   }

   public void placeItemBackInInventory(ItemResource resource, int amount, TransactionContext transactionContext) {
      int inserted = this.insert(resource, amount, transactionContext);
      if (inserted < amount) {
         this.drop(resource, amount - inserted, false, false, transactionContext);
      }
   }

   public void drop(ItemResource resource, int amount, boolean dropAround, boolean includeThrowerName, TransactionContext transaction) {
      TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
      if (amount != 0) {
         if (!this.inventory.player.level().isClientSide()) {
            this.droppedItems.addDrop(resource, amount, dropAround, includeThrowerName, transaction);
         }
      }
   }

   @Override
   public String toString() {
      return "PlayerInventoryWrapper{player=%s}".formatted(this.inventory.player);
   }

   private class ArmorSlotWrapper extends VanillaContainerWrapper.SlotWrapper {
      private final EquipmentSlot slot;

      ArmorSlotWrapper(int index, EquipmentSlot slot) {
         super(index);
         this.slot = slot;
      }

      @Override
      protected boolean isValid(ItemResource resource) {
         return resource.toStack().canEquip(this.slot, PlayerInventoryWrapper.this.inventory.player) && super.isValid(resource);
      }

      @Override
      protected int getCapacity(ItemResource resource) {
         return this.slot.countLimit;
      }

      @Override
      public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
         return !PlayerInventoryWrapper.this.inventory.player.isCreative()
               && EnchantmentHelper.has(resource.toStack(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            ? 0
            : super.extract(index, resource, amount, transaction);
      }
   }

   private class DroppedItems extends SnapshotJournal<Integer> {
      final Deque<PlayerInventoryWrapper.DroppedItems.DropInfo> entries = new ArrayDeque<>();

      void addDrop(ItemResource resource, int amount, boolean dropAround, boolean includeThrowerName, TransactionContext transaction) {
         this.updateSnapshots(transaction);
         this.entries.add(new PlayerInventoryWrapper.DroppedItems.DropInfo(resource, amount, dropAround, includeThrowerName));
      }

      protected Integer createSnapshot() {
         return this.entries.size();
      }

      protected void revertToSnapshot(Integer snapshot) {
         int previousSize = snapshot;

         while (this.entries.size() > previousSize) {
            this.entries.removeLast();
         }
      }

      protected void onRootCommit(Integer originalState) {
         while (!this.entries.isEmpty()) {
            PlayerInventoryWrapper.DroppedItems.DropInfo dropInfo = this.entries.removeFirst();
            int remainder = dropInfo.amount;
            int maxStackSize = dropInfo.resource.getMaxStackSize();

            while (remainder > 0) {
               int dropped = Math.min(maxStackSize, remainder);
               CommonHooks.onPlayerTossEvent(
                  PlayerInventoryWrapper.this.inventory.player, dropInfo.resource.toStack(dropped), dropInfo.dropAround, dropInfo.includeThrowerName
               );
               remainder -= dropped;
            }
         }
      }

      private record DropInfo(ItemResource resource, int amount, boolean dropAround, boolean includeThrowerName) {
      }
   }
}
