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
   void setItemSlot(EquipmentSlot var1, ItemStack var2);

   ItemStack getItemBySlot(EquipmentSlot var1);

   void setDropChance(EquipmentSlot var1, float var2);

   default void equip(EquipmentTable p_340994_, LootParams p_338408_) {
      this.equip(p_340994_.lootTable(), p_338408_, p_340994_.slotDropChances());
   }

   default void equip(ResourceKey<LootTable> p_341330_, LootParams p_340863_, Map<EquipmentSlot, Float> p_341011_) {
      this.equip(p_341330_, p_340863_, 0L, p_341011_);
   }

   default void equip(ResourceKey<LootTable> p_340873_, LootParams p_338202_, long p_341024_, Map<EquipmentSlot, Float> p_341367_) {
      LootTable loottable = p_338202_.getLevel().getServer().reloadableRegistries().getLootTable(p_340873_);
      if (loottable != LootTable.EMPTY) {
         List<ItemStack> list = loottable.getRandomItems(p_338202_, p_341024_);
         List<EquipmentSlot> list1 = new ArrayList<>();

         for (ItemStack itemstack : list) {
            EquipmentSlot equipmentslot = this.resolveSlot(itemstack, list1);
            if (equipmentslot != null) {
               ItemStack itemstack1 = equipmentslot.limit(itemstack);
               this.setItemSlot(equipmentslot, itemstack1);
               Float f = p_341367_.get(equipmentslot);
               if (f != null) {
                  this.setDropChance(equipmentslot, f);
               }

               list1.add(equipmentslot);
            }
         }
      }
   }

   @Nullable
   default EquipmentSlot resolveSlot(ItemStack p_338225_, List<EquipmentSlot> p_338547_) {
      if (p_338225_.isEmpty()) {
         return null;
      } else {
         Equippable equippable = p_338225_.get(DataComponents.EQUIPPABLE);
         if (equippable != null) {
            EquipmentSlot equipmentslot = equippable.slot();
            if (!p_338547_.contains(equipmentslot)) {
               return equipmentslot;
            }
         } else if (!p_338547_.contains(EquipmentSlot.MAINHAND)) {
            return EquipmentSlot.MAINHAND;
         }

         return null;
      }
   }
}
