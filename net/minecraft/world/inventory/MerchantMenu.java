package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantMenu extends AbstractContainerMenu {
   protected static final int PAYMENT1_SLOT = 0;
   protected static final int PAYMENT2_SLOT = 1;
   protected static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private static final int SELLSLOT1_X = 136;
   private static final int SELLSLOT2_X = 162;
   private static final int BUYSLOT_X = 220;
   private static final int ROW_Y = 37;
   private final Merchant trader;
   private final MerchantContainer tradeContainer;
   private int merchantLevel;
   private boolean showProgressBar;
   private boolean canRestock;

   public MerchantMenu(int p_40033_, Inventory p_40034_) {
      this(p_40033_, p_40034_, new ClientSideMerchant(p_40034_.player));
   }

   public MerchantMenu(int p_40036_, Inventory p_40037_, Merchant p_40038_) {
      super(MenuType.MERCHANT, p_40036_);
      this.trader = p_40038_;
      this.tradeContainer = new MerchantContainer(p_40038_);
      this.addSlot(new Slot(this.tradeContainer, 0, 136, 37));
      this.addSlot(new Slot(this.tradeContainer, 1, 162, 37));
      this.addSlot(new MerchantResultSlot(p_40037_.player, p_40038_, this.tradeContainer, 2, 220, 37));

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 9; $$4++) {
            this.addSlot(new Slot(p_40037_, $$4 + $$3 * 9 + 9, 108 + $$4 * 18, 84 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 9; $$5++) {
         this.addSlot(new Slot(p_40037_, $$5, 108 + $$5 * 18, 142));
      }
   }

   public void setShowProgressBar(boolean p_40049_) {
      this.showProgressBar = p_40049_;
   }

   @Override
   public void slotsChanged(Container p_40040_) {
      this.tradeContainer.updateSellItem();
      super.slotsChanged(p_40040_);
   }

   public void setSelectionHint(int p_40064_) {
      this.tradeContainer.setSelectionHint(p_40064_);
   }

   @Override
   public boolean stillValid(Player p_40042_) {
      return this.trader.getTradingPlayer() == p_40042_;
   }

   public int getTraderXp() {
      return this.trader.getVillagerXp();
   }

   public int getFutureTraderXp() {
      return this.tradeContainer.getFutureXp();
   }

   public void setXp(int p_40067_) {
      this.trader.overrideXp(p_40067_);
   }

   public int getTraderLevel() {
      return this.merchantLevel;
   }

   public void setMerchantLevel(int p_40070_) {
      this.merchantLevel = p_40070_;
   }

   public void setCanRestock(boolean p_40059_) {
      this.canRestock = p_40059_;
   }

   public boolean canRestock() {
      return this.canRestock;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack p_40044_, Slot p_40045_) {
      return false;
   }

   @Override
   public ItemStack quickMoveStack(Player p_40053_, int p_40054_) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(p_40054_);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (p_40054_ == 2) {
            if (!this.moveItemStackTo($$4, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
            this.playTradeSound();
         } else if (p_40054_ != 0 && p_40054_ != 1) {
            if (p_40054_ >= 3 && p_40054_ < 30) {
               if (!this.moveItemStackTo($$4, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (p_40054_ >= 30 && p_40054_ < 39 && !this.moveItemStackTo($$4, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 3, 39, false)) {
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

         $$3.onTake(p_40053_, $$4);
      }

      return $$2;
   }

   private void playTradeSound() {
      if (!this.trader.isClientSide()) {
         Entity $$0 = (Entity)this.trader;
         $$0.level().playLocalSound($$0.getX(), $$0.getY(), $$0.getZ(), this.trader.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
      }
   }

   @Override
   public void removed(Player p_40051_) {
      super.removed(p_40051_);
      this.trader.setTradingPlayer(null);
      if (!this.trader.isClientSide()) {
         if (!p_40051_.isAlive() || p_40051_ instanceof ServerPlayer && ((ServerPlayer)p_40051_).hasDisconnected()) {
            ItemStack $$1 = this.tradeContainer.removeItemNoUpdate(0);
            if (!$$1.isEmpty()) {
               p_40051_.drop($$1, false);
            }

            $$1 = this.tradeContainer.removeItemNoUpdate(1);
            if (!$$1.isEmpty()) {
               p_40051_.drop($$1, false);
            }
         } else if (p_40051_ instanceof ServerPlayer) {
            p_40051_.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(0));
            p_40051_.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(1));
         }
      }
   }

   public void tryMoveItems(int p_40073_) {
      if (p_40073_ >= 0 && this.getOffers().size() > p_40073_) {
         ItemStack $$1 = this.tradeContainer.getItem(0);
         if (!$$1.isEmpty()) {
            if (!this.moveItemStackTo($$1, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(0, $$1);
         }

         ItemStack $$2 = this.tradeContainer.getItem(1);
         if (!$$2.isEmpty()) {
            if (!this.moveItemStackTo($$2, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(1, $$2);
         }

         if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
            MerchantOffer $$3 = this.getOffers().get(p_40073_);
            this.moveFromInventoryToPaymentSlot(0, $$3.getItemCostA());
            $$3.getItemCostB().ifPresent(p_330236_ -> this.moveFromInventoryToPaymentSlot(1, p_330236_));
         }
      }
   }

   private void moveFromInventoryToPaymentSlot(int p_40061_, ItemCost p_331779_) {
      for (int $$2 = 3; $$2 < 39; $$2++) {
         ItemStack $$3 = this.slots.get($$2).getItem();
         if (!$$3.isEmpty() && p_331779_.test($$3)) {
            ItemStack $$4 = this.tradeContainer.getItem(p_40061_);
            if ($$4.isEmpty() || ItemStack.isSameItemSameComponents($$3, $$4)) {
               int $$5 = $$3.getMaxStackSize();
               int $$6 = Math.min($$5 - $$4.getCount(), $$3.getCount());
               ItemStack $$7 = $$3.copyWithCount($$4.getCount() + $$6);
               $$3.shrink($$6);
               this.tradeContainer.setItem(p_40061_, $$7);
               if ($$7.getCount() >= $$5) {
                  break;
               }
            }
         }
      }
   }

   public void setOffers(MerchantOffers p_40047_) {
      this.trader.overrideOffers(p_40047_);
   }

   public MerchantOffers getOffers() {
      return this.trader.getOffers();
   }

   public boolean showProgressBar() {
      return this.showProgressBar;
   }
}
