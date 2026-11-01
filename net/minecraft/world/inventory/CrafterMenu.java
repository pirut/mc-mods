package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;

public class CrafterMenu extends AbstractContainerMenu implements ContainerListener {
   protected static final int SLOT_COUNT = 9;
   private static final int INV_SLOT_START = 9;
   private static final int INV_SLOT_END = 36;
   private static final int USE_ROW_SLOT_START = 36;
   private static final int USE_ROW_SLOT_END = 45;
   private final ResultContainer resultContainer = new ResultContainer();
   private final ContainerData containerData;
   private final Player player;
   private final CraftingContainer container;

   public CrafterMenu(int p_307357_, Inventory p_307257_) {
      super(MenuType.CRAFTER_3x3, p_307357_);
      this.player = p_307257_.player;
      this.containerData = new SimpleContainerData(10);
      this.container = new TransientCraftingContainer(this, 3, 3);
      this.addSlots(p_307257_);
   }

   public CrafterMenu(int p_307363_, Inventory p_307517_, CraftingContainer p_307449_, ContainerData p_307285_) {
      super(MenuType.CRAFTER_3x3, p_307363_);
      this.player = p_307517_.player;
      this.containerData = p_307285_;
      this.container = p_307449_;
      checkContainerSize(p_307449_, 9);
      p_307449_.startOpen(p_307517_.player);
      this.addSlots(p_307517_);
      this.addSlotListener(this);
   }

   private void addSlots(Inventory p_307214_) {
      for (int $$1 = 0; $$1 < 3; $$1++) {
         for (int $$2 = 0; $$2 < 3; $$2++) {
            int $$3 = $$2 + $$1 * 3;
            this.addSlot(new CrafterSlot(this.container, $$3, 26 + $$2 * 18, 17 + $$1 * 18, this));
         }
      }

      for (int $$4 = 0; $$4 < 3; $$4++) {
         for (int $$5 = 0; $$5 < 9; $$5++) {
            this.addSlot(new Slot(p_307214_, $$5 + $$4 * 9 + 9, 8 + $$5 * 18, 84 + $$4 * 18));
         }
      }

      for (int $$6 = 0; $$6 < 9; $$6++) {
         this.addSlot(new Slot(p_307214_, $$6, 8 + $$6 * 18, 142));
      }

      this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
      this.addDataSlots(this.containerData);
      this.refreshRecipeResult();
   }

   public void setSlotState(int p_307384_, boolean p_307499_) {
      CrafterSlot $$2 = (CrafterSlot)this.getSlot(p_307384_);
      this.containerData.set($$2.index, p_307499_ ? 0 : 1);
      this.broadcastChanges();
   }

   public boolean isSlotDisabled(int p_307609_) {
      return p_307609_ > -1 && p_307609_ < 9 ? this.containerData.get(p_307609_) == 1 : false;
   }

   public boolean isPowered() {
      return this.containerData.get(9) == 1;
   }

   @Override
   public ItemStack quickMoveStack(Player p_307459_, int p_307204_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_307204_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_307204_ < 9) {
            if (!this.moveItemStackTo($$4, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.set(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }

         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(p_307459_, $$4);
      }

      return $$2;
   }

   @Override
   public boolean stillValid(Player p_307229_) {
      return this.container.stillValid(p_307229_);
   }

   private void refreshRecipeResult() {
      if (this.player instanceof ServerPlayer $$0) {
         Level $$1 = $$0.level();
         CraftingInput $$2 = this.container.asCraftInput();
         ItemStack $$3 = CrafterBlock.getPotentialResults($$1, $$2)
            .map(p_344359_ -> p_344359_.value().assemble($$2, $$1.registryAccess()))
            .orElse(ItemStack.EMPTY);
         this.resultContainer.setItem(0, $$3);
      }
   }

   public Container getContainer() {
      return this.container;
   }

   @Override
   public void slotChanged(AbstractContainerMenu p_307332_, int p_307437_, ItemStack p_307233_) {
      this.refreshRecipeResult();
   }

   @Override
   public void dataChanged(AbstractContainerMenu p_307424_, int p_307646_, int p_307221_) {
   }
}
