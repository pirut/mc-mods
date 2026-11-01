package net.minecraft.world.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemCombinerMenu extends AbstractContainerMenu {
   private static final int INVENTORY_SLOTS_PER_ROW = 9;
   private static final int INVENTORY_SLOTS_PER_COLUMN = 3;
   protected final ContainerLevelAccess access;
   protected final Player player;
   protected final Container inputSlots;
   private final List<Integer> inputSlotIndexes;
   protected final ResultContainer resultSlots = new ResultContainer();
   private final int resultSlotIndex;

   protected abstract boolean mayPickup(Player var1, boolean var2);

   protected abstract void onTake(Player var1, ItemStack var2);

   protected abstract boolean isValidBlock(BlockState var1);

   public ItemCombinerMenu(@Nullable MenuType<?> p_39773_, int p_39774_, Inventory p_39775_, ContainerLevelAccess p_39776_) {
      super(p_39773_, p_39774_);
      this.access = p_39776_;
      this.player = p_39775_.player;
      ItemCombinerMenuSlotDefinition $$4 = this.createInputSlotDefinitions();
      this.inputSlots = this.createContainer($$4.getNumOfInputSlots());
      this.inputSlotIndexes = $$4.getInputSlotIndexes();
      this.resultSlotIndex = $$4.getResultSlotIndex();
      this.createInputSlots($$4);
      this.createResultSlot($$4);
      this.createInventorySlots(p_39775_);
   }

   private void createInputSlots(ItemCombinerMenuSlotDefinition p_267172_) {
      for (final ItemCombinerMenuSlotDefinition.SlotDefinition $$1 : p_267172_.getSlots()) {
         this.addSlot(new Slot(this.inputSlots, $$1.slotIndex(), $$1.x(), $$1.y()) {
            @Override
            public boolean mayPlace(ItemStack p_267156_) {
               return $$1.mayPlace().test(p_267156_);
            }
         });
      }
   }

   private void createResultSlot(ItemCombinerMenuSlotDefinition p_267000_) {
      this.addSlot(new Slot(this.resultSlots, p_267000_.getResultSlot().slotIndex(), p_267000_.getResultSlot().x(), p_267000_.getResultSlot().y()) {
         @Override
         public boolean mayPlace(ItemStack p_39818_) {
            return false;
         }

         @Override
         public boolean mayPickup(Player p_39813_) {
            return ItemCombinerMenu.this.mayPickup(p_39813_, this.hasItem());
         }

         @Override
         public void onTake(Player p_150604_, ItemStack p_150605_) {
            ItemCombinerMenu.this.onTake(p_150604_, p_150605_);
         }
      });
   }

   private void createInventorySlots(Inventory p_267325_) {
      for (int $$1 = 0; $$1 < 3; $$1++) {
         for (int $$2 = 0; $$2 < 9; $$2++) {
            this.addSlot(new Slot(p_267325_, $$2 + $$1 * 9 + 9, 8 + $$2 * 18, 84 + $$1 * 18));
         }
      }

      for (int $$3 = 0; $$3 < 9; $$3++) {
         this.addSlot(new Slot(p_267325_, $$3, 8 + $$3 * 18, 142));
      }
   }

   public abstract void createResult();

   protected abstract ItemCombinerMenuSlotDefinition createInputSlotDefinitions();

   private SimpleContainer createContainer(int p_267204_) {
      return new SimpleContainer(p_267204_) {
         @Override
         public void setChanged() {
            super.setChanged();
            ItemCombinerMenu.this.slotsChanged(this);
         }
      };
   }

   @Override
   public void slotsChanged(Container p_39778_) {
      super.slotsChanged(p_39778_);
      if (p_39778_ == this.inputSlots) {
         this.createResult();
      }
   }

   @Override
   public void removed(Player p_39790_) {
      super.removed(p_39790_);
      this.access.execute((p_39796_, p_39797_) -> this.clearContainer(p_39790_, this.inputSlots));
   }

   @Override
   public boolean stillValid(Player p_39780_) {
      return this.access
         .evaluate(
            (p_339525_, p_339526_) -> !this.isValidBlock(p_339525_.getBlockState(p_339526_)) ? false : p_39780_.canInteractWithBlock(p_339526_, 4.0), true
         );
   }

   @Override
   public ItemStack quickMoveStack(Player p_39792_, int p_39793_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39793_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         int $$5 = this.getInventorySlotStart();
         int $$6 = this.getUseRowEnd();
         if (p_39793_ == this.getResultSlot()) {
            if (!this.moveItemStackTo($$4, $$5, $$6, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (this.inputSlotIndexes.contains(p_39793_)) {
            if (!this.moveItemStackTo($$4, $$5, $$6, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.canMoveIntoInputSlots($$4) && p_39793_ >= this.getInventorySlotStart() && p_39793_ < this.getUseRowEnd()) {
            int $$7 = this.getSlotToQuickMoveTo($$2);
            if (!this.moveItemStackTo($$4, $$7, this.getResultSlot(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39793_ >= this.getInventorySlotStart() && p_39793_ < this.getInventorySlotEnd()) {
            if (!this.moveItemStackTo($$4, this.getUseRowStart(), this.getUseRowEnd(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (p_39793_ >= this.getUseRowStart()
            && p_39793_ < this.getUseRowEnd()
            && !this.moveItemStackTo($$4, this.getInventorySlotStart(), this.getInventorySlotEnd(), false)) {
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

         $$3.onTake(p_39792_, $$4);
      }

      return $$2;
   }

   protected boolean canMoveIntoInputSlots(ItemStack p_39787_) {
      return true;
   }

   public int getSlotToQuickMoveTo(ItemStack p_267159_) {
      return this.inputSlots.isEmpty() ? 0 : this.inputSlotIndexes.get(0);
   }

   public int getResultSlot() {
      return this.resultSlotIndex;
   }

   private int getInventorySlotStart() {
      return this.getResultSlot() + 1;
   }

   private int getInventorySlotEnd() {
      return this.getInventorySlotStart() + 27;
   }

   private int getUseRowStart() {
      return this.getInventorySlotEnd();
   }

   private int getUseRowEnd() {
      return this.getUseRowStart() + 9;
   }
}
