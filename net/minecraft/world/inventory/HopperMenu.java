package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HopperMenu extends AbstractContainerMenu {
   public static final int CONTAINER_SIZE = 5;
   private final Container hopper;

   public HopperMenu(int p_39640_, Inventory p_39641_) {
      this(p_39640_, p_39641_, new SimpleContainer(5));
   }

   public HopperMenu(int p_39643_, Inventory p_39644_, Container p_39645_) {
      super(MenuType.HOPPER, p_39643_);
      this.hopper = p_39645_;
      checkContainerSize(p_39645_, 5);
      p_39645_.startOpen(p_39644_.player);
      int $$3 = 51;

      for (int $$4 = 0; $$4 < 5; $$4++) {
         this.addSlot(new Slot(p_39645_, $$4, 44 + $$4 * 18, 20));
      }

      for (int $$5 = 0; $$5 < 3; $$5++) {
         for (int $$6 = 0; $$6 < 9; $$6++) {
            this.addSlot(new Slot(p_39644_, $$6 + $$5 * 9 + 9, 8 + $$6 * 18, $$5 * 18 + 51));
         }
      }

      for (int $$7 = 0; $$7 < 9; $$7++) {
         this.addSlot(new Slot(p_39644_, $$7, 8 + $$7 * 18, 109));
      }
   }

   @Override
   public boolean stillValid(Player p_39647_) {
      return this.hopper.stillValid(p_39647_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39651_, int p_39652_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39652_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_39652_ < this.hopper.getContainerSize()) {
            if (!this.moveItemStackTo($$4, this.hopper.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, this.hopper.getContainerSize(), false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }
      }

      return $$2;
   }

   @Override
   public void removed(Player p_39649_) {
      super.removed(p_39649_);
      this.hopper.stopOpen(p_39649_);
   }
}
