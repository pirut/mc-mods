package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DispenserBlockEntity extends RandomizableContainerBlockEntity {
   public static final int CONTAINER_SIZE = 9;
   private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

   protected DispenserBlockEntity(BlockEntityType<?> p_155489_, BlockPos p_155490_, BlockState p_155491_) {
      super(p_155489_, p_155490_, p_155491_);
   }

   public DispenserBlockEntity(BlockPos p_155493_, BlockState p_155494_) {
      this(BlockEntityType.DISPENSER, p_155493_, p_155494_);
   }

   @Override
   public int getContainerSize() {
      return 9;
   }

   public int getRandomSlot(RandomSource p_222762_) {
      this.unpackLootTable(null);
      int $$1 = -1;
      int $$2 = 1;

      for (int $$3 = 0; $$3 < this.items.size(); $$3++) {
         if (!this.items.get($$3).isEmpty() && p_222762_.nextInt($$2++) == 0) {
            $$1 = $$3;
         }
      }

      return $$1;
   }

   public ItemStack insertItem(ItemStack p_347519_) {
      int $$1 = this.getMaxStackSize(p_347519_);

      for (int $$2 = 0; $$2 < this.items.size(); $$2++) {
         ItemStack $$3 = this.items.get($$2);
         if ($$3.isEmpty() || ItemStack.isSameItemSameComponents(p_347519_, $$3)) {
            int $$4 = Math.min(p_347519_.getCount(), $$1 - $$3.getCount());
            if ($$4 > 0) {
               if ($$3.isEmpty()) {
                  this.setItem($$2, p_347519_.split($$4));
               } else {
                  p_347519_.shrink($$4);
                  $$3.grow($$4);
               }
            }

            if (p_347519_.isEmpty()) {
               break;
            }
         }
      }

      return p_347519_;
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("container.dispenser");
   }

   @Override
   protected void loadAdditional(CompoundTag p_155496_, HolderLookup.Provider p_323868_) {
      super.loadAdditional(p_155496_, p_323868_);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(p_155496_)) {
         ContainerHelper.loadAllItems(p_155496_, this.items, p_323868_);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag p_187498_, HolderLookup.Provider p_323791_) {
      super.saveAdditional(p_187498_, p_323791_);
      if (!this.trySaveLootTable(p_187498_)) {
         ContainerHelper.saveAllItems(p_187498_, this.items, p_323791_);
      }
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> p_59243_) {
      this.items = p_59243_;
   }

   @Override
   protected AbstractContainerMenu createMenu(int p_59235_, Inventory p_59236_) {
      return new DispenserMenu(p_59235_, p_59236_, this);
   }
}
