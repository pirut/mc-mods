package net.minecraft.util.debug;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public abstract class TrackingDebugSynchronizer<T> {
   protected final DebugSubscription<T> subscription;
   private final Set<UUID> subscribedPlayers = new ObjectOpenHashSet();

   public TrackingDebugSynchronizer(DebugSubscription<T> p_449689_) {
      this.subscription = p_449689_;
   }

   public final void tick(ServerLevel p_449973_) {
      for (ServerPlayer serverplayer : p_449973_.players()) {
         boolean flag = this.subscribedPlayers.contains(serverplayer.getUUID());
         boolean flag1 = serverplayer.debugSubscriptions().contains(this.subscription);
         if (flag1 != flag) {
            if (flag1) {
               this.addSubscriber(serverplayer);
            } else {
               this.subscribedPlayers.remove(serverplayer.getUUID());
            }
         }
      }

      this.subscribedPlayers.removeIf(p_449288_ -> p_449973_.getPlayerByUUID(p_449288_) == null);
      if (!this.subscribedPlayers.isEmpty()) {
         this.pollAndSendUpdates(p_449973_);
      }
   }

   private void addSubscriber(ServerPlayer p_449459_) {
      this.subscribedPlayers.add(p_449459_.getUUID());
      p_449459_.getChunkTrackingView().forEach(p_449536_ -> {
         if (!p_449459_.connection.chunkSender.isPending(p_449536_.toLong())) {
            this.startTrackingChunk(p_449459_, p_449536_);
         }
      });
      p_449459_.level().getChunkSource().chunkMap.forEachEntityTrackedBy(p_449459_, p_449691_ -> this.startTrackingEntity(p_449459_, p_449691_));
   }

   protected final void sendToPlayersTrackingChunk(ServerLevel p_449024_, ChunkPos p_449913_, Packet<? super ClientGamePacketListener> p_449621_) {
      ChunkMap chunkmap = p_449024_.getChunkSource().chunkMap;

      for (UUID uuid : this.subscribedPlayers) {
         if (p_449024_.getPlayerByUUID(uuid) instanceof ServerPlayer serverplayer && chunkmap.isChunkTracked(serverplayer, p_449913_.x, p_449913_.z)) {
            serverplayer.connection.send(p_449621_);
         }
      }
   }

   protected final void sendToPlayersTrackingEntity(ServerLevel p_449987_, Entity p_449447_, Packet<? super ClientGamePacketListener> p_449419_) {
      ChunkMap chunkmap = p_449987_.getChunkSource().chunkMap;
      chunkmap.sendToTrackingPlayersFiltered(p_449447_, p_449419_, p_449476_ -> this.subscribedPlayers.contains(p_449476_.getUUID()));
   }

   public final void startTrackingChunk(ServerPlayer p_449936_, ChunkPos p_449628_) {
      if (this.subscribedPlayers.contains(p_449936_.getUUID())) {
         this.sendInitialChunk(p_449936_, p_449628_);
      }
   }

   public final void startTrackingEntity(ServerPlayer p_449603_, Entity p_449850_) {
      if (this.subscribedPlayers.contains(p_449603_.getUUID())) {
         this.sendInitialEntity(p_449603_, p_449850_);
      }
   }

   protected void clear() {
   }

   protected void pollAndSendUpdates(ServerLevel p_449682_) {
   }

   protected void sendInitialChunk(ServerPlayer p_449877_, ChunkPos p_449648_) {
   }

   protected void sendInitialEntity(ServerPlayer p_449557_, Entity p_449601_) {
   }

   public static class PoiSynchronizer extends TrackingDebugSynchronizer<DebugPoiInfo> {
      public PoiSynchronizer() {
         super(DebugSubscriptions.POIS);
      }

      @Override
      protected void sendInitialChunk(ServerPlayer p_449976_, ChunkPos p_449207_) {
         ServerLevel serverlevel = p_449976_.level();
         PoiManager poimanager = serverlevel.getPoiManager();
         poimanager.getInChunk(p_449938_ -> true, p_449207_, PoiManager.Occupancy.ANY)
            .forEach(
               p_449585_ -> p_449976_.connection
                  .send(new ClientboundDebugBlockValuePacket(p_449585_.getPos(), this.subscription.packUpdate(new DebugPoiInfo(p_449585_))))
            );
      }

      public void onPoiAdded(ServerLevel p_449937_, PoiRecord p_449650_) {
         this.sendToPlayersTrackingChunk(
            p_449937_,
            new ChunkPos(p_449650_.getPos()),
            new ClientboundDebugBlockValuePacket(p_449650_.getPos(), this.subscription.packUpdate(new DebugPoiInfo(p_449650_)))
         );
      }

      public void onPoiRemoved(ServerLevel p_449666_, BlockPos p_449882_) {
         this.sendToPlayersTrackingChunk(p_449666_, new ChunkPos(p_449882_), new ClientboundDebugBlockValuePacket(p_449882_, this.subscription.emptyUpdate()));
      }

      public void onPoiTicketCountChanged(ServerLevel p_449510_, BlockPos p_449081_) {
         this.sendToPlayersTrackingChunk(
            p_449510_,
            new ChunkPos(p_449081_),
            new ClientboundDebugBlockValuePacket(p_449081_, this.subscription.packUpdate(p_449510_.getPoiManager().getDebugPoiInfo(p_449081_)))
         );
      }
   }

   public static class SourceSynchronizer<T> extends TrackingDebugSynchronizer<T> {
      private final Map<ChunkPos, TrackingDebugSynchronizer.ValueSource<T>> chunkSources = new HashMap<>();
      private final Map<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> blockEntitySources = new HashMap<>();
      private final Map<UUID, TrackingDebugSynchronizer.ValueSource<T>> entitySources = new HashMap<>();

      public SourceSynchronizer(DebugSubscription<T> p_449462_) {
         super(p_449462_);
      }

      @Override
      protected void clear() {
         this.chunkSources.clear();
         this.blockEntitySources.clear();
         this.entitySources.clear();
      }

      @Override
      protected void pollAndSendUpdates(ServerLevel p_449084_) {
         for (Entry<ChunkPos, TrackingDebugSynchronizer.ValueSource<T>> entry : this.chunkSources.entrySet()) {
            DebugSubscription.Update<T> update = entry.getValue().pollUpdate(this.subscription);
            if (update != null) {
               ChunkPos chunkpos = entry.getKey();
               this.sendToPlayersTrackingChunk(p_449084_, chunkpos, new ClientboundDebugChunkValuePacket(chunkpos, update));
            }
         }

         for (Entry<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> entry1 : this.blockEntitySources.entrySet()) {
            DebugSubscription.Update<T> update1 = entry1.getValue().pollUpdate(this.subscription);
            if (update1 != null) {
               BlockPos blockpos = entry1.getKey();
               ChunkPos chunkpos1 = new ChunkPos(blockpos);
               this.sendToPlayersTrackingChunk(p_449084_, chunkpos1, new ClientboundDebugBlockValuePacket(blockpos, update1));
            }
         }

         for (Entry<UUID, TrackingDebugSynchronizer.ValueSource<T>> entry2 : this.entitySources.entrySet()) {
            DebugSubscription.Update<T> update2 = entry2.getValue().pollUpdate(this.subscription);
            if (update2 != null) {
               Entity entity = Objects.requireNonNull(p_449084_.getEntity(entry2.getKey()));
               this.sendToPlayersTrackingEntity(p_449084_, entity, new ClientboundDebugEntityValuePacket(entity.getId(), update2));
            }
         }
      }

      public void registerChunk(ChunkPos p_449467_, DebugValueSource.ValueGetter<T> p_449716_) {
         this.chunkSources.put(p_449467_, new TrackingDebugSynchronizer.ValueSource<>(p_449716_));
      }

      public void registerBlockEntity(BlockPos p_449119_, DebugValueSource.ValueGetter<T> p_449353_) {
         this.blockEntitySources.put(p_449119_, new TrackingDebugSynchronizer.ValueSource<>(p_449353_));
      }

      public void registerEntity(UUID p_449635_, DebugValueSource.ValueGetter<T> p_449308_) {
         this.entitySources.put(p_449635_, new TrackingDebugSynchronizer.ValueSource<>(p_449308_));
      }

      public void dropChunk(ChunkPos p_449471_) {
         this.chunkSources.remove(p_449471_);
         this.blockEntitySources.keySet().removeIf(p_449471_::contains);
      }

      public void dropBlockEntity(ServerLevel p_449369_, BlockPos p_449990_) {
         TrackingDebugSynchronizer.ValueSource<T> valuesource = this.blockEntitySources.remove(p_449990_);
         if (valuesource != null) {
            ChunkPos chunkpos = new ChunkPos(p_449990_);
            this.sendToPlayersTrackingChunk(p_449369_, chunkpos, new ClientboundDebugBlockValuePacket(p_449990_, this.subscription.emptyUpdate()));
         }
      }

      public void dropEntity(Entity p_449023_) {
         this.entitySources.remove(p_449023_.getUUID());
      }

      @Override
      protected void sendInitialChunk(ServerPlayer p_449347_, ChunkPos p_449761_) {
         TrackingDebugSynchronizer.ValueSource<T> valuesource = this.chunkSources.get(p_449761_);
         if (valuesource != null && valuesource.lastSyncedValue != null) {
            p_449347_.connection.send(new ClientboundDebugChunkValuePacket(p_449761_, this.subscription.packUpdate(valuesource.lastSyncedValue)));
         }

         for (Entry<BlockPos, TrackingDebugSynchronizer.ValueSource<T>> entry : this.blockEntitySources.entrySet()) {
            T t = entry.getValue().lastSyncedValue;
            if (t != null) {
               BlockPos blockpos = entry.getKey();
               if (p_449761_.contains(blockpos)) {
                  p_449347_.connection.send(new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(t)));
               }
            }
         }
      }

      @Override
      protected void sendInitialEntity(ServerPlayer p_449080_, Entity p_449559_) {
         TrackingDebugSynchronizer.ValueSource<T> valuesource = this.entitySources.get(p_449559_.getUUID());
         if (valuesource != null && valuesource.lastSyncedValue != null) {
            p_449080_.connection.send(new ClientboundDebugEntityValuePacket(p_449559_.getId(), this.subscription.packUpdate(valuesource.lastSyncedValue)));
         }
      }
   }

   static class ValueSource<T> {
      private final DebugValueSource.ValueGetter<T> getter;
      @Nullable
      T lastSyncedValue;

      ValueSource(DebugValueSource.ValueGetter<T> p_449431_) {
         this.getter = p_449431_;
      }

      @Nullable
      public DebugSubscription.Update<T> pollUpdate(DebugSubscription<T> p_449641_) {
         T t = this.getter.get();
         if (!Objects.equals(t, this.lastSyncedValue)) {
            this.lastSyncedValue = t;
            return p_449641_.packUpdate(t);
         } else {
            return null;
         }
      }
   }

   public static class VillageSectionSynchronizer extends TrackingDebugSynchronizer<Unit> {
      public VillageSectionSynchronizer() {
         super(DebugSubscriptions.VILLAGE_SECTIONS);
      }

      @Override
      protected void sendInitialChunk(ServerPlayer p_449880_, ChunkPos p_449583_) {
         ServerLevel serverlevel = p_449880_.level();
         PoiManager poimanager = serverlevel.getPoiManager();
         poimanager.getInChunk(p_449632_ -> true, p_449583_, PoiManager.Occupancy.ANY).forEach(p_449398_ -> {
            SectionPos sectionpos = SectionPos.of(p_449398_.getPos());
            forEachVillageSectionUpdate(serverlevel, sectionpos, (p_449508_, p_449216_) -> {
               BlockPos blockpos = p_449508_.center();
               p_449880_.connection.send(new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(p_449216_ ? Unit.INSTANCE : null)));
            });
         });
      }

      public void onPoiAdded(ServerLevel p_449731_, PoiRecord p_449128_) {
         this.sendVillageSectionsPacket(p_449731_, p_449128_.getPos());
      }

      public void onPoiRemoved(ServerLevel p_449174_, BlockPos p_449094_) {
         this.sendVillageSectionsPacket(p_449174_, p_449094_);
      }

      private void sendVillageSectionsPacket(ServerLevel p_449104_, BlockPos p_449054_) {
         forEachVillageSectionUpdate(
            p_449104_,
            SectionPos.of(p_449054_),
            (p_449805_, p_449681_) -> {
               BlockPos blockpos = p_449805_.center();
               if (p_449681_) {
                  this.sendToPlayersTrackingChunk(
                     p_449104_, new ChunkPos(blockpos), new ClientboundDebugBlockValuePacket(blockpos, this.subscription.packUpdate(Unit.INSTANCE))
                  );
               } else {
                  this.sendToPlayersTrackingChunk(
                     p_449104_, new ChunkPos(blockpos), new ClientboundDebugBlockValuePacket(blockpos, this.subscription.emptyUpdate())
                  );
               }
            }
         );
      }

      private static void forEachVillageSectionUpdate(ServerLevel p_449177_, SectionPos p_449884_, BiConsumer<SectionPos, Boolean> p_449799_) {
         for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
               for (int k = -1; k <= 1; k++) {
                  SectionPos sectionpos = p_449884_.offset(j, k, i);
                  if (p_449177_.isVillage(sectionpos.center())) {
                     p_449799_.accept(sectionpos, true);
                  } else {
                     p_449799_.accept(sectionpos, false);
                  }
               }
            }
         }
      }
   }
}
