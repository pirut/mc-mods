package net.neoforged.neoforge.event.village;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class WandererTradesEvent extends Event {
   protected List<VillagerTrades.ItemListing> generic;
   protected List<VillagerTrades.ItemListing> rare;
   private final RegistryAccess registryAccess;

   @Deprecated(
      forRemoval = true,
      since = "1.21.1"
   )
   public WandererTradesEvent(List<VillagerTrades.ItemListing> generic, List<VillagerTrades.ItemListing> rare) {
      this(generic, rare, RegistryAccess.EMPTY);
   }

   @Internal
   public WandererTradesEvent(List<VillagerTrades.ItemListing> generic, List<VillagerTrades.ItemListing> rare, RegistryAccess registryAccess) {
      this.generic = generic;
      this.rare = rare;
      this.registryAccess = registryAccess;
   }

   public List<VillagerTrades.ItemListing> getGenericTrades() {
      return this.generic;
   }

   public List<VillagerTrades.ItemListing> getRareTrades() {
      return this.rare;
   }

   public RegistryAccess getRegistryAccess() {
      return this.registryAccess;
   }
}
