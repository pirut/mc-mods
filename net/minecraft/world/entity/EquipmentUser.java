package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

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
      if (!p_340873_.equals(BuiltInLootTables.EMPTY)) {
         LootTable $$4 = p_338202_.getLevel().getServer().reloadableRegistries().getLootTable(p_340873_);
         if ($$4 != LootTable.EMPTY) {
            List<ItemStack> $$5 = $$4.getRandomItems(p_338202_, p_341024_);
            List<EquipmentSlot> $$6 = new ArrayList<>();

            for (ItemStack $$7 : $$5) {
               EquipmentSlot $$8 = this.resolveSlot($$7, $$6);
               if ($$8 != null) {
                  ItemStack $$9 = $$8.limit($$7);
                  this.setItemSlot($$8, $$9);
                  Float $$10 = p_341367_.get($$8);
                  if ($$10 != null) {
                     this.setDropChance($$8, $$10);
                  }

                  $$6.add($$8);
               }
            }
         }
      }
   }

   @Nullable
   default EquipmentSlot resolveSlot(ItemStack p_338225_, List<EquipmentSlot> p_338547_) {
      if (p_338225_.isEmpty()) {
         return null;
      } else {
         Equipable $$2 = Equipable.get(p_338225_);
         if ($$2 != null) {
            EquipmentSlot $$3 = $$2.getEquipmentSlot();
            if (!p_338547_.contains($$3)) {
               return $$3;
            }
         } else if (!p_338547_.contains(EquipmentSlot.MAINHAND)) {
            return EquipmentSlot.MAINHAND;
         }

         return null;
      }
   }
}
