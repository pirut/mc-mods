package net.neoforged.neoforge.common;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.apache.commons.lang3.tuple.Pair;

public class VillagerTradingManager {
   private static final Map<ResourceKey<VillagerProfession>, Int2ObjectMap<VillagerTrades.ItemListing[]>> VANILLA_TRADES = new HashMap<>();
   private static final List<Pair<VillagerTrades.ItemListing[], Integer>> WANDERER_TRADES = new ArrayList<>();

   static void loadTrades(TagsUpdatedEvent e) {
      if (e.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
         postWandererEvent(e.getLookupProvider());
         postVillagerEvents(e.getLookupProvider());
      }
   }

   private static void postWandererEvent(HolderLookup.Provider registries) {
      List<VillagerTrades.ItemListing> buying = NonNullList.create();
      List<VillagerTrades.ItemListing> rare = NonNullList.create();
      List<VillagerTrades.ItemListing> generic = NonNullList.create();
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(0).getLeft()).forEach(buying::add);
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(1).getLeft()).forEach(rare::add);
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(2).getLeft()).forEach(generic::add);
      int buyingAmount = (Integer)WANDERER_TRADES.get(0).getRight();
      int rareAmount = (Integer)WANDERER_TRADES.get(1).getRight();
      int genericAmount = (Integer)WANDERER_TRADES.get(2).getRight();
      List<Pair<List<VillagerTrades.ItemListing>, Integer>> additional = new ArrayList<>();
      WandererTradesEvent event = new WandererTradesEvent(buying, buyingAmount, rare, rareAmount, generic, genericAmount, additional, registries);
      NeoForge.EVENT_BUS.post(event);
      VillagerTrades.WANDERING_TRADER_TRADES = ImmutableList.builder()
         .add(Pair.of(buying.toArray(VillagerTrades.ItemListing[]::new), event.getBuyingAmount()))
         .add(Pair.of(rare.toArray(VillagerTrades.ItemListing[]::new), event.getRareAmount()))
         .add(Pair.of(generic.toArray(VillagerTrades.ItemListing[]::new), event.getGenericAmount()))
         .addAll(additional.stream().map(pair -> Pair.of(((List)pair.getLeft()).toArray(VillagerTrades.ItemListing[]::new), (Integer)pair.getRight())).toList())
         .build();
   }

   private static void postVillagerEvents(HolderLookup.Provider registries) {
      for (Entry<ResourceKey<VillagerProfession>, VillagerProfession> entry : BuiltInRegistries.VILLAGER_PROFESSION.entrySet()) {
         ResourceKey<VillagerProfession> prof = entry.getKey();
         Int2ObjectMap<VillagerTrades.ItemListing[]> trades = VANILLA_TRADES.getOrDefault(prof, new Int2ObjectOpenHashMap());
         Int2ObjectMap<List<VillagerTrades.ItemListing>> mutableTrades = new Int2ObjectOpenHashMap();

         for (int i = 1; i < 6; i++) {
            mutableTrades.put(i, NonNullList.create());
         }

         trades.int2ObjectEntrySet()
            .forEach(e -> Arrays.stream((VillagerTrades.ItemListing[])e.getValue()).forEach(((List)mutableTrades.get(e.getIntKey()))::add));
         NeoForge.EVENT_BUS.post(new VillagerTradesEvent(mutableTrades, prof, registries));
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
         .forEach(
            e -> WANDERER_TRADES.add(
               Pair.of(
                  (VillagerTrades.ItemListing[])Arrays.copyOf((VillagerTrades.ItemListing[])e.getLeft(), ((VillagerTrades.ItemListing[])e.getLeft()).length),
                  (Integer)e.getRight()
               )
            )
         );
   }
}
