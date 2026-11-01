package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public interface Equipable {
   EquipmentSlot getEquipmentSlot();

   default Holder<SoundEvent> getEquipSound() {
      return SoundEvents.ARMOR_EQUIP_GENERIC;
   }

   default InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item p_270453_, Level p_270395_, Player p_270300_, InteractionHand p_270262_) {
      ItemStack $$4 = p_270300_.getItemInHand(p_270262_);
      EquipmentSlot $$5 = p_270300_.getEquipmentSlotForItem($$4);
      if (!p_270300_.canUseSlot($$5)) {
         return InteractionResultHolder.pass($$4);
      } else {
         ItemStack $$6 = p_270300_.getItemBySlot($$5);
         if ((!EnchantmentHelper.has($$6, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || p_270300_.isCreative()) && !ItemStack.matches($$4, $$6)) {
            if (!p_270395_.isClientSide()) {
               p_270300_.awardStat(Stats.ITEM_USED.get(p_270453_));
            }

            ItemStack $$7 = $$6.isEmpty() ? $$4 : $$6.copyAndClear();
            ItemStack $$8 = p_270300_.isCreative() ? $$4.copy() : $$4.copyAndClear();
            p_270300_.setItemSlot($$5, $$8);
            return InteractionResultHolder.sidedSuccess($$7, p_270395_.isClientSide());
         } else {
            return InteractionResultHolder.fail($$4);
         }
      }
   }

   @Nullable
   static Equipable get(ItemStack p_270317_) {
      if (p_270317_.getItem() instanceof Equipable $$1) {
         return $$1;
      } else {
         return p_270317_.getItem() instanceof BlockItem $$2 && $$2.getBlock() instanceof Equipable $$3 ? $$3 : null;
      }
   }
}
