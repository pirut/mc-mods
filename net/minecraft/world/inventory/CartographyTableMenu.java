package net.minecraft.world.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class CartographyTableMenu extends AbstractContainerMenu {
   public static final int MAP_SLOT = 0;
   public static final int ADDITIONAL_SLOT = 1;
   public static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private final ContainerLevelAccess access;
   long lastSoundTime;
   public final Container container = new SimpleContainer(2) {
      @Override
      public void setChanged() {
         CartographyTableMenu.this.slotsChanged(this);
         super.setChanged();
      }
   };
   private final ResultContainer resultContainer = new ResultContainer() {
      @Override
      public void setChanged() {
         CartographyTableMenu.this.slotsChanged(this);
         super.setChanged();
      }
   };

   public CartographyTableMenu(int p_39140_, Inventory p_39141_) {
      this(p_39140_, p_39141_, ContainerLevelAccess.NULL);
   }

   public CartographyTableMenu(int p_39143_, Inventory p_39144_, final ContainerLevelAccess p_39145_) {
      super(MenuType.CARTOGRAPHY_TABLE, p_39143_);
      this.access = p_39145_;
      this.addSlot(new Slot(this.container, 0, 15, 15) {
         @Override
         public boolean mayPlace(ItemStack p_39194_) {
            return p_39194_.is(Items.FILLED_MAP);
         }
      });
      this.addSlot(new Slot(this.container, 1, 15, 52) {
         @Override
         public boolean mayPlace(ItemStack p_39203_) {
            return p_39203_.is(Items.PAPER) || p_39203_.is(Items.MAP) || p_39203_.is(Items.GLASS_PANE);
         }
      });
      this.addSlot(new Slot(this.resultContainer, 2, 145, 39) {
         @Override
         public boolean mayPlace(ItemStack p_39217_) {
            return false;
         }

         @Override
         public void onTake(Player p_150509_, ItemStack p_150510_) {
            CartographyTableMenu.this.slots.get(0).remove(1);
            CartographyTableMenu.this.slots.get(1).remove(1);
            p_150510_.getItem().onCraftedBy(p_150510_, p_150509_.level(), p_150509_);
            p_39145_.execute((p_39219_, p_39220_) -> {
               long $$2 = p_39219_.getGameTime();
               if (CartographyTableMenu.this.lastSoundTime != $$2) {
                  p_39219_.playSound(null, p_39220_, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                  CartographyTableMenu.this.lastSoundTime = $$2;
               }
            });
            super.onTake(p_150509_, p_150510_);
         }
      });

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 9; $$4++) {
            this.addSlot(new Slot(p_39144_, $$4 + $$3 * 9 + 9, 8 + $$4 * 18, 84 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 9; $$5++) {
         this.addSlot(new Slot(p_39144_, $$5, 8 + $$5 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player p_39149_) {
      return stillValid(this.access, p_39149_, Blocks.CARTOGRAPHY_TABLE);
   }

   @Override
   public void slotsChanged(Container p_39147_) {
      ItemStack $$1 = this.container.getItem(0);
      ItemStack $$2 = this.container.getItem(1);
      ItemStack $$3 = this.resultContainer.getItem(2);
      if ($$3.isEmpty() || !$$1.isEmpty() && !$$2.isEmpty()) {
         if (!$$1.isEmpty() && !$$2.isEmpty()) {
            this.setupResultSlot($$1, $$2, $$3);
         }
      } else {
         this.resultContainer.removeItemNoUpdate(2);
      }
   }

   private void setupResultSlot(ItemStack p_39163_, ItemStack p_39164_, ItemStack p_39165_) {
      this.access.execute((p_279039_, p_279040_) -> {
         MapItemSavedData $$5 = MapItem.getSavedData(p_39163_, p_279039_);
         if ($$5 != null) {
            ItemStack $$6;
            if (p_39164_.is(Items.PAPER) && !$$5.locked && $$5.scale < 4) {
               $$6 = p_39163_.copyWithCount(1);
               $$6.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
               this.broadcastChanges();
            } else if (p_39164_.is(Items.GLASS_PANE) && !$$5.locked) {
               $$6 = p_39163_.copyWithCount(1);
               $$6.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.LOCK);
               this.broadcastChanges();
            } else {
               if (!p_39164_.is(Items.MAP)) {
                  this.resultContainer.removeItemNoUpdate(2);
                  this.broadcastChanges();
                  return;
               }

               $$6 = p_39163_.copyWithCount(2);
               this.broadcastChanges();
            }

            if (!ItemStack.matches($$6, p_39165_)) {
               this.resultContainer.setItem(2, $$6);
               this.broadcastChanges();
            }
         }
      });
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_39160_, Slot p_39161_) {
      return p_39161_.container != this.resultContainer && super.canTakeItemForPickAll(p_39160_, p_39161_);
   }

   @Override
   public ItemStack quickMoveStack(Player p_39175_, int p_39176_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_39176_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_39176_ == 2) {
            $$4.getItem().onCraftedBy($$4, p_39175_.level(), p_39175_);
            if (!this.moveItemStackTo($$4, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (p_39176_ != 1 && p_39176_ != 0) {
            if ($$4.is(Items.FILLED_MAP)) {
               if (!this.moveItemStackTo($$4, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!$$4.is(Items.PAPER) && !$$4.is(Items.MAP) && !$$4.is(Items.GLASS_PANE)) {
               if (p_39176_ >= 3 && p_39176_ < 30) {
                  if (!this.moveItemStackTo($$4, 30, 39, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (p_39176_ >= 30 && p_39176_ < 39 && !this.moveItemStackTo($$4, 3, 30, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo($$4, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         }

         $$3.setChanged();
         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(p_39175_, $$4);
         this.broadcastChanges();
      }

      return $$2;
   }

   @Override
   public void removed(Player p_39173_) {
      super.removed(p_39173_);
      this.resultContainer.removeItemNoUpdate(2);
      this.access.execute((p_39152_, p_39153_) -> this.clearContainer(p_39173_, this.container));
   }
}
