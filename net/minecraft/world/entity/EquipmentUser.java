package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public interface EquipmentUser {
   void setItemSlot(EquipmentSlot slot, ItemStack stack);

   ItemStack getItemBySlot(EquipmentSlot slot);

   void setDropChance(EquipmentSlot slot, float dropChance);

   default void equip(EquipmentTable equipmentTable, LootParams params) {
      this.equip(equipmentTable.lootTable(), params, equipmentTable.slotDropChances());
   }

   default void equip(ResourceKey<LootTable> equipmentLootTable, LootParams params, Map<EquipmentSlot, Float> slotDropChances) {
      this.equip(equipmentLootTable, params, 0L, slotDropChances);
   }

   default void equip(ResourceKey<LootTable> equipmentLootTable, LootParams params, long seed, Map<EquipmentSlot, Float> slotDropChances) {
      LootTable lootTable = params.getLevel().getServer().reloadableRegistries().getLootTable(equipmentLootTable);
      if (lootTable != LootTable.EMPTY) {
         List<ItemStack> list = lootTable.getRandomItems(params, seed);
         List<EquipmentSlot> list2 = new ArrayList<>();

         for (ItemStack itemStack : list) {
            EquipmentSlot equipmentSlot = this.resolveSlot(itemStack, list2);
            if (equipmentSlot != null) {
               ItemStack itemStack2 = equipmentSlot.limit(itemStack);
               this.setItemSlot(equipmentSlot, itemStack2);
               Float float_ = slotDropChances.get(equipmentSlot);
               if (float_ != null) {
                  this.setDropChance(equipmentSlot, float_);
               }

               list2.add(equipmentSlot);
            }
         }
      }
   }

   @Nullable
   default EquipmentSlot resolveSlot(ItemStack stack, List<EquipmentSlot> excludedSlots) {
      if (stack.isEmpty()) {
         return null;
      } else {
         Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
         if (equippable != null) {
            EquipmentSlot equipmentSlot = equippable.slot();
            if (!excludedSlots.contains(equipmentSlot)) {
               return equipmentSlot;
            }
         } else if (!excludedSlots.contains(EquipmentSlot.MAINHAND)) {
            return EquipmentSlot.MAINHAND;
         }

         return null;
      }
   }
}
