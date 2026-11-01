package net.neoforged.neoforge.server.command.generation;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenerationTask {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int BATCH_SIZE = 32;
   private static final int QUEUE_THRESHOLD = 8;
   private static final int COARSE_CELL_SIZE = 4;
   private final MinecraftServer server;
   private final ServerChunkCache chunkSource;
   private final ServerLevel serverLevel;
   private final Iterator<ChunkPos> iterator;
   private final int x;
   private final int z;
   private final int radius;
   private final int totalCount;
   private final Object queueLock = new Object();
   private final AtomicInteger queuedCount = new AtomicInteger();
   private final AtomicInteger okCount = new AtomicInteger();
   private final AtomicInteger errorCount = new AtomicInteger();
   private final AtomicInteger skippedCount = new AtomicInteger();
   private volatile GenerationTask.Listener listener;
   private volatile boolean stopped;
   public static final TicketType<ChunkPos> NEOFORGE_GENERATE_FORCED = TicketType.create("neoforge_generate_forced", Comparator.comparingLong(ChunkPos::toLong));

   public GenerationTask(ServerLevel serverLevel, int x, int z, int radius) {
      this.server = serverLevel.getServer();
      this.chunkSource = serverLevel.getChunkSource();
      this.serverLevel = serverLevel;
      this.iterator = new CoarseOnionIterator(radius, 4);
      this.x = x;
      this.z = z;
      this.radius = radius;
      int diameter = radius * 2 + 1;
      this.totalCount = diameter * diameter;
   }

   public int getOkCount() {
      return this.okCount.get();
   }

   public int getErrorCount() {
      return this.errorCount.get();
   }

   public int getSkippedCount() {
      return this.skippedCount.get();
   }

   public int getTotalCount() {
      return this.totalCount;
   }

   public void run(GenerationTask.Listener listener) {
      if (this.listener != null) {
         throw new IllegalStateException("already running!");
      } else {
         this.listener = listener;
         CompletableFuture.runAsync(this::tryEnqueueTasks, Util.backgroundExecutor());
      }
   }

   public void stop() {
      synchronized (this.queueLock) {
         this.stopped = true;
         this.listener = null;
      }
   }

   private void tryEnqueueTasks() {
      synchronized (this.queueLock) {
         if (!this.stopped) {
            int enqueueCount = 32 - this.queuedCount.get();
            if (enqueueCount > 0) {
               LongList chunks = this.collectChunks(enqueueCount);
               if (chunks.isEmpty()) {
                  this.listener.complete(this.errorCount.get());
                  this.stopped = true;
               } else {
                  this.queuedCount.getAndAdd(chunks.size());
                  this.server.submit(() -> this.enqueueChunks(chunks));
               }
            }
         }
      }
   }

   private void enqueueChunks(LongList chunks) {
      for (int i = 0; i < chunks.size(); i++) {
         long chunk = chunks.getLong(i);
         this.acquireChunk(chunk);
      }

      this.chunkSource.tick(() -> false, true);
      ChunkMap chunkMap = this.chunkSource.chunkMap;

      for (int i = 0; i < chunks.size(); i++) {
         long chunkLongPos = chunks.getLong(i);
         ChunkHolder holder = chunkMap.getVisibleChunkIfPresent(chunkLongPos);
         if (holder == null) {
            LOGGER.warn("Added ticket for chunk but it was not added! ({}; {})", ChunkPos.getX(chunkLongPos), ChunkPos.getZ(chunkLongPos));
            this.acceptChunkResult(chunkLongPos, ChunkHolder.UNLOADED_CHUNK);
         } else {
            holder.scheduleChunkGenerationTask(ChunkStatus.FULL, chunkMap).whenCompleteAsync((result, throwable) -> {
               if (throwable == null) {
                  this.acceptChunkResult(chunkLongPos, (ChunkResult<ChunkAccess>)result);
               } else {
                  LOGGER.warn("Encountered unexpected error while generating chunk", throwable);
                  this.acceptChunkResult(chunkLongPos, ChunkHolder.UNLOADED_CHUNK);
               }
            }, runnable -> chunkMap.scheduleOnMainThreadMailbox(ChunkTaskPriorityQueueSorter.message(holder, runnable)));
         }
      }
   }

   private void acceptChunkResult(long chunk, ChunkResult<ChunkAccess> result) {
      this.server.submit(() -> this.releaseChunk(chunk));
      if (result.isSuccess()) {
         this.okCount.getAndIncrement();
      } else {
         this.errorCount.getAndIncrement();
      }

      this.listener.update(this.okCount.get(), this.errorCount.get(), this.skippedCount.get(), this.totalCount);
      int queuedCount = this.queuedCount.decrementAndGet();
      if (queuedCount <= 8) {
         this.tryEnqueueTasks();
      }

      if ((this.okCount.get() + this.errorCount.get()) % 1000 == 999) {
         this.server.submit(() -> this.serverLevel.save(null, false, true));
      }
   }

   private LongList collectChunks(int count) {
      LongList chunks = new LongArrayList(count);
      Iterator<ChunkPos> iterator = this.iterator;
      int i = 0;

      while (i < count && iterator.hasNext()) {
         ChunkPos chunkPosInLocalSpace = iterator.next();
         if (this.isChunkFullyGenerated(chunkPosInLocalSpace)) {
            this.skippedCount.incrementAndGet();
            this.listener.update(this.okCount.get(), this.errorCount.get(), this.skippedCount.get(), this.totalCount);
         } else {
            chunks.add(ChunkPos.asLong(chunkPosInLocalSpace.x + this.x, chunkPosInLocalSpace.z + this.z));
            i++;
         }
      }

      return chunks;
   }

   private void acquireChunk(long chunk) {
      ChunkPos pos = new ChunkPos(chunk);
      this.chunkSource.addRegionTicket(NEOFORGE_GENERATE_FORCED, pos, 0, pos);
   }

   private void releaseChunk(long chunk) {
      ChunkPos pos = new ChunkPos(chunk);
      this.chunkSource.removeRegionTicket(NEOFORGE_GENERATE_FORCED, pos, 0, pos);
   }

   private boolean isChunkFullyGenerated(ChunkPos chunkPosInLocalSpace) {
      ChunkPos chunkPosInWorldSpace = new ChunkPos(chunkPosInLocalSpace.x + this.x, chunkPosInLocalSpace.z + this.z);
      CollectFields collectFields = new CollectFields(new FieldSelector(StringTag.TYPE, "Status"));
      this.chunkSource.chunkMap.chunkScanner().scanChunk(chunkPosInWorldSpace, collectFields).join();
      return collectFields.getResult() instanceof CompoundTag compoundTag ? compoundTag.getString("Status").equals("minecraft:full") : false;
   }

   public interface Listener {
      void update(int var1, int var2, int var3, int var4);

      void complete(int var1);
   }
}
