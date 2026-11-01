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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ClientDebugSubscriber {
   private final ClientPacketListener connection;
   private final DebugScreenOverlay debugScreenOverlay;
   private Set<DebugSubscription<?>> remoteSubscriptions = Set.of();
   private final Map<DebugSubscription<?>, ClientDebugSubscriber.ValueMaps<?>> valuesBySubscription = new HashMap<>();

   public ClientDebugSubscriber(ClientPacketListener connection, DebugScreenOverlay debugScreenOverlay) {
      this.debugScreenOverlay = debugScreenOverlay;
      this.connection = connection;
   }

   private static void addFlag(Set<DebugSubscription<?>> subscriptions, DebugSubscription<?> subscription, boolean enabled) {
      if (enabled) {
         subscriptions.add(subscription);
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

      return set;
   }

   public void clear() {
      this.remoteSubscriptions = Set.of();
      this.dropLevel();
   }

   public void tick(long gameTime) {
      Set<DebugSubscription<?>> set = this.requestedSubscriptions();
      if (!set.equals(this.remoteSubscriptions)) {
         this.remoteSubscriptions = set;
         this.onSubscriptionsChanged(set);
      }

      this.valuesBySubscription.forEach((debugSubscription, valueMaps) -> {
         if (debugSubscription.expireAfterTicks() != 0) {
            valueMaps.purgeExpired(gameTime);
         }
      });
   }

   private void onSubscriptionsChanged(Set<DebugSubscription<?>> subscriptions) {
      this.valuesBySubscription.keySet().retainAll(subscriptions);
      this.initializeSubscriptions(subscriptions);
      this.connection.send(new ServerboundDebugSubscriptionRequestPacket(subscriptions));
   }

   private void initializeSubscriptions(Set<DebugSubscription<?>> subscriptions) {
      for (DebugSubscription<?> debugSubscription : subscriptions) {
         this.valuesBySubscription.computeIfAbsent(debugSubscription, debugSubscriptionx -> new ClientDebugSubscriber.ValueMaps());
      }
   }

   @Nullable
   <V> ClientDebugSubscriber.ValueMaps<V> getValueMaps(DebugSubscription<V> subscription) {
      return (ClientDebugSubscriber.ValueMaps<V>)this.valuesBySubscription.get(subscription);
   }

   @Nullable
   private <K, V> ClientDebugSubscriber.ValueMap<K, V> getValueMap(DebugSubscription<V> subscription, ClientDebugSubscriber.ValueMapType<K, V> type) {
      ClientDebugSubscriber.ValueMaps<V> valueMaps = this.getValueMaps(subscription);
      return valueMaps != null ? type.get(valueMaps) : null;
   }

   @Nullable
   <K, V> V getValue(DebugSubscription<V> subscription, K key, ClientDebugSubscriber.ValueMapType<K, V> type) {
      ClientDebugSubscriber.ValueMap<K, V> valueMap = this.getValueMap(subscription, type);
      return valueMap != null ? valueMap.getValue(key) : null;
   }

   public DebugValueAccess createDebugValueAccess(Level level) {
      return new DebugValueAccess() {
         @Override
         public <T> void forEachChunk(DebugSubscription<T> subscription, BiConsumer<ChunkPos, T> action) {
            ClientDebugSubscriber.this.forEachValue(subscription, ClientDebugSubscriber.chunks(), action);
         }

         @Nullable
         @Override
         public <T> T getChunkValue(DebugSubscription<T> subscription, ChunkPos chunkPos) {
            return ClientDebugSubscriber.this.getValue(subscription, chunkPos, ClientDebugSubscriber.chunks());
         }

         @Override
         public <T> void forEachBlock(DebugSubscription<T> subscription, BiConsumer<BlockPos, T> action) {
            ClientDebugSubscriber.this.forEachValue(subscription, ClientDebugSubscriber.blocks(), action);
         }

         @Nullable
         @Override
         public <T> T getBlockValue(DebugSubscription<T> subscription, BlockPos pos) {
            return ClientDebugSubscriber.this.getValue(subscription, pos, ClientDebugSubscriber.blocks());
         }

         @Override
         public <T> void forEachEntity(DebugSubscription<T> subscription, BiConsumer<Entity, T> action) {
            ClientDebugSubscriber.this.forEachValue(subscription, ClientDebugSubscriber.entities(), (uUID, object) -> {
               Entity entity = level.getEntity(uUID);
               if (entity != null) {
                  action.accept(entity, object);
               }
            });
         }

         @Nullable
         @Override
         public <T> T getEntityValue(DebugSubscription<T> subscription, Entity entity) {
            return ClientDebugSubscriber.this.getValue(subscription, entity.getUUID(), ClientDebugSubscriber.entities());
         }

         @Override
         public <T> void forEachEvent(DebugSubscription<T> subscription, DebugValueAccess.EventVisitor<T> action) {
            ClientDebugSubscriber.ValueMaps<T> valueMaps = ClientDebugSubscriber.this.getValueMaps(subscription);
            if (valueMaps != null) {
               long l = level.getGameTime();

               for (ClientDebugSubscriber.ValueWrapper<T> valueWrapper : valueMaps.events) {
                  int i = (int)(valueWrapper.expiresAfterTime() - l);
                  int j = subscription.expireAfterTicks();
                  action.accept(valueWrapper.value(), i, j);
               }
            }
         }
      };
   }

   public <T> void updateChunk(long gameTime, ChunkPos chunkPos, DebugSubscription.Update<T> update) {
      this.updateMap(gameTime, chunkPos, update, chunks());
   }

   public <T> void updateBlock(long gameTime, BlockPos pos, DebugSubscription.Update<T> update) {
      this.updateMap(gameTime, pos, update, blocks());
   }

   public <T> void updateEntity(long gameTime, Entity entity, DebugSubscription.Update<T> update) {
      this.updateMap(gameTime, entity.getUUID(), update, entities());
   }

   public <T> void pushEvent(long gameTime, DebugSubscription.Event<T> event) {
      ClientDebugSubscriber.ValueMaps<T> valueMaps = this.getValueMaps(event.subscription());
      if (valueMaps != null) {
         valueMaps.events.add(new ClientDebugSubscriber.ValueWrapper<>(event.value(), gameTime + event.subscription().expireAfterTicks()));
      }
   }

   private <K, V> void updateMap(long gameTime, K key, DebugSubscription.Update<V> update, ClientDebugSubscriber.ValueMapType<K, V> type) {
      ClientDebugSubscriber.ValueMap<K, V> valueMap = this.getValueMap(update.subscription(), type);
      if (valueMap != null) {
         valueMap.apply(gameTime, key, update);
      }
   }

   <K, V> void forEachValue(DebugSubscription<V> subscription, ClientDebugSubscriber.ValueMapType<K, V> type, BiConsumer<K, V> action) {
      ClientDebugSubscriber.ValueMap<K, V> valueMap = this.getValueMap(subscription, type);
      if (valueMap != null) {
         valueMap.forEach(action);
      }
   }

   public void dropLevel() {
      this.valuesBySubscription.clear();
      this.initializeSubscriptions(this.remoteSubscriptions);
   }

   public void dropChunk(ChunkPos chunkPos) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriber.ValueMaps<?> valueMaps : this.valuesBySubscription.values()) {
            valueMaps.dropChunkAndBlocks(chunkPos);
         }
      }
   }

   public void dropEntity(Entity entity) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriber.ValueMaps<?> valueMaps : this.valuesBySubscription.values()) {
            valueMaps.entityValues.removeKey(entity.getUUID());
         }
      }
   }

   static <T> ClientDebugSubscriber.ValueMapType<UUID, T> entities() {
      return valueMaps -> valueMaps.entityValues;
   }

   static <T> ClientDebugSubscriber.ValueMapType<BlockPos, T> blocks() {
      return valueMaps -> valueMaps.blockValues;
   }

   static <T> ClientDebugSubscriber.ValueMapType<ChunkPos, T> chunks() {
      return valueMaps -> valueMaps.chunkValues;
   }

   @Environment(EnvType.CLIENT)
   static class ValueMap<K, V> {
      private final Map<K, ClientDebugSubscriber.ValueWrapper<V>> values = new HashMap<>();

      public void removeValues(Predicate<ClientDebugSubscriber.ValueWrapper<V>> predicate) {
         this.values.values().removeIf(predicate);
      }

      public void removeKey(K key) {
         this.values.remove(key);
      }

      public void removeKeys(Predicate<K> predicate) {
         this.values.keySet().removeIf(predicate);
      }

      @Nullable
      public V getValue(K key) {
         ClientDebugSubscriber.ValueWrapper<V> valueWrapper = this.values.get(key);
         return valueWrapper != null ? valueWrapper.value() : null;
      }

      public void apply(long gameTime, K key, DebugSubscription.Update<V> update) {
         if (update.value().isPresent()) {
            this.values.put(key, new ClientDebugSubscriber.ValueWrapper<>(update.value().get(), gameTime + update.subscription().expireAfterTicks()));
         } else {
            this.values.remove(key);
         }
      }

      public void forEach(BiConsumer<K, V> action) {
         this.values.forEach((object, valueWrapper) -> action.accept((K)object, valueWrapper.value()));
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface ValueMapType<K, V> {
      ClientDebugSubscriber.ValueMap<K, V> get(ClientDebugSubscriber.ValueMaps<V> valueMaps);
   }

   @Environment(EnvType.CLIENT)
   static class ValueMaps<V> {
      final ClientDebugSubscriber.ValueMap<ChunkPos, V> chunkValues = new ClientDebugSubscriber.ValueMap<>();
      final ClientDebugSubscriber.ValueMap<BlockPos, V> blockValues = new ClientDebugSubscriber.ValueMap<>();
      final ClientDebugSubscriber.ValueMap<UUID, V> entityValues = new ClientDebugSubscriber.ValueMap<>();
      final List<ClientDebugSubscriber.ValueWrapper<V>> events = new ArrayList<>();

      public void purgeExpired(long gameTime) {
         Predicate<ClientDebugSubscriber.ValueWrapper<V>> predicate = valueWrapper -> valueWrapper.hasExpired(gameTime);
         this.chunkValues.removeValues(predicate);
         this.blockValues.removeValues(predicate);
         this.entityValues.removeValues(predicate);
         this.events.removeIf(predicate);
      }

      public void dropChunkAndBlocks(ChunkPos chunkPos) {
         this.chunkValues.removeKey(chunkPos);
         this.blockValues.removeKeys(chunkPos::contains);
      }
   }

   @Environment(EnvType.CLIENT)
   record ValueWrapper<T>(T value, long expiresAfterTime) {
      private static final long NO_EXPIRY = -1L;

      public boolean hasExpired(long gameTime) {
         return this.expiresAfterTime == -1L ? false : gameTime >= this.expiresAfterTime;
      }
   }
}
