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

   public ArmorSlot(Container container, LivingEntity owner, EquipmentSlot slot, int slotIndex, int x, int y, @Nullable Identifier emptyIcon) {
      super(container, slotIndex, x, y);
      this.owner = owner;
      this.slot = slot;
      this.emptyIcon = emptyIcon;
   }

   @Override
   public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
      this.owner.onEquipItem(this.slot, oldStack, newStack);
      super.setByPlayer(newStack, oldStack);
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean mayPlace(ItemStack stack) {
      return this.owner.isEquippableInSlot(stack, this.slot);
   }

   @Override
   public boolean isActive() {
      return this.owner.canUseSlot(this.slot);
   }

   @Override
   public boolean mayPickup(Player player) {
      ItemStack itemStack = this.getItem();
      return !itemStack.isEmpty() && !player.isCreative() && EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
         ? false
         : super.mayPickup(player);
   }

   @Nullable
   @Override
   public Identifier getNoItemIcon() {
      return this.emptyIcon;
   }
}
