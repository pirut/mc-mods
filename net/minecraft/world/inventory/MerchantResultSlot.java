package net.minecraft.world.inventory;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class MerchantResultSlot extends Slot {
   private final MerchantContainer slots;
   private final Player player;
   private int removeCount;
   private final Merchant merchant;

   public MerchantResultSlot(Player p_40083_, Merchant p_40084_, MerchantContainer p_40085_, int p_40086_, int p_40087_, int p_40088_) {
      super(p_40085_, p_40086_, p_40087_, p_40088_);
      this.player = p_40083_;
      this.merchant = p_40084_;
      this.slots = p_40085_;
   }

   @Override
   public boolean mayPlace(ItemStack p_40095_) {
      return false;
   }

   @Override
   public ItemStack remove(int p_40090_) {
      if (this.hasItem()) {
         this.removeCount = this.removeCount + Math.min(p_40090_, this.getItem().getCount());
      }

      return super.remove(p_40090_);
   }

   @Override
   protected void onQuickCraft(ItemStack p_40097_, int p_40098_) {
      this.removeCount += p_40098_;
      this.checkTakeAchievements(p_40097_);
   }

   @Override
   protected void checkTakeAchievements(ItemStack p_40100_) {
      p_40100_.onCraftedBy(this.player.level(), this.player, this.removeCount);
      this.removeCount = 0;
   }

   @Override
   public void onTake(Player p_150631_, ItemStack p_150632_) {
      this.checkTakeAchievements(p_150632_);
      MerchantOffer $$2 = this.slots.getActiveOffer();
      if ($$2 != null) {
         ItemStack $$3 = this.slots.getItem(0);
         ItemStack $$4 = this.slots.getItem(1);
         if ($$2.take($$3, $$4) || $$2.take($$4, $$3)) {
            this.merchant.notifyTrade($$2);
            p_150631_.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.slots.setItem(0, $$3);
            this.slots.setItem(1, $$4);
         }

         this.merchant.overrideXp(this.merchant.getVillagerXp() + $$2.getXp());
      }
   }
}
