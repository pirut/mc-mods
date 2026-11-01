package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;

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

   @Override
   public void fromTag(ListTag p_40108_, HolderLookup.Provider p_330977_) {
      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         this.setItem($$2, ItemStack.EMPTY);
      }

      for (int $$3 = 0; $$3 < p_40108_.size(); $$3++) {
         CompoundTag $$4 = p_40108_.getCompound($$3);
         int $$5 = $$4.getByte("Slot") & 255;
         if ($$5 >= 0 && $$5 < this.getContainerSize()) {
            this.setItem($$5, ItemStack.parse(p_330977_, $$4).orElse(ItemStack.EMPTY));
         }
      }
   }

   @Override
   public ListTag createTag(HolderLookup.Provider p_330801_) {
      ListTag $$1 = new ListTag();

      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         ItemStack $$3 = this.getItem($$2);
         if (!$$3.isEmpty()) {
            CompoundTag $$4 = new CompoundTag();
            $$4.putByte("Slot", (byte)$$2);
            $$1.add($$3.save(p_330801_, $$4));
         }
      }

      return $$1;
   }

   @Override
   public boolean stillValid(Player p_40104_) {
      return this.activeChest != null && !this.activeChest.stillValid(p_40104_) ? false : super.stillValid(p_40104_);
   }

   @Override
   public void startOpen(Player p_40112_) {
      if (this.activeChest != null) {
         this.activeChest.startOpen(p_40112_);
      }

      super.startOpen(p_40112_);
   }

   @Override
   public void stopOpen(Player p_40110_) {
      if (this.activeChest != null) {
         this.activeChest.stopOpen(p_40110_);
      }

      super.stopOpen(p_40110_);
      this.activeChest = null;
   }
}
