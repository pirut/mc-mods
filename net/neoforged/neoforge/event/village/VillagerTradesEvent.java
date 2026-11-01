package net.neoforged.neoforge.event.village;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class VillagerTradesEvent extends Event {
   protected Int2ObjectMap<List<VillagerTrades.ItemListing>> trades;
   protected VillagerProfession type;
   private final RegistryAccess registryAccess;

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   public VillagerTradesEvent(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, VillagerProfession type) {
      this(trades, type, RegistryAccess.EMPTY);
   }

   @Internal
   public VillagerTradesEvent(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, VillagerProfession type, RegistryAccess registryAccess) {
      this.trades = trades;
      this.type = type;
      this.registryAccess = registryAccess;
   }

   public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() {
      return this.trades;
   }

   public VillagerProfession getType() {
      return this.type;
   }

   public RegistryAccess getRegistryAccess() {
      return this.registryAccess;
   }
}
