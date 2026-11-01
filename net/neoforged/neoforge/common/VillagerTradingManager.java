package net.neoforged.neoforge.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

public class VillagerTradingManager {
   private static final Map<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> VANILLA_TRADES = new HashMap<>();
   private static final Int2ObjectMap<VillagerTrades.ItemListing[]> WANDERER_TRADES = new Int2ObjectOpenHashMap();

   static void loadTrades(TagsUpdatedEvent e) {
      if (e.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
         postWandererEvent(e.getRegistryAccess());
         postVillagerEvents(e.getRegistryAccess());
      }
   }

   private static void postWandererEvent(RegistryAccess registryAccess) {
      List<VillagerTrades.ItemListing> generic = NonNullList.create();
      List<VillagerTrades.ItemListing> rare = NonNullList.create();
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(1)).forEach(generic::add);
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(2)).forEach(rare::add);
      NeoForge.EVENT_BUS.post(new WandererTradesEvent(generic, rare, registryAccess));
      VillagerTrades.WANDERING_TRADER_TRADES.put(1, generic.toArray(new VillagerTrades.ItemListing[0]));
      VillagerTrades.WANDERING_TRADER_TRADES.put(2, rare.toArray(new VillagerTrades.ItemListing[0]));
   }

   private static void postVillagerEvents(RegistryAccess registryAccess) {
      for (VillagerProfession prof : BuiltInRegistries.VILLAGER_PROFESSION) {
         Int2ObjectMap<VillagerTrades.ItemListing[]> trades = VANILLA_TRADES.getOrDefault(prof, new Int2ObjectOpenHashMap());
         Int2ObjectMap<List<VillagerTrades.ItemListing>> mutableTrades = new Int2ObjectOpenHashMap();

         for (int i = 1; i < 6; i++) {
            mutableTrades.put(i, NonNullList.create());
         }

         trades.int2ObjectEntrySet()
            .forEach(e -> Arrays.stream((VillagerTrades.ItemListing[])e.getValue()).forEach(((List)mutableTrades.get(e.getIntKey()))::add));
         NeoForge.EVENT_BUS.post(new VillagerTradesEvent(mutableTrades, prof, registryAccess));
         Int2ObjectMap<VillagerTrades.ItemListing[]> newTrades = new Int2ObjectOpenHashMap();
         mutableTrades.int2ObjectEntrySet().forEach(e -> newTrades.put(e.getIntKey(), ((List)e.getValue()).toArray(new VillagerTrades.ItemListing[0])));
         VillagerTrades.TRADES.put(prof, newTrades);
      }
   }

   static {
      VillagerTrades.TRADES
         .entrySet()
         .forEach(
            e -> {
               Int2ObjectMap<VillagerTrades.ItemListing[]> copy = new Int2ObjectOpenHashMap();
               e.getValue()
                  .int2ObjectEntrySet()
                  .forEach(
                     ent -> copy.put(
                        ent.getIntKey(),
                        (VillagerTrades.ItemListing[])Arrays.copyOf(
                           (VillagerTrades.ItemListing[])ent.getValue(), ((VillagerTrades.ItemListing[])ent.getValue()).length
                        )
                     )
                  );
               VANILLA_TRADES.put(e.getKey(), copy);
            }
         );
      VillagerTrades.WANDERING_TRADER_TRADES
         .int2ObjectEntrySet()
         .forEach(
            e -> WANDERER_TRADES.put(
               e.getIntKey(),
               (VillagerTrades.ItemListing[])Arrays.copyOf((VillagerTrades.ItemListing[])e.getValue(), ((VillagerTrades.ItemListing[])e.getValue()).length)
            )
         );
   }
}
