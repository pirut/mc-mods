package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class LevelDebugSynchronizers {
   private final ServerLevel level;
   private final List<TrackingDebugSynchronizer<?>> allSynchronizers = new ArrayList<>();
   private final Map<DebugSubscription<?>, TrackingDebugSynchronizer.SourceSynchronizer<?>> sourceSynchronizers = new HashMap<>();
   private final TrackingDebugSynchronizer.PoiSynchronizer poiSynchronizer = new TrackingDebugSynchronizer.PoiSynchronizer();
   private final TrackingDebugSynchronizer.VillageSectionSynchronizer villageSectionSynchronizer = new TrackingDebugSynchronizer.VillageSectionSynchronizer();
   private boolean sleeping = true;
   private Set<DebugSubscription<?>> enabledSubscriptions = Set.of();

   public LevelDebugSynchronizers(ServerLevel p_449569_) {
      this.level = p_449569_;

      for (DebugSubscription<?> debugsubscription : BuiltInRegistries.DEBUG_SUBSCRIPTION) {
         if (debugsubscription.valueStreamCodec() != null) {
            this.sourceSynchronizers.put(debugsubscription, new TrackingDebugSynchronizer.SourceSynchronizer<>(debugsubscription));
         }
      }

      this.allSynchronizers.addAll(this.sourceSynchronizers.values());
      this.allSynchronizers.add(this.poiSynchronizer);
      this.allSynchronizers.add(this.villageSectionSynchronizer);
   }

   public void tick(ServerDebugSubscribers p_449142_) {
      this.enabledSubscriptions = p_449142_.enabledSubscriptions();
      boolean flag = this.enabledSubscriptions.isEmpty();
      if (this.sleeping != flag) {
         this.sleeping = flag;
         if (flag) {
            for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
               trackingdebugsynchronizer.clear();
            }
         } else {
            this.wakeUp();
         }
      }

      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer1 : this.allSynchronizers) {
            trackingdebugsynchronizer1.tick(this.level);
         }
      }
   }

   private void wakeUp() {
      ChunkMap chunkmap = this.level.getChunkSource().chunkMap;
      chunkmap.forEachReadyToSendChunk(this::registerChunk);

      for (Entity entity : this.level.getAllEntities()) {
         if (chunkmap.isTrackedByAnyPlayer(entity)) {
            this.registerEntity(entity);
         }
      }
   }

   <T> TrackingDebugSynchronizer.SourceSynchronizer<T> getSourceSynchronizer(DebugSubscription<T> p_449320_) {
      return (TrackingDebugSynchronizer.SourceSynchronizer<T>)this.sourceSynchronizers.get(p_449320_);
   }

   public void registerChunk(final LevelChunk p_449017_) {
      if (!this.sleeping) {
         p_449017_.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(DebugSubscription<T> p_449505_, DebugValueSource.ValueGetter<T> p_449171_) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(p_449505_).registerChunk(p_449017_.getPos(), p_449171_);
            }
         });
         p_449017_.getBlockEntities().values().forEach(this::registerBlockEntity);
      }
   }

   public void dropChunk(ChunkPos p_449540_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
            sourcesynchronizer.dropChunk(p_449540_);
         }
      }
   }

   public void registerBlockEntity(final BlockEntity p_449722_) {
      if (!this.sleeping) {
         p_449722_.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(DebugSubscription<T> p_449499_, DebugValueSource.ValueGetter<T> p_449473_) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(p_449499_).registerBlockEntity(p_449722_.getBlockPos(), p_449473_);
            }
         });
      }
   }

   public void dropBlockEntity(BlockPos p_449414_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
            sourcesynchronizer.dropBlockEntity(this.level, p_449414_);
         }
      }
   }

   public void registerEntity(final Entity p_449412_) {
      if (!this.sleeping) {
         p_449412_.registerDebugValues(this.level, new DebugValueSource.Registration() {
            @Override
            public <T> void register(DebugSubscription<T> p_449475_, DebugValueSource.ValueGetter<T> p_449045_) {
               LevelDebugSynchronizers.this.getSourceSynchronizer(p_449475_).registerEntity(p_449412_.getUUID(), p_449045_);
            }
         });
      }
   }

   public void dropEntity(Entity p_449702_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer.SourceSynchronizer<?> sourcesynchronizer : this.sourceSynchronizers.values()) {
            sourcesynchronizer.dropEntity(p_449702_);
         }
      }
   }

   public void startTrackingChunk(ServerPlayer p_449615_, ChunkPos p_449873_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
            trackingdebugsynchronizer.startTrackingChunk(p_449615_, p_449873_);
         }
      }
   }

   public void startTrackingEntity(ServerPlayer p_449194_, Entity p_449598_) {
      if (!this.sleeping) {
         for (TrackingDebugSynchronizer<?> trackingdebugsynchronizer : this.allSynchronizers) {
            trackingdebugsynchronizer.startTrackingEntity(p_449194_, p_449598_);
         }
      }
   }

   public void registerPoi(PoiRecord p_449027_) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiAdded(this.level, p_449027_);
         this.villageSectionSynchronizer.onPoiAdded(this.level, p_449027_);
      }
   }

   public void updatePoi(BlockPos p_449564_) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiTicketCountChanged(this.level, p_449564_);
      }
   }

   public void dropPoi(BlockPos p_449210_) {
      if (!this.sleeping) {
         this.poiSynchronizer.onPoiRemoved(this.level, p_449210_);
         this.villageSectionSynchronizer.onPoiRemoved(this.level, p_449210_);
      }
   }

   public boolean hasAnySubscriberFor(DebugSubscription<?> p_449162_) {
      return this.enabledSubscriptions.contains(p_449162_);
   }

   public <T> void sendBlockValue(BlockPos p_449492_, DebugSubscription<T> p_449217_, T p_449278_) {
      if (this.hasAnySubscriberFor(p_449217_)) {
         this.broadcastToTracking(new ChunkPos(p_449492_), p_449217_, new ClientboundDebugBlockValuePacket(p_449492_, p_449217_.packUpdate(p_449278_)));
      }
   }

   public <T> void clearBlockValue(BlockPos p_449227_, DebugSubscription<T> p_449599_) {
      if (this.hasAnySubscriberFor(p_449599_)) {
         this.broadcastToTracking(new ChunkPos(p_449227_), p_449599_, new ClientboundDebugBlockValuePacket(p_449227_, p_449599_.emptyUpdate()));
      }
   }

   public <T> void sendEntityValue(Entity p_449144_, DebugSubscription<T> p_449584_, T p_449087_) {
      if (this.hasAnySubscriberFor(p_449584_)) {
         this.broadcastToTracking(p_449144_, p_449584_, new ClientboundDebugEntityValuePacket(p_449144_.getId(), p_449584_.packUpdate(p_449087_)));
      }
   }

   public <T> void clearEntityValue(Entity p_449788_, DebugSubscription<T> p_449244_) {
      if (this.hasAnySubscriberFor(p_449244_)) {
         this.broadcastToTracking(p_449788_, p_449244_, new ClientboundDebugEntityValuePacket(p_449788_.getId(), p_449244_.emptyUpdate()));
      }
   }

   public <T> void broadcastEventToTracking(BlockPos p_449458_, DebugSubscription<T> p_449594_, T p_449123_) {
      if (this.hasAnySubscriberFor(p_449594_)) {
         this.broadcastToTracking(new ChunkPos(p_449458_), p_449594_, new ClientboundDebugEventPacket(p_449594_.packEvent(p_449123_)));
      }
   }

   private void broadcastToTracking(ChunkPos p_449496_, DebugSubscription<?> p_449325_, Packet<? super ClientGamePacketListener> p_449819_) {
      ChunkMap chunkmap = this.level.getChunkSource().chunkMap;

      for (ServerPlayer serverplayer : chunkmap.getPlayers(p_449496_, false)) {
         if (serverplayer.debugSubscriptions().contains(p_449325_)) {
            serverplayer.connection.send(p_449819_);
         }
      }
   }

   private void broadcastToTracking(Entity p_449851_, DebugSubscription<?> p_449041_, Packet<? super ClientGamePacketListener> p_449140_) {
      ChunkMap chunkmap = this.level.getChunkSource().chunkMap;
      chunkmap.sendToTrackingPlayersFiltered(p_449851_, p_449140_, p_449879_ -> p_449879_.debugSubscriptions().contains(p_449041_));
   }
}
