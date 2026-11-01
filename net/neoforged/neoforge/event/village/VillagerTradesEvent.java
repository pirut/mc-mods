package net.neoforged.neoforge.event.village;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class VillagerTradesEvent extends Event {
   protected Int2ObjectMap<List<VillagerTrades.ItemListing>> trades;
   protected ResourceKey<VillagerProfession> type;
   private final HolderLookup.Provider registries;

   @Internal
   public VillagerTradesEvent(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, ResourceKey<VillagerProfession> type, HolderLookup.Provider registries) {
      this.trades = trades;
      this.type = type;
      this.registries = registries;
   }

   public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() {
      return this.trades;
   }

   public ResourceKey<VillagerProfession> getType() {
      return this.type;
   }

   public HolderLookup.Provider getRegistries() {
      return this.registries;
   }
}
