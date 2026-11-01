package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DispenserMenu extends AbstractContainerMenu {
   private static final int SLOT_COUNT = 9;
   private static final int INV_SLOT_START = 9;
   private static final int INV_SLOT_END = 36;
   private static final int USE_ROW_SLOT_START = 36;
   private static final int USE_ROW_SLOT_END = 45;
   private final Container dispenser;

   public DispenserMenu(int p_39433_, Inventory p_39434_) {
      this(p_39433_, p_39434_, new SimpleContainer(9));
   }

   public DispenserMenu(int p_39436_, Inventory p_39437_, Container p_39438_) {
      super(MenuType.GENERIC_3x3, p_39436_);
      checkContainerSize(p_39438_, 9);
      this.dispenser = p_39438_;
      p_39438_.startOpen(p_39437_.player);

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 3; $$4++) {
            this.addSlot(new Slot(p_39438_, $$4 + $$3 * 3, 62 + $$4 * 18, 17 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 3; $$5++) {
         for (int $$6 = 0; $$6 < 9; $$6++) {
            this.addSlot(new Slot(p_39437_, $$6 + $$5 * 9 + 9, 8 + $$6 * 18, 84 + $$5 * 18));
         }
      }

      for (int $$7 = 0; $$7 < 9; $$7++) {
         this.addSlot(new Slot(p_39437_, $$7, 8 + $$7 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player p_39440_) {
      return this.dispenser.stillValid(p_39440_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39444_, int p_39445_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39445_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_39445_ < 9) {
            if (!this.moveItemStackTo($$4, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }

         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(p_39444_, $$4);
      }

      return $$2;
   }

   @Override
   public void removed(Player p_39442_) {
      super.removed(p_39442_);
      this.dispenser.stopOpen(p_39442_);
   }
}
