package net.minecraft.world.entity.ai.gossip;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;

public class GossipContainer {
   public static final Codec<GossipContainer> CODEC = GossipContainer.GossipEntry.CODEC
      .listOf()
      .xmap(GossipContainer::new, gossipContainer -> gossipContainer.unpack().toList());
   public static final int DISCARD_THRESHOLD = 2;
   private final Map<UUID, GossipContainer.EntityGossips> gossips = new HashMap<>();

   public GossipContainer() {
   }

   private GossipContainer(List<GossipContainer.GossipEntry> entries) {
      entries.forEach(gossipEntry -> this.getOrCreate(gossipEntry.target).entries.put(gossipEntry.type, gossipEntry.value));
   }

   @VisibleForDebug
   public Map<UUID, Object2IntMap<GossipType>> getGossipEntries() {
      Map<UUID, Object2IntMap<GossipType>> map = Maps.newHashMap();
      this.gossips.keySet().forEach(uUID -> {
         GossipContainer.EntityGossips entityGossips = this.gossips.get(uUID);
         map.put(uUID, entityGossips.entries);
      });
      return map;
   }

   public void decay() {
      Iterator<GossipContainer.EntityGossips> iterator = this.gossips.values().iterator();

      while (iterator.hasNext()) {
         GossipContainer.EntityGossips entityGossips = iterator.next();
         entityGossips.decay();
         if (entityGossips.isEmpty()) {
            iterator.remove();
         }
      }
   }

   private Stream<GossipContainer.GossipEntry> unpack() {
      return this.gossips.entrySet().stream().flatMap(entry -> entry.getValue().unpack(entry.getKey()));
   }

   private Collection<GossipContainer.GossipEntry> selectGossipsForTransfer(RandomSource random, int amount) {
      List<GossipContainer.GossipEntry> list = this.unpack().toList();
      if (list.isEmpty()) {
         return Collections.emptyList();
      } else {
         int[] is = new int[list.size()];
         int i = 0;

         for (int j = 0; j < list.size(); j++) {
            GossipContainer.GossipEntry gossipEntry = list.get(j);
            i += Math.abs(gossipEntry.weightedValue());
            is[j] = i - 1;
         }

         Set<GossipContainer.GossipEntry> set = Sets.newIdentityHashSet();

         for (int k = 0; k < amount; k++) {
            int l = random.nextInt(i);
            int m = Arrays.binarySearch(is, l);
            set.add(list.get(m < 0 ? -m - 1 : m));
         }

         return set;
      }
   }

   private GossipContainer.EntityGossips getOrCreate(UUID identifier) {
      return this.gossips.computeIfAbsent(identifier, uUID -> new GossipContainer.EntityGossips());
   }

   public void transferFrom(GossipContainer container, RandomSource randomSource, int amount) {
      Collection<GossipContainer.GossipEntry> collection = container.selectGossipsForTransfer(randomSource, amount);
      collection.forEach(gossipEntry -> {
         int i = gossipEntry.value - gossipEntry.type.decayPerTransfer;
         if (i >= 2) {
            this.getOrCreate(gossipEntry.target).entries.mergeInt(gossipEntry.type, i, GossipContainer::mergeValuesForTransfer);
         }
      });
   }

   public int getReputation(UUID identifier, Predicate<GossipType> gossip) {
      GossipContainer.EntityGossips entityGossips = this.gossips.get(identifier);
      return entityGossips != null ? entityGossips.weightedValue(gossip) : 0;
   }

   public long getCountForType(GossipType gossipType, DoublePredicate gossipPredicate) {
      return this.gossips
         .values()
         .stream()
         .filter(entityGossips -> gossipPredicate.test(entityGossips.entries.getOrDefault(gossipType, 0) * gossipType.weight))
         .count();
   }

   public void add(UUID identifier, GossipType gossipType, int gossipValue) {
      GossipContainer.EntityGossips entityGossips = this.getOrCreate(identifier);
      entityGossips.entries.mergeInt(gossipType, gossipValue, (i, j) -> this.mergeValuesForAddition(gossipType, i, j));
      entityGossips.makeSureValueIsntTooLowOrTooHigh(gossipType);
      if (entityGossips.isEmpty()) {
         this.gossips.remove(identifier);
      }
   }

   public void remove(UUID identifier, GossipType gossipType, int gossipValue) {
      this.add(identifier, gossipType, -gossipValue);
   }

   public void remove(UUID identifier, GossipType gossipType) {
      GossipContainer.EntityGossips entityGossips = this.gossips.get(identifier);
      if (entityGossips != null) {
         entityGossips.remove(gossipType);
         if (entityGossips.isEmpty()) {
            this.gossips.remove(identifier);
         }
      }
   }

   public void remove(GossipType gossipType) {
      Iterator<GossipContainer.EntityGossips> iterator = this.gossips.values().iterator();

      while (iterator.hasNext()) {
         GossipContainer.EntityGossips entityGossips = iterator.next();
         entityGossips.remove(gossipType);
         if (entityGossips.isEmpty()) {
            iterator.remove();
         }
      }
   }

   public void clear() {
      this.gossips.clear();
   }

   public void putAll(GossipContainer other) {
      other.gossips.forEach((uUID, entityGossips) -> this.getOrCreate(uUID).entries.putAll(entityGossips.entries));
   }

   private static int mergeValuesForTransfer(int value1, int value2) {
      return Math.max(value1, value2);
   }

   private int mergeValuesForAddition(GossipType gossipType, int existing, int additive) {
      int i = existing + additive;
      return i > gossipType.max ? Math.max(gossipType.max, existing) : i;
   }

   public GossipContainer copy() {
      GossipContainer gossipContainer = new GossipContainer();
      gossipContainer.putAll(this);
      return gossipContainer;
   }

   static class EntityGossips {
      final Object2IntMap<GossipType> entries = new Object2IntOpenHashMap();

      public int weightedValue(Predicate<GossipType> gossipType) {
         return this.entries
            .object2IntEntrySet()
            .stream()
            .filter(entry -> gossipType.test((GossipType)entry.getKey()))
            .mapToInt(entry -> entry.getIntValue() * ((GossipType)entry.getKey()).weight)
            .sum();
      }

      public Stream<GossipContainer.GossipEntry> unpack(UUID identifier) {
         return this.entries
            .object2IntEntrySet()
            .stream()
            .map(entry -> new GossipContainer.GossipEntry(identifier, (GossipType)entry.getKey(), entry.getIntValue()));
      }

      public void decay() {
         ObjectIterator<Entry<GossipType>> objectIterator = this.entries.object2IntEntrySet().iterator();

         while (objectIterator.hasNext()) {
            Entry<GossipType> entry = (Entry<GossipType>)objectIterator.next();
            int i = entry.getIntValue() - ((GossipType)entry.getKey()).decayPerDay;
            if (i < 2) {
               objectIterator.remove();
            } else {
               entry.setValue(i);
            }
         }
      }

      public boolean isEmpty() {
         return this.entries.isEmpty();
      }

      public void makeSureValueIsntTooLowOrTooHigh(GossipType gossipType) {
         int i = this.entries.getInt(gossipType);
         if (i > gossipType.max) {
            this.entries.put(gossipType, gossipType.max);
         }

         if (i < 2) {
            this.remove(gossipType);
         }
      }

      public void remove(GossipType gossipType) {
         this.entries.removeInt(gossipType);
      }
   }

   record GossipEntry(UUID target, GossipType type, int value) {
      public static final Codec<GossipContainer.GossipEntry> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               UUIDUtil.CODEC.fieldOf("Target").forGetter(GossipContainer.GossipEntry::target),
               GossipType.CODEC.fieldOf("Type").forGetter(GossipContainer.GossipEntry::type),
               ExtraCodecs.POSITIVE_INT.fieldOf("Value").forGetter(GossipContainer.GossipEntry::value)
            )
            .apply(instance, GossipContainer.GossipEntry::new)
      );

      public int weightedValue() {
         return this.value * this.type.weight;
      }
   }
}
