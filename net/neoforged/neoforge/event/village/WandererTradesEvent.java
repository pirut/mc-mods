package net.neoforged.neoforge.event.village;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.neoforged.bus.api.Event;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

public class WandererTradesEvent extends Event {
   protected List<VillagerTrades.ItemListing> buying;
   protected int buyingAmount;
   protected List<VillagerTrades.ItemListing> rare;
   protected int rareAmount;
   protected List<VillagerTrades.ItemListing> generic;
   protected int genericAmount;
   protected List<Pair<List<VillagerTrades.ItemListing>, Integer>> additionalTrades;
   private final HolderLookup.Provider registries;

   @Internal
   public WandererTradesEvent(
      List<VillagerTrades.ItemListing> buying,
      int buyingAmount,
      List<VillagerTrades.ItemListing> rare,
      int rareAmount,
      List<VillagerTrades.ItemListing> generic,
      int genericAmount,
      List<Pair<List<VillagerTrades.ItemListing>, Integer>> additionalTrades,
      HolderLookup.Provider registries
   ) {
      this.buying = buying;
      this.buyingAmount = buyingAmount;
      this.rare = rare;
      this.rareAmount = rareAmount;
      this.generic = generic;
      this.genericAmount = genericAmount;
      this.additionalTrades = additionalTrades;
      this.registries = registries;
   }

   public List<VillagerTrades.ItemListing> getBuyingTrades() {
      return this.buying;
   }

   public int getBuyingAmount() {
      return this.buyingAmount;
   }

   public void setBuyingAmount(int amount) {
      this.buyingAmount = amount;
   }

   public List<VillagerTrades.ItemListing> getRareTrades() {
      return this.rare;
   }

   public int getRareAmount() {
      return this.rareAmount;
   }

   public void setRareAmount(int amount) {
      this.rareAmount = amount;
   }

   public List<VillagerTrades.ItemListing> getGenericTrades() {
      return this.generic;
   }

   public int getGenericAmount() {
      return this.genericAmount;
   }

   public void setGenericAmount(int amount) {
      this.genericAmount = amount;
   }

   public void addTrades(List<VillagerTrades.ItemListing> trades, int amount) {
      this.additionalTrades.add(Pair.of(trades, amount));
   }

   public HolderLookup.Provider getRegistries() {
      return this.registries;
   }
}
