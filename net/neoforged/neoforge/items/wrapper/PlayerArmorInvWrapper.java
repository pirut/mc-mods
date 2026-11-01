package net.neoforged.neoforge.items.wrapper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class PlayerArmorInvWrapper extends RangedWrapper {
   private final Inventory inventoryPlayer;

   public PlayerArmorInvWrapper(Inventory inv) {
      super(new InvWrapper(inv), 36, 40);
      this.inventoryPlayer = inv;
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      EquipmentSlot equ = null;

      for (EquipmentSlot s : EquipmentSlot.values()) {
         if (s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && s.getIndex() == slot) {
            equ = s;
            break;
         }
      }

      return equ != null && slot < 4 && !stack.isEmpty() && stack.canEquip(equ, this.getInventoryPlayer().player)
         ? super.insertItem(slot, stack, simulate)
         : stack;
   }

   public Inventory getInventoryPlayer() {
      return this.inventoryPlayer;
   }
}
