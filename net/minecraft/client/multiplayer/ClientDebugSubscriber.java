package net.minecraft.client.multiplayer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.AddDebugSubscriptionFlagsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientDebugSubscriber {
   private final ClientPacketListener connection;
   private final DebugScreenOverlay debugScreenOverlay;
   private Set<DebugSubscription<?>> remoteSubscriptions = Set.of();
   private final Map<DebugSubscription<?>, ClientDebugSubscriber.ValueMaps<?>> valuesBySubscription = new HashMap<>();

   public ClientDebugSubscriber(ClientPacketListener p_449338_, DebugScreenOverlay p_449555_) {
      this.debugScreenOverlay = p_449555_;
      this.connection = p_449338_;
   }

   private static void addFlag(Set<DebugSubscription<?>> p_449507_, DebugSubscription<?> p_449222_, boolean p_449838_) {
      if (p_449838_) {
         p_449507_.add(p_449222_);
      }
   }

   private Set<DebugSubscription<?>> requestedSubscriptions() {
      Set<DebugSubscription<?>> set = new ReferenceOpenHashSet();
      addFlag(set, RemoteDebugSampleType.TICK_TIME.subscription(), this.debugScreenOverlay.showFpsCharts());
      if (SharedConstants.DEBUG_ENABLED) {
         addFlag(set, DebugSubscriptions.BEES, SharedConstants.DEBUG_BEES);
         addFlag(set, DebugSubscriptions.BEE_HIVES, SharedConstants.DEBUG_BEES);
         addFlag(set, DebugSubscriptions.BRAINS, SharedConstants.DEBUG_BRAIN);
         addFlag(set, DebugSubscriptions.BREEZES, SharedConstants.DEBUG_BREEZE_MOB);
         addFlag(set, DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION);
         addFlag(set, DebugSubscriptions.ENTITY_PATHS, SharedConstants.DEBUG_PATHFINDING);
         addFlag(set, DebugSubscriptions.GAME_EVENTS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
         addFlag(set, DebugSubscriptions.GAME_EVENT_LISTENERS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
         addFlag(set, DebugSubscriptions.GOAL_SELECTORS, SharedConstants.DEBUG_GOAL_SELECTOR || SharedConstants.DEBUG_BEES);
         addFlag(set, DebugSubscriptions.NEIGHBOR_UPDATES, SharedConstants.DEBUG_NEIGHBORSUPDATE);
         addFlag(set, DebugSubscriptions.POIS, SharedConstants.DEBUG_POI);
         addFlag(set, DebugSubscriptions.RAIDS, SharedConstants.DEBUG_RAIDS);
         addFlag(set, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER);
         addFlag(set, DebugSubscriptions.STRUCTURES, SharedConstants.DEBUG_STRUCTURES);
         addFlag(set, DebugSubscriptions.VILLAGE_SECTIONS, SharedConstants.DEBUG_VILLAGE_SECTIONS);
      }

      NeoForge.EVENT_BUS.post(new AddDebugSubscriptionFlagsEvent((subscription, flag) -> addFlag(set, subscription, flag)));
      return set;
   }

   public void clear() {
      this.remoteSubscriptions = Set.of();
      this.dropLevel();
   }

   public void tick(long p_449322_) {
      Set<DebugSubscription<?>> set = this.requestedSubscriptions();
      if (!set.equals(this.remoteSubscriptions)) {
         this.remoteSubscriptions = set;
         this.onSubscriptionsChanged(set);
      }

      this.valuesBySubscription.forEach((p_449415_, p_449542_) -> {
         if (p_449415_.expireAfterTicks() != 0) {
            p_449542_.purgeExpired(p_449322_);
         }
      });
   }

   private void onSubscriptionsChanged(Set<DebugSubscription<?>> p_449552_) {
      this.valuesBySubscription.keySet().retainAll(p_449552_);
      this.initializeSubscriptions(p_449552_);
      this.connection.send(new ServerboundDebugSubscriptionRequestPacket(p_449552_));
   }

   private void initializeSubscriptions(Set<DebugSubscription<?>> p_481861_) {
      for (DebugSubscription<?> debugsubscription : p_481861_) {
         this.valuesBySubscription.computeIfAbsent(debugsubscription, p_449463_ -> new ClientDebugSubscriber.ValueMaps());
      }
   }

   @Nullable
   <V> ClientDebugSubscriber.ValueMaps<V> getValueMaps(DebugSubscription<V> p_449237_) {
      return (ClientDebugSubscriber.ValueMaps<V>)this.valuesBySubscription.get(p_449237_);
   }

   @Nullable
   private <K, V> ClientDebugSubscriber.ValueMap<K, V> getValueMap(DebugSubscription<V> p_449832_, ClientDebugSubscriber.ValueMapType<K, V> p_449078_) {
      ClientDebugSubscriber.ValueMaps<V> valuemaps = this.getValueMaps(p_449832_);
      return valuemaps != null ? p_449078_.get(valuemaps) : null;
   }

   @Nullable
   <K, V> V getValue(DebugSubscription<V> p_449484_, K p_449223_, ClientDebugSubscriber.ValueMapType<K, V> p_449375_) {
      ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(p_449484_, p_449375_);
      return valuemap != null ? valuemap.getValue(p_449223_) : null;
   }

   public DebugValueAccess createDebugValueAccess(final Level p_449558_) {
      return new DebugValueAccess() {
         @Override
         public <T> void forEachChunk(DebugSubscription<T> p_449630_, BiConsumer<ChunkPos, T> p_449130_) {
            ClientDebugSubscriber.this.forEachValue(p_449630_, ClientDebugSubscriber.chunks(), p_449130_);
         }

         @Nullable
         @Override
         public <T> T getChunkValue(DebugSubscription<T> p_449512_, ChunkPos p_449631_) {
            return ClientDebugSubscriber.this.getValue(p_449512_, p_449631_, ClientDebugSubscriber.chunks());
         }

         @Override
         public <T> void forEachBlock(DebugSubscription<T> p_449957_, BiConsumer<BlockPos, T> p_449504_) {
            ClientDebugSubscriber.this.forEachValue(p_449957_, ClientDebugSubscriber.blocks(), p_449504_);
         }

         @Nullable
         @Override
         public <T> T getBlockValue(DebugSubscription<T> p_449946_, BlockPos p_449548_) {
            return ClientDebugSubscriber.this.getValue(p_449946_, p_449548_, ClientDebugSubscriber.blocks());
         }

         @Override
         public <T> void forEachEntity(DebugSubscription<T> p_449134_, BiConsumer<Entity, T> p_449817_) {
            ClientDebugSubscriber.this.forEachValue(p_449134_, ClientDebugSubscriber.entities(), (p_449106_, p_449916_) -> {
               Entity entity = p_449558_.getEntity(p_449106_);
               if (entity != null) {
                  p_449817_.accept(entity, p_449916_);
               }
            });
         }

         @Nullable
         @Override
         public <T> T getEntityValue(DebugSubscription<T> p_449411_, Entity p_449503_) {
            return ClientDebugSubscriber.this.getValue(p_449411_, p_449503_.getUUID(), ClientDebugSubscriber.entities());
         }

         @Override
         public <T> void forEachEvent(DebugSubscription<T> p_449826_, DebugValueAccess.EventVisitor<T> p_449240_) {
            ClientDebugSubscriber.ValueMaps<T> valuemaps = ClientDebugSubscriber.this.getValueMaps(p_449826_);
            if (valuemaps != null) {
               long i = p_449558_.getGameTime();

               for (ClientDebugSubscriber.ValueWrapper<T> valuewrapper : valuemaps.events) {
                  int j = (int)(valuewrapper.expiresAfterTime() - i);
                  int k = p_449826_.expireAfterTicks();
                  p_449240_.accept(valuewrapper.value(), j, k);
               }
            }
         }
      };
   }

   public <T> void updateChunk(long p_449700_, ChunkPos p_449982_, DebugSubscription.Update<T> p_449061_) {
      this.updateMap(p_449700_, p_449982_, p_449061_, chunks());
   }

   public <T> void updateBlock(long p_449248_, BlockPos p_449224_, DebugSubscription.Update<T> p_449071_) {
      this.updateMap(p_449248_, p_449224_, p_449071_, blocks());
   }

   public <T> void updateEntity(long p_449611_, Entity p_449961_, DebugSubscription.Update<T> p_449450_) {
      this.updateMap(p_449611_, p_449961_.getUUID(), p_449450_, entities());
   }

   public <T> void pushEvent(long p_449389_, DebugSubscription.Event<T> p_449058_) {
      ClientDebugSubscriber.ValueMaps<T> valuemaps = this.getValueMaps(p_449058_.subscription());
      if (valuemaps != null) {
         valuemaps.events.add(new ClientDebugSubscriber.ValueWrapper<>(p_449058_.value(), p_449389_ + p_449058_.subscription().expireAfterTicks()));
      }
   }

   private <K, V> void updateMap(long p_449767_, K p_449384_, DebugSubscription.Update<V> p_449800_, ClientDebugSubscriber.ValueMapType<K, V> p_449733_) {
      ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(p_449800_.subscription(), p_449733_);
      if (valuemap != null) {
         valuemap.apply(p_449767_, p_449384_, p_449800_);
      }
   }

   <K, V> void forEachValue(DebugSubscription<V> p_449485_, ClientDebugSubscriber.ValueMapType<K, V> p_449221_, BiConsumer<K, V> p_449605_) {
      ClientDebugSubscriber.ValueMap<K, V> valuemap = this.getValueMap(p_449485_, p_449221_);
      if (valuemap != null) {
         valuemap.forEach(p_449605_);
      }
   }

   public void dropLevel() {
      this.valuesBySubscription.clear();
      this.initializeSubscriptions(this.remoteSubscriptions);
   }

   public void dropChunk(ChunkPos p_449853_) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriber.ValueMaps<?> valuemaps : this.valuesBySubscription.values()) {
            valuemaps.dropChunkAndBlocks(p_449853_);
         }
      }
   }

   public void dropEntity(Entity p_449287_) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriber.ValueMaps<?> valuemaps : this.valuesBySubscription.values()) {
            valuemaps.entityValues.removeKey(p_449287_.getUUID());
         }
      }
   }

   static <T> ClientDebugSubscriber.ValueMapType<UUID, T> entities() {
      return p_449714_ -> p_449714_.entityValues;
   }

   static <T> ClientDebugSubscriber.ValueMapType<BlockPos, T> blocks() {
      return p_449060_ -> p_449060_.blockValues;
   }

   static <T> ClientDebugSubscriber.ValueMapType<ChunkPos, T> chunks() {
      return p_449235_ -> p_449235_.chunkValues;
   }

   @OnlyIn(Dist.CLIENT)
   static class ValueMap<K, V> {
      private final Map<K, ClientDebugSubscriber.ValueWrapper<V>> values = new HashMap<>();

      public void removeValues(Predicate<ClientDebugSubscriber.ValueWrapper<V>> p_449259_) {
         this.values.values().removeIf(p_449259_);
      }

      public void removeKey(K p_449359_) {
         this.values.remove(p_449359_);
      }

      public void removeKeys(Predicate<K> p_449298_) {
         this.values.keySet().removeIf(p_449298_);
      }

      @Nullable
      public V getValue(K p_449147_) {
         ClientDebugSubscriber.ValueWrapper<V> valuewrapper = this.values.get(p_449147_);
         return valuewrapper != null ? valuewrapper.value() : null;
      }

      public void apply(long p_449252_, K p_449339_, DebugSubscription.Update<V> p_449312_) {
         if (p_449312_.value().isPresent()) {
            this.values
               .put(p_449339_, new ClientDebugSubscriber.ValueWrapper<>(p_449312_.value().get(), p_449252_ + p_449312_.subscription().expireAfterTicks()));
         } else {
            this.values.remove(p_449339_);
         }
      }

      public void forEach(BiConsumer<K, V> p_449402_) {
         this.values.forEach((p_449133_, p_449153_) -> p_449402_.accept((K)p_449133_, p_449153_.value()));
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface ValueMapType<K, V> {
      ClientDebugSubscriber.ValueMap<K, V> get(ClientDebugSubscriber.ValueMaps<V> var1);
   }

   @OnlyIn(Dist.CLIENT)
   static class ValueMaps<V> {
      final ClientDebugSubscriber.ValueMap<ChunkPos, V> chunkValues = new ClientDebugSubscriber.ValueMap<>();
      final ClientDebugSubscriber.ValueMap<BlockPos, V> blockValues = new ClientDebugSubscriber.ValueMap<>();
      final ClientDebugSubscriber.ValueMap<UUID, V> entityValues = new ClientDebugSubscriber.ValueMap<>();
      final List<ClientDebugSubscriber.ValueWrapper<V>> events = new ArrayList<>();

      public void purgeExpired(long p_449950_) {
         Predicate<ClientDebugSubscriber.ValueWrapper<V>> predicate = p_449834_ -> p_449834_.hasExpired(p_449950_);
         this.chunkValues.removeValues(predicate);
         this.blockValues.removeValues(predicate);
         this.entityValues.removeValues(predicate);
         this.events.removeIf(predicate);
      }

      public void dropChunkAndBlocks(ChunkPos p_449653_) {
         this.chunkValues.removeKey(p_449653_);
         this.blockValues.removeKeys(p_449653_::contains);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ValueWrapper<T>(T value, long expiresAfterTime) {
      private static final long NO_EXPIRY = -1L;

      public boolean hasExpired(long p_449758_) {
         return this.expiresAfterTime == -1L ? false : p_449758_ >= this.expiresAfterTime;
      }
   }
}
