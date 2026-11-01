package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.util.TriState;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class DistanceManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap();
   private final LoadingChunkTracker loadingChunkTracker;
   private final SimulationChunkTracker simulationChunkTracker;
   final TicketStorage ticketStorage;
   private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
   private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(32);
   protected final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet();
   final ThrottlingChunkTaskDispatcher ticketDispatcher;
   final LongSet ticketsToRelease = new LongOpenHashSet();
   final Executor mainThreadExecutor;
   private int simulationDistance = 10;

   protected DistanceManager(TicketStorage ticketStorage, Executor dispatcher, Executor mainThreadExecutor) {
      this.ticketStorage = ticketStorage;
      this.loadingChunkTracker = new LoadingChunkTracker(this, ticketStorage);
      this.simulationChunkTracker = new SimulationChunkTracker(ticketStorage);
      TaskScheduler<Runnable> taskScheduler = TaskScheduler.wrapExecutor("player ticket throttler", mainThreadExecutor);
      this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(taskScheduler, dispatcher, 4);
      this.mainThreadExecutor = mainThreadExecutor;
   }

   protected abstract boolean isChunkToRemove(long chunkPos);

   @Nullable
   protected abstract ChunkHolder getChunk(long chunkPos);

   @Nullable
   protected abstract ChunkHolder updateChunkScheduling(long chunkPos, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

   public boolean runAllUpdates(ChunkMap chunkMap) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      this.simulationChunkTracker.runAllUpdates();
      this.playerTicketManager.runAllUpdates();
      int i = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
      boolean bl = i != 0;
      if (bl && SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("DMU {}", i);
      }

      if (!this.chunksToUpdateFutures.isEmpty()) {
         for (ChunkHolder chunkHolder : this.chunksToUpdateFutures) {
            chunkHolder.updateHighestAllowedStatus(chunkMap);
         }

         for (ChunkHolder chunkHolder : this.chunksToUpdateFutures) {
            chunkHolder.updateFutures(chunkMap, this.mainThreadExecutor);
         }

         this.chunksToUpdateFutures.clear();
         return true;
      } else {
         if (!this.ticketsToRelease.isEmpty()) {
            LongIterator longIterator = this.ticketsToRelease.iterator();

            while (longIterator.hasNext()) {
               long l = longIterator.nextLong();
               if (this.ticketStorage.getTickets(l).stream().anyMatch(ticket -> ticket.getType() == TicketType.PLAYER_LOADING)) {
                  ChunkHolder chunkHolder2 = chunkMap.getUpdatingChunkIfPresent(l);
                  if (chunkHolder2 == null) {
                     throw new IllegalStateException();
                  }

                  CompletableFuture<ChunkResult<LevelChunk>> completableFuture = chunkHolder2.getEntityTickingChunkFuture();
                  completableFuture.thenAccept(chunkResult -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(l, () -> {}, false)));
               }
            }

            this.ticketsToRelease.clear();
         }

         return bl;
      }
   }

   public void addPlayer(SectionPos sectionPos, ServerPlayer player) {
      ChunkPos chunkPos = sectionPos.chunk();
      long l = chunkPos.toLong();
      ((ObjectSet)this.playersPerChunk.computeIfAbsent(l, lx -> new ObjectOpenHashSet())).add(player);
      this.naturalSpawnChunkCounter.update(l, 0, true);
      this.playerTicketManager.update(l, 0, true);
      this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkPos);
   }

   public void removePlayer(SectionPos sectionPos, ServerPlayer player) {
      ChunkPos chunkPos = sectionPos.chunk();
      long l = chunkPos.toLong();
      ObjectSet<ServerPlayer> objectSet = (ObjectSet<ServerPlayer>)this.playersPerChunk.get(l);
      objectSet.remove(player);
      if (objectSet.isEmpty()) {
         this.playersPerChunk.remove(l);
         this.naturalSpawnChunkCounter.update(l, Integer.MAX_VALUE, false);
         this.playerTicketManager.update(l, Integer.MAX_VALUE, false);
         this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkPos);
      }
   }

   private int getPlayerTicketLevel() {
      return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
   }

   public boolean inEntityTickingRange(long chunkPos) {
      return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(chunkPos));
   }

   public boolean inBlockTickingRange(long chunkPos) {
      return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(chunkPos));
   }

   public int getChunkLevel(long chunkPos, boolean simulate) {
      return simulate ? this.simulationChunkTracker.getLevel(chunkPos) : this.loadingChunkTracker.getLevel(chunkPos);
   }

   protected void updatePlayerTickets(int viewDistance) {
      this.playerTicketManager.updateViewDistance(viewDistance);
   }

   public void updateSimulationDistance(int simulationDistance) {
      if (simulationDistance != this.simulationDistance) {
         this.simulationDistance = simulationDistance;
         this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
      }
   }

   public int getNaturalSpawnChunkCount() {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.size();
   }

   public TriState hasPlayersNearby(long chunkPos) {
      this.naturalSpawnChunkCounter.runAllUpdates();
      int i = this.naturalSpawnChunkCounter.getLevel(chunkPos);
      if (i <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
         return TriState.TRUE;
      } else {
         return i > 8 ? TriState.FALSE : TriState.DEFAULT;
      }
   }

   public void forEachEntityTickingChunk(LongConsumer action) {
      ObjectIterator var2 = Long2ByteMaps.fastIterable(this.simulationChunkTracker.chunks).iterator();

      while (var2.hasNext()) {
         Entry entry = (Entry)var2.next();
         byte b = entry.getByteValue();
         long l = entry.getLongKey();
         if (ChunkLevel.isEntityTicking(b)) {
            action.accept(l);
         }
      }
   }

   public LongIterator getSpawnCandidateChunks() {
      this.naturalSpawnChunkCounter.runAllUpdates();
      return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
   }

   public String getDebugStatus() {
      return this.ticketDispatcher.getDebugStatus();
   }

   public boolean hasTickets() {
      return this.ticketStorage.hasTickets();
   }

   class FixedPlayerDistanceChunkTracker extends ChunkTracker {
      protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
      protected final int maxDistance;

      protected FixedPlayerDistanceChunkTracker(final int maxDistance) {
         super(maxDistance + 2, 16, 256);
         this.maxDistance = maxDistance;
         this.chunks.defaultReturnValue((byte)(maxDistance + 2));
      }

      @Override
      protected int getLevel(long chunkPos) {
         return this.chunks.get(chunkPos);
      }

      @Override
      protected void setLevel(long chunkPos, int level) {
         byte b;
         if (level > this.maxDistance) {
            b = this.chunks.remove(chunkPos);
         } else {
            b = this.chunks.put(chunkPos, (byte)level);
         }

         this.onLevelChange(chunkPos, b, level);
      }

      protected void onLevelChange(long chunkPos, int oldLevel, int newLevel) {
      }

      @Override
      protected int getLevelFromSource(long pos) {
         return this.havePlayer(pos) ? 0 : Integer.MAX_VALUE;
      }

      private boolean havePlayer(long chunkPos) {
         ObjectSet<ServerPlayer> objectSet = (ObjectSet<ServerPlayer>)DistanceManager.this.playersPerChunk.get(chunkPos);
         return objectSet != null && !objectSet.isEmpty();
      }

      public void runAllUpdates() {
         this.runUpdates(Integer.MAX_VALUE);
      }
   }

   class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
      private int viewDistance;
      private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
      private final LongSet toUpdate = new LongOpenHashSet();

      protected PlayerTicketTracker(final int i) {
         super(i);
         this.viewDistance = 0;
         this.queueLevels.defaultReturnValue(i + 2);
      }

      @Override
      protected void onLevelChange(long chunkPos, int oldLevel, int newLevel) {
         this.toUpdate.add(chunkPos);
      }

      public void updateViewDistance(int viewDistance) {
         ObjectIterator var2 = this.chunks.long2ByteEntrySet().iterator();

         while (var2.hasNext()) {
            Entry entry = (Entry)var2.next();
            byte b = entry.getByteValue();
            long l = entry.getLongKey();
            this.onLevelChange(l, b, this.haveTicketFor(b), b <= viewDistance);
         }

         this.viewDistance = viewDistance;
      }

      private void onLevelChange(long chunkPos, int level, boolean hadTicket, boolean hasTicket) {
         if (hadTicket != hasTicket) {
            Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, DistanceManager.PLAYER_TICKET_LEVEL);
            if (hasTicket) {
               DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                  if (this.haveTicketFor(this.getLevel(chunkPos))) {
                     DistanceManager.this.ticketStorage.addTicket(chunkPos, ticket);
                     DistanceManager.this.ticketsToRelease.add(chunkPos);
                  } else {
                     DistanceManager.this.ticketDispatcher.release(chunkPos, () -> {}, false);
                  }
               }), chunkPos, () -> level);
            } else {
               DistanceManager.this.ticketDispatcher
                  .release(
                     chunkPos,
                     () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.ticketStorage.removeTicket(chunkPos, ticket)),
                     true
                  );
            }
         }
      }

      @Override
      public void runAllUpdates() {
         super.runAllUpdates();
         if (!this.toUpdate.isEmpty()) {
            LongIterator longIterator = this.toUpdate.iterator();

            while (longIterator.hasNext()) {
               long l = longIterator.nextLong();
               int i = this.queueLevels.get(l);
               int j = this.getLevel(l);
               if (i != j) {
                  DistanceManager.this.ticketDispatcher.onLevelChange(new ChunkPos(l), () -> this.queueLevels.get(l), j, ix -> {
                     if (ix >= this.queueLevels.defaultReturnValue()) {
                        this.queueLevels.remove(l);
                     } else {
                        this.queueLevels.put(l, ix);
                     }
                  });
                  this.onLevelChange(l, j, this.haveTicketFor(i), this.haveTicketFor(j));
               }
            }

            this.toUpdate.clear();
         }
      }

      private boolean haveTicketFor(int level) {
         return level <= this.viewDistance;
      }
   }
}
