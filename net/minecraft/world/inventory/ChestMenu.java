package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChestMenu extends AbstractContainerMenu {
   private static final int SLOTS_PER_ROW = 9;
   private final Container container;
   private final int containerRows;

   private ChestMenu(MenuType<?> p_39224_, int p_39225_, Inventory p_39226_, int p_39227_) {
      this(p_39224_, p_39225_, p_39226_, new SimpleContainer(9 * p_39227_), p_39227_);
   }

   public static ChestMenu oneRow(int p_39235_, Inventory p_39236_) {
      return new ChestMenu(MenuType.GENERIC_9x1, p_39235_, p_39236_, 1);
   }

   public static ChestMenu twoRows(int p_39244_, Inventory p_39245_) {
      return new ChestMenu(MenuType.GENERIC_9x2, p_39244_, p_39245_, 2);
   }

   public static ChestMenu threeRows(int p_39256_, Inventory p_39257_) {
      return new ChestMenu(MenuType.GENERIC_9x3, p_39256_, p_39257_, 3);
   }

   public static ChestMenu fourRows(int p_39259_, Inventory p_39260_) {
      return new ChestMenu(MenuType.GENERIC_9x4, p_39259_, p_39260_, 4);
   }

   public static ChestMenu fiveRows(int p_39263_, Inventory p_39264_) {
      return new ChestMenu(MenuType.GENERIC_9x5, p_39263_, p_39264_, 5);
   }

   public static ChestMenu sixRows(int p_39267_, Inventory p_39268_) {
      return new ChestMenu(MenuType.GENERIC_9x6, p_39267_, p_39268_, 6);
   }

   public static ChestMenu threeRows(int p_39238_, Inventory p_39239_, Container p_39240_) {
      return new ChestMenu(MenuType.GENERIC_9x3, p_39238_, p_39239_, p_39240_, 3);
   }

   public static ChestMenu sixRows(int p_39247_, Inventory p_39248_, Container p_39249_) {
      return new ChestMenu(MenuType.GENERIC_9x6, p_39247_, p_39248_, p_39249_, 6);
   }

   public ChestMenu(MenuType<?> p_39229_, int p_39230_, Inventory p_39231_, Container p_39232_, int p_39233_) {
      super(p_39229_, p_39230_);
      checkContainerSize(p_39232_, p_39233_ * 9);
      this.container = p_39232_;
      this.containerRows = p_39233_;
      p_39232_.startOpen(p_39231_.player);
      int $$5 = (this.containerRows - 4) * 18;

      for (int $$6 = 0; $$6 < this.containerRows; $$6++) {
         for (int $$7 = 0; $$7 < 9; $$7++) {
            this.addSlot(new Slot(p_39232_, $$7 + $$6 * 9, 8 + $$7 * 18, 18 + $$6 * 18));
         }
      }

      for (int $$8 = 0; $$8 < 3; $$8++) {
         for (int $$9 = 0; $$9 < 9; $$9++) {
            this.addSlot(new Slot(p_39231_, $$9 + $$8 * 9 + 9, 8 + $$9 * 18, 103 + $$8 * 18 + $$5));
         }
      }

      for (int $$10 = 0; $$10 < 9; $$10++) {
         this.addSlot(new Slot(p_39231_, $$10, 8 + $$10 * 18, 161 + $$5));
      }
   }

   @Override
   public boolean stillValid(Player p_39242_) {
      return this.container.stillValid(p_39242_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39253_, int p_39254_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39254_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_39254_ < this.containerRows * 9) {
            if (!this.moveItemStackTo($$4, this.containerRows * 9, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, this.containerRows * 9, false)) {
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
   public void removed(Player p_39251_) {
      super.removed(p_39251_);
      this.container.stopOpen(p_39251_);
   }

   public Container getContainer() {
      return this.container;
   }

   public int getRowCount() {
      return this.containerRows;
   }
}
