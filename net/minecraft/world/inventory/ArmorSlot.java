package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

class ArmorSlot extends Slot {
   private final LivingEntity owner;
   private final EquipmentSlot slot;
   @Nullable
   private final Identifier emptyIcon;

   public ArmorSlot(
      Container p_345898_, LivingEntity p_345231_, EquipmentSlot p_345728_, int p_345121_, int p_346000_, int p_346095_, @Nullable Identifier p_469046_
   ) {
      super(p_345898_, p_345121_, p_346000_, p_346095_);
      this.owner = p_345231_;
      this.slot = p_345728_;
      this.emptyIcon = p_469046_;
   }

   @Override
   public void setByPlayer(ItemStack p_345031_, ItemStack p_344961_) {
      this.owner.onEquipItem(this.slot, p_344961_, p_345031_);
      super.setByPlayer(p_345031_, p_344961_);
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean mayPlace(ItemStack p_345029_) {
      return p_345029_.canEquip(this.slot, this.owner);
   }

   @Override
   public boolean isActive() {
      return this.owner.canUseSlot(this.slot);
   }

   @Override
   public boolean mayPickup(Player p_345575_) {
      ItemStack itemstack = this.getItem();
      return !itemstack.isEmpty() && !p_345575_.isCreative() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
         ? false
         : super.mayPickup(p_345575_);
   }

   @Nullable
   @Override
   public Identifier getNoItemIcon() {
      return this.emptyIcon;
   }
}
