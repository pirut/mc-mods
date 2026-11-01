package net.minecraft.world.entity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
   ItemStack get();

   boolean set(ItemStack var1);

   static SlotAccess of(final Supplier<ItemStack> p_340997_, final Consumer<ItemStack> p_341182_) {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return p_340997_.get();
         }

         @Override
         public boolean set(ItemStack p_147314_) {
            p_341182_.accept(p_147314_);
            return true;
         }
      };
   }

   static SlotAccess forEquipmentSlot(final LivingEntity p_147303_, final EquipmentSlot p_147304_, final Predicate<ItemStack> p_147305_) {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return p_147303_.getItemBySlot(p_147304_);
         }

         @Override
         public boolean set(ItemStack p_147324_) {
            if (!p_147305_.test(p_147324_)) {
               return false;
            } else {
               p_147303_.setItemSlot(p_147304_, p_147324_);
               return true;
            }
         }
      };
   }

   static SlotAccess forEquipmentSlot(LivingEntity p_147300_, EquipmentSlot p_147301_) {
      return forEquipmentSlot(p_147300_, p_147301_, p_147310_ -> true);
   }

   static SlotAccess forListElement(final List<ItemStack> p_461248_, final int p_460831_) {
      return new SlotAccess() {
         @Override
         public ItemStack get() {
            return p_461248_.get(p_460831_);
         }

         @Override
         public boolean set(ItemStack p_147334_) {
            p_461248_.set(p_460831_, p_147334_);
            return true;
         }
      };
   }
}
