package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ShulkerBoxMenu extends AbstractContainerMenu {
   private static final int CONTAINER_SIZE = 27;
   private final Container container;

   public ShulkerBoxMenu(int p_40188_, Inventory p_40189_) {
      this(p_40188_, p_40189_, new SimpleContainer(27));
   }

   public ShulkerBoxMenu(int p_40191_, Inventory p_40192_, Container p_40193_) {
      super(MenuType.SHULKER_BOX, p_40191_);
      checkContainerSize(p_40193_, 27);
      this.container = p_40193_;
      p_40193_.startOpen(p_40192_.player);
      int $$3 = 3;
      int $$4 = 9;

      for (int $$5 = 0; $$5 < 3; $$5++) {
         for (int $$6 = 0; $$6 < 9; $$6++) {
            this.addSlot(new ShulkerBoxSlot(p_40193_, $$6 + $$5 * 9, 8 + $$6 * 18, 18 + $$5 * 18));
         }
      }

      for (int $$7 = 0; $$7 < 3; $$7++) {
         for (int $$8 = 0; $$8 < 9; $$8++) {
            this.addSlot(new Slot(p_40192_, $$8 + $$7 * 9 + 9, 8 + $$8 * 18, 84 + $$7 * 18));
         }
      }

      for (int $$9 = 0; $$9 < 9; $$9++) {
         this.addSlot(new Slot(p_40192_, $$9, 8 + $$9 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player p_40195_) {
      return this.container.stillValid(p_40195_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_40199_, int p_40200_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_40200_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_40200_ < this.container.getContainerSize()) {
            if (!this.moveItemStackTo($$4, this.container.getContainerSize(), this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, this.container.getContainerSize(), false)) {
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
   public void removed(Player p_40197_) {
      super.removed(p_40197_);
      this.container.stopOpen(p_40197_);
   }
}
