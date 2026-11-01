package net.minecraft.world.entity.npc;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public interface InventoryCarrier {
   String TAG_INVENTORY = "Inventory";

   SimpleContainer getInventory();

   static void pickUpItem(Mob p_219612_, InventoryCarrier p_219613_, ItemEntity p_219614_) {
      ItemStack $$3 = p_219614_.getItem();
      if (p_219612_.wantsToPickUp($$3)) {
         SimpleContainer $$4 = p_219613_.getInventory();
         boolean $$5 = $$4.canAddItem($$3);
         if (!$$5) {
            return;
         }

         p_219612_.onItemPickup(p_219614_);
         int $$6 = $$3.getCount();
         ItemStack $$7 = $$4.addItem($$3);
         p_219612_.take(p_219614_, $$6 - $$7.getCount());
         if ($$7.isEmpty()) {
            p_219614_.discard();
         } else {
            $$3.setCount($$7.getCount());
         }
      }
   }

   default void readInventoryFromTag(CompoundTag p_253699_, HolderLookup.Provider p_330390_) {
      if (p_253699_.contains("Inventory", 9)) {
         this.getInventory().fromTag(p_253699_.getList("Inventory", 10), p_330390_);
      }
   }

   default void writeInventoryToTag(CompoundTag p_254428_, HolderLookup.Provider p_331103_) {
      p_254428_.put("Inventory", this.getInventory().createTag(p_331103_));
   }
}
