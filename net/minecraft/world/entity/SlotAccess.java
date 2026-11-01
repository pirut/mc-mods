package net.minecraft.world.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
   ItemStack get();

   boolean set(ItemStack item);

   static SlotAccess of(Supplier<ItemStack> getter, Consumer<ItemStack> setter) {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return getter.get();
         }

         @Override
         public boolean set(ItemStack item) {
            setter.accept(item);
            return true;
         }
      };
   }

   static SlotAccess forEquipmentSlot(LivingEntity entity, EquipmentSlot slot, Predicate<ItemStack> stackFilter) {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return entity.getItemBySlot(slot);
         }

         @Override
         public boolean set(ItemStack item) {
            if (!stackFilter.test(item)) {
               return false;
            } else {
               entity.setItemSlot(slot, item);
               return true;
            }
         }
      };
   }

   static SlotAccess forEquipmentSlot(LivingEntity entity, EquipmentSlot slot) {
      return forEquipmentSlot(entity, slot, itemStack -> true);
   }

   static SlotAccess forListElement(List<ItemStack> items, int index) {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return items.get(index);
         }

         @Override
         public boolean set(ItemStack item) {
            items.set(index, item);
            return true;
         }
      };
   }
}
