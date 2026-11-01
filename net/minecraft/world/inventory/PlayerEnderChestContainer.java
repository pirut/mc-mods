package net.minecraft.world.inventory;

import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PlayerEnderChestContainer extends SimpleContainer {
   @Nullable
   private EnderChestBlockEntity activeChest;

   public PlayerEnderChestContainer() {
      super(27);
   }

   public void setActiveChest(EnderChestBlockEntity p_40106_) {
      this.activeChest = p_40106_;
   }

   public boolean isActiveChest(EnderChestBlockEntity p_150634_) {
      return this.activeChest == p_150634_;
   }

   public void fromSlots(ValueInput.TypedInputList<ItemStackWithSlot> p_421839_) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         this.setItem(i, ItemStack.EMPTY);
      }

      for (ItemStackWithSlot itemstackwithslot : p_421839_) {
         if (itemstackwithslot.isValidInContainer(this.getContainerSize())) {
            this.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
         }
      }
   }

   public void storeAsSlots(ValueOutput.TypedOutputList<ItemStackWithSlot> p_421593_) {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack itemstack = this.getItem(i);
         if (!itemstack.isEmpty()) {
            p_421593_.add(new ItemStackWithSlot(i, itemstack));
         }
      }
   }

   @Override
   public boolean stillValid(Player p_40104_) {
      return this.activeChest != null && !this.activeChest.stillValid(p_40104_) ? false : super.stillValid(p_40104_);
   }

   @Override
   public void startOpen(ContainerUser p_435470_) {
      if (this.activeChest != null) {
         this.activeChest.startOpen(p_435470_);
      }

      super.startOpen(p_435470_);
   }

   @Override
   public void stopOpen(ContainerUser p_435096_) {
      if (this.activeChest != null) {
         this.activeChest.stopOpen(p_435096_);
      }

      super.stopOpen(p_435096_);
      this.activeChest = null;
   }
}
