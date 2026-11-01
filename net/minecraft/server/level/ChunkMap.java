package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkMap extends SimpleRegionStorage implements ChunkHolder.PlayerProvider, GeneratingChunkMap {
   private static final ChunkResult<List<ChunkAccess>> UNLOADED_CHUNK_LIST_RESULT = ChunkResult.error("Unloaded chunks found in range");
   private static final CompletableFuture<ChunkResult<List<ChunkAccess>>> UNLOADED_CHUNK_LIST_FUTURE = CompletableFuture.completedFuture(
      UNLOADED_CHUNK_LIST_RESULT
   );
   private static final byte CHUNK_TYPE_REPLACEABLE = -1;
   private static final byte CHUNK_TYPE_UNKNOWN = 0;
   private static final byte CHUNK_TYPE_FULL = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHUNK_SAVED_PER_TICK = 200;
   private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
   private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
   private static final int MAX_ACTIVE_CHUNK_WRITES = 128;
   public static final int MIN_VIEW_DISTANCE = 2;
   public static final int MAX_VIEW_DISTANCE = 32;
   public static final int FORCED_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap = this.updatingChunkMap.clone();
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads = new Long2ObjectLinkedOpenHashMap();
   private final List<ChunkGenerationTask> pendingGenerationTasks = new ArrayList<>();
   final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private final RandomState randomState;
   private final ChunkGeneratorStructureState chunkGeneratorState;
   private final TicketStorage ticketStorage;
   private final PoiManager poiManager;
   final LongSet toDrop = new LongOpenHashSet();
   private boolean modified;
   private final ChunkTaskDispatcher worldgenTaskDispatcher;
   private final ChunkTaskDispatcher lightTaskDispatcher;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
   private final String storageName;
   private final PlayerMap playerMap = new PlayerMap();
   private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectOpenHashMap();
   private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
   private final Long2LongMap nextChunkSaveTime = new Long2LongOpenHashMap();
   private final LongSet chunksToEagerlySave = new LongLinkedOpenHashSet();
   private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
   private final AtomicInteger activeChunkWrites = new AtomicInteger();
   private int serverViewDistance;
   private final WorldGenContext worldGenContext;

   public ChunkMap(
      ServerLevel level,
      LevelStorageSource.LevelStorageAccess storageSource,
      DataFixer fixerUpper,
      StructureTemplateManager structureManager,
      Executor dispatcher,
      BlockableEventLoop<Runnable> mainThreadExecutor,
      LightChunkGetter lightChunk,
      ChunkGenerator generator,
      ChunkStatusUpdateListener chunkStatusListener,
      Supplier<DimensionDataStorage> overworldDataStorage,
      TicketStorage ticketStorage,
      int serverViewDistance,
      boolean sync
   ) {
      super(
         new RegionStorageInfo(storageSource.getLevelId(), level.dimension(), "chunk"),
         storageSource.getDimensionPath(level.dimension()).resolve("region"),
         fixerUpper,
         sync,
         DataFixTypes.CHUNK,
         LegacyStructureDataHandler.getLegacyTagFixer(level.dimension(), overworldDataStorage, fixerUpper)
      );
      Path path = storageSource.getDimensionPath(level.dimension());
      this.storageName = path.getFileName().toString();
      this.level = level;
      RegistryAccess registryAccess = level.registryAccess();
      long l = level.getSeed();
      if (generator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
         this.randomState = RandomState.create(noiseBasedChunkGenerator.generatorSettings().value(), registryAccess.lookupOrThrow(Registries.NOISE), l);
      } else {
         this.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), registryAccess.lookupOrThrow(Registries.NOISE), l);
      }

      this.chunkGeneratorState = generator.createState(registryAccess.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, l);
      this.mainThreadExecutor = mainThreadExecutor;
      ConsecutiveExecutor consecutiveExecutor = new ConsecutiveExecutor(dispatcher, "worldgen");
      this.chunkStatusListener = chunkStatusListener;
      ConsecutiveExecutor consecutiveExecutor2 = new ConsecutiveExecutor(dispatcher, "light");
      this.worldgenTaskDispatcher = new ChunkTaskDispatcher(consecutiveExecutor, dispatcher);
      this.lightTaskDispatcher = new ChunkTaskDispatcher(consecutiveExecutor2, dispatcher);
      this.lightEngine = new ThreadedLevelLightEngine(
         lightChunk, this, this.level.dimensionType().hasSkyLight(), consecutiveExecutor2, this.lightTaskDispatcher
      );
      this.distanceManager = new ChunkMap.DistanceManager(ticketStorage, dispatcher, mainThreadExecutor);
      this.ticketStorage = ticketStorage;
      this.poiManager = new PoiManager(
         new RegionStorageInfo(storageSource.getLevelId(), level.dimension(), "poi"),
         path.resolve("poi"),
         fixerUpper,
         sync,
         registryAccess,
         level.getServer(),
         level
      );
      this.setServerViewDistance(serverViewDistance);
      this.worldGenContext = new WorldGenContext(level, generator, structureManager, this.lightEngine, mainThreadExecutor, this::setChunkUnsaved);
   }

   private void setChunkUnsaved(ChunkPos chunkPos) {
      this.chunksToEagerlySave.add(chunkPos.toLong());
   }

   protected ChunkGenerator generator() {
      return this.worldGenContext.generator();
   }

   protected ChunkGeneratorStructureState generatorState() {
      return this.chunkGeneratorState;
   }

   protected RandomState randomState() {
      return this.randomState;
   }

   public boolean isChunkTracked(ServerPlayer player, int x, int z) {
      return player.getChunkTrackingView().contains(x, z) && !player.connection.chunkSender.isPending(ChunkPos.asLong(x, z));
   }

   private boolean isChunkOnTrackedBorder(ServerPlayer player, int x, int z) {
      if (!this.isChunkTracked(player, x, z)) {
         return false;
      } else {
         for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
               if ((i != 0 || j != 0) && !this.isChunkTracked(player, x + i, z + j)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   public ChunkHolder getUpdatingChunkIfPresent(long chunkPos) {
      return (ChunkHolder)this.updatingChunkMap.get(chunkPos);
   }

   @Nullable
   protected ChunkHolder getVisibleChunkIfPresent(long chunkPos) {
      return (ChunkHolder)this.visibleChunkMap.get(chunkPos);
   }

   @Nullable
   public ChunkStatus getLatestStatus(long chunkPos) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos);
      return chunkHolder != null ? chunkHolder.getLatestStatus() : null;
   }

   protected IntSupplier getChunkQueueLevel(long chunkPos) {
      return () -> {
         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos);
         return chunkHolder == null
            ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1
            : Math.min(chunkHolder.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(ChunkPos pos) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.toLong());
      if (chunkHolder == null) {
         return "null";
      } else {
         String string = chunkHolder.getTicketLevel() + "\n";
         ChunkStatus chunkStatus = chunkHolder.getLatestStatus();
         ChunkAccess chunkAccess = chunkHolder.getLatestChunk();
         if (chunkStatus != null) {
            string = string + "St: §" + chunkStatus.getIndex() + chunkStatus + "§r\n";
         }

         if (chunkAccess != null) {
            string = string + "Ch: §" + chunkAccess.getPersistedStatus().getIndex() + chunkAccess.getPersistedStatus() + "§r\n";
         }

         FullChunkStatus fullChunkStatus = chunkHolder.getFullStatus();
         string = string + '§' + fullChunkStatus.ordinal() + fullChunkStatus;
         return string + "§r";
      }
   }

   CompletableFuture<ChunkResult<List<ChunkAccess>>> getChunkRangeFuture(ChunkHolder chunkHolder, int range, IntFunction<ChunkStatus> statusGetter) {
      if (range == 0) {
         ChunkStatus chunkStatus = statusGetter.apply(0);
         return chunkHolder.scheduleChunkGenerationTask(chunkStatus, this).thenApply(chunkResult -> chunkResult.map(List::of));
      } else {
         int i = Mth.square(range * 2 + 1);
         List<CompletableFuture<ChunkResult<ChunkAccess>>> list = new ArrayList<>(i);
         ChunkPos chunkPos = chunkHolder.getPos();

         for (int j = -range; j <= range; j++) {
            for (int k = -range; k <= range; k++) {
               int l = Math.max(Math.abs(k), Math.abs(j));
               long m = ChunkPos.asLong(chunkPos.x + k, chunkPos.z + j);
               ChunkHolder chunkHolder2 = this.getUpdatingChunkIfPresent(m);
               if (chunkHolder2 == null) {
                  return UNLOADED_CHUNK_LIST_FUTURE;
               }

               ChunkStatus chunkStatus2 = statusGetter.apply(l);
               list.add(chunkHolder2.scheduleChunkGenerationTask(chunkStatus2, this));
            }
         }

         return Util.sequence(list).thenApply(listx -> {
            List<ChunkAccess> list2 = new ArrayList<>(listx.size());

            for (ChunkResult<ChunkAccess> chunkResult : listx) {
               if (chunkResult == null) {
                  throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
               }

               ChunkAccess chunkAccess = chunkResult.orElse(null);
               if (chunkAccess == null) {
                  return UNLOADED_CHUNK_LIST_RESULT;
               }

               list2.add(chunkAccess);
            }

            return ChunkResult.of(list2);
         });
      }
   }

   public ReportedException debugFuturesAndCreateReportedException(IllegalStateException exception, String details) {
      StringBuilder stringBuilder = new StringBuilder();
      Consumer<ChunkHolder> consumer = chunkHolder -> chunkHolder.getAllFutures()
         .forEach(
            pair -> {
               ChunkStatus chunkStatus = (ChunkStatus)pair.getFirst();
               CompletableFuture<ChunkResult<ChunkAccess>> completableFuture = (CompletableFuture<ChunkResult<ChunkAccess>>)pair.getSecond();
               if (completableFuture != null && completableFuture.isDone() && completableFuture.join() == null) {
                  stringBuilder.append(chunkHolder.getPos())
                     .append(" - status: ")
                     .append(chunkStatus)
                     .append(" future: ")
                     .append(completableFuture)
                     .append(System.lineSeparator());
               }
            }
         );
      stringBuilder.append("Updating:").append(System.lineSeparator());
      this.updatingChunkMap.values().forEach(consumer);
      stringBuilder.append("Visible:").append(System.lineSeparator());
      this.visibleChunkMap.values().forEach(consumer);
      CrashReport crashReport = CrashReport.forThrowable(exception, "Chunk loading");
      CrashReportCategory crashReportCategory = crashReport.addCategory("Chunk loading");
      crashReportCategory.setDetail("Details", details);
      crashReportCategory.setDetail("Futures", stringBuilder);
      return new ReportedException(crashReport);
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareEntityTickingChunk(ChunkHolder chunk) {
      return this.getChunkRangeFuture(chunk, 2, i -> ChunkStatus.FULL).thenApply(chunkResult -> chunkResult.map(list -> (LevelChunk)list.get(list.size() / 2)));
   }

   @Nullable
   ChunkHolder updateChunkScheduling(long chunkPos, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
      if (!ChunkLevel.isLoaded(oldLevel) && !ChunkLevel.isLoaded(newLevel)) {
         return holder;
      } else {
         if (holder != null) {
            holder.setTicketLevel(newLevel);
         }

         if (holder != null) {
            if (!ChunkLevel.isLoaded(newLevel)) {
               this.toDrop.add(chunkPos);
            } else {
               this.toDrop.remove(chunkPos);
            }
         }

         if (ChunkLevel.isLoaded(newLevel) && holder == null) {
            holder = (ChunkHolder)this.pendingUnloads.remove(chunkPos);
            if (holder != null) {
               holder.setTicketLevel(newLevel);
            } else {
               holder = new ChunkHolder(new ChunkPos(chunkPos), newLevel, this.level, this.lightEngine, this::onLevelChange, this);
            }

            this.updatingChunkMap.put(chunkPos, holder);
            this.modified = true;
         }

         return holder;
      }
   }

   private void onLevelChange(ChunkPos chunkPos, IntSupplier queueLevelGetter, int ticketLevel, IntConsumer queueLevelSetter) {
      this.worldgenTaskDispatcher.onLevelChange(chunkPos, queueLevelGetter, ticketLevel, queueLevelSetter);
      this.lightTaskDispatcher.onLevelChange(chunkPos, queueLevelGetter, ticketLevel, queueLevelSetter);
   }

   @Override
   public void close() throws IOException {
      try {
         this.worldgenTaskDispatcher.close();
         this.lightTaskDispatcher.close();
         this.poiManager.close();
      } finally {
         super.close();
      }
   }

   protected void saveAllChunks(boolean flush) {
      if (flush) {
         List<ChunkHolder> list = this.visibleChunkMap
            .values()
            .stream()
            .filter(ChunkHolder::wasAccessibleSinceLastSave)
            .peek(ChunkHolder::refreshAccessibility)
            .toList();
         MutableBoolean mutableBoolean = new MutableBoolean();

         do {
            mutableBoolean.setFalse();
            list.stream()
               .map(chunkHolderx -> {
                  this.mainThreadExecutor.managedBlock(chunkHolderx::isReadyForSaving);
                  return chunkHolderx.getLatestChunk();
               })
               .filter(chunkAccess -> chunkAccess instanceof ImposterProtoChunk || chunkAccess instanceof LevelChunk)
               .filter(this::save)
               .forEach(chunkAccess -> mutableBoolean.setTrue());
         } while (mutableBoolean.isTrue());

         this.poiManager.flushAll();
         this.processUnloads(() -> true);
         this.synchronize(true).join();
      } else {
         this.nextChunkSaveTime.clear();
         long l = Util.getMillis();
         ObjectIterator var4 = this.visibleChunkMap.values().iterator();

         while (var4.hasNext()) {
            ChunkHolder chunkHolder = (ChunkHolder)var4.next();
            this.saveChunkIfNeeded(chunkHolder, l);
         }
      }
   }

   protected void tick(BooleanSupplier hasTimeLeft) {
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("poi");
      this.poiManager.tick(hasTimeLeft);
      profilerFiller.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(hasTimeLeft);
      }

      profilerFiller.pop();
   }

   public boolean hasWork() {
      return this.lightEngine.hasLightWork()
         || !this.pendingUnloads.isEmpty()
         || !this.updatingChunkMap.isEmpty()
         || this.poiManager.hasWork()
         || !this.toDrop.isEmpty()
         || !this.unloadQueue.isEmpty()
         || this.worldgenTaskDispatcher.hasWork()
         || this.lightTaskDispatcher.hasWork()
         || this.distanceManager.hasTickets();
   }

   private void processUnloads(BooleanSupplier hasTimeLeft) {
      for (LongIterator longIterator = this.toDrop.iterator(); longIterator.hasNext(); longIterator.remove()) {
         long l = longIterator.nextLong();
         ChunkHolder chunkHolder = (ChunkHolder)this.updatingChunkMap.get(l);
         if (chunkHolder != null) {
            this.updatingChunkMap.remove(l);
            this.pendingUnloads.put(l, chunkHolder);
            this.modified = true;
            this.scheduleUnload(l, chunkHolder);
         }
      }

      int i = Math.max(0, this.unloadQueue.size() - 2000);

      Runnable runnable;
      while ((i > 0 || hasTimeLeft.getAsBoolean()) && (runnable = this.unloadQueue.poll()) != null) {
         i--;
         runnable.run();
      }

      this.saveChunksEagerly(hasTimeLeft);
   }

   private void saveChunksEagerly(BooleanSupplier hasTimeLeft) {
      long l = Util.getMillis();
      int i = 0;
      LongIterator longIterator = this.chunksToEagerlySave.iterator();

      while (i < 20 && this.activeChunkWrites.get() < 128 && hasTimeLeft.getAsBoolean() && longIterator.hasNext()) {
         long m = longIterator.nextLong();
         ChunkHolder chunkHolder = (ChunkHolder)this.visibleChunkMap.get(m);
         ChunkAccess chunkAccess = chunkHolder != null ? chunkHolder.getLatestChunk() : null;
         if (chunkAccess == null || !chunkAccess.isUnsaved()) {
            longIterator.remove();
         } else if (this.saveChunkIfNeeded(chunkHolder, l)) {
            i++;
            longIterator.remove();
         }
      }
   }

   private void scheduleUnload(long chunkPos, ChunkHolder chunkHolder) {
      CompletableFuture<?> completableFuture = chunkHolder.getSaveSyncFuture();
      completableFuture.thenRunAsync(() -> {
         CompletableFuture<?> completableFuture2 = chunkHolder.getSaveSyncFuture();
         if (completableFuture2 != completableFuture) {
            this.scheduleUnload(chunkPos, chunkHolder);
         } else {
            ChunkAccess chunkAccess = chunkHolder.getLatestChunk();
            if (this.pendingUnloads.remove(chunkPos, chunkHolder) && chunkAccess != null) {
               if (chunkAccess instanceof LevelChunk levelChunk) {
                  levelChunk.setLoaded(false);
               }

               this.save(chunkAccess);
               if (chunkAccess instanceof LevelChunk levelChunk) {
                  this.level.unload(levelChunk);
               }

               this.lightEngine.updateChunkStatus(chunkAccess.getPos());
               this.lightEngine.tryScheduleUpdate();
               this.nextChunkSaveTime.remove(chunkAccess.getPos().toLong());
            }
         }
      }, this.unloadQueue::add).whenComplete((void_, throwable) -> {
         if (throwable != null) {
            LOGGER.error("Failed to save chunk {}", chunkHolder.getPos(), throwable);
         }
      });
   }

   protected boolean promoteChunkMap() {
      if (!this.modified) {
         return false;
      } else {
         this.visibleChunkMap = this.updatingChunkMap.clone();
         this.modified = false;
         return true;
      }
   }

   private CompletableFuture<ChunkAccess> scheduleChunkLoad(ChunkPos chunkPos) {
      CompletableFuture<Optional<SerializableChunkData>> completableFuture = this.readChunk(chunkPos).thenApplyAsync(optional -> optional.map(compoundTag -> {
         SerializableChunkData serializableChunkData = SerializableChunkData.parse(this.level, this.level.palettedContainerFactory(), compoundTag);
         if (serializableChunkData == null) {
            LOGGER.error("Chunk file at {} is missing level data, skipping", chunkPos);
         }

         return serializableChunkData;
      }), Util.backgroundExecutor().forName("parseChunk"));
      CompletableFuture<?> completableFuture2 = this.poiManager.prefetch(chunkPos);
      return completableFuture.<Object, Optional<SerializableChunkData>>thenCombine(
            (CompletionStage<? extends Object>)completableFuture2, (optional, object) -> optional
         )
         .thenApplyAsync(optional -> {
            Profiler.get().incrementCounter("chunkLoad");
            if (optional.isPresent()) {
               ChunkAccess chunkAccess = optional.get().read(this.level, this.poiManager, this.storageInfo(), chunkPos);
               this.markPosition(chunkPos, chunkAccess.getPersistedStatus().getChunkType());
               return chunkAccess;
            } else {
               return this.createEmptyChunk(chunkPos);
            }
         }, this.mainThreadExecutor)
         .exceptionallyAsync(throwable -> this.handleChunkLoadFailure(throwable, chunkPos), this.mainThreadExecutor);
   }

   private ChunkAccess handleChunkLoadFailure(Throwable exception, ChunkPos chunkPos) {
      Throwable throwable = exception instanceof CompletionException completionException ? completionException.getCause() : exception;
      Throwable throwable2 = throwable instanceof ReportedException reportedException ? reportedException.getCause() : throwable;
      boolean bl = throwable2 instanceof Error;
      boolean bl2 = throwable2 instanceof IOException || throwable2 instanceof NbtException;
      if (!bl) {
         if (!bl2) {
         }

         this.level.getServer().reportChunkLoadFailure(throwable2, this.storageInfo(), chunkPos);
         return this.createEmptyChunk(chunkPos);
      } else {
         CrashReport crashReport = CrashReport.forThrowable(exception, "Exception loading chunk");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Chunk being loaded");
         crashReportCategory.setDetail("pos", chunkPos);
         this.markPositionReplaceable(chunkPos);
         throw new ReportedException(crashReport);
      }
   }

   private ChunkAccess createEmptyChunk(ChunkPos chunkPos) {
      this.markPositionReplaceable(chunkPos);
      return new ProtoChunk(chunkPos, UpgradeData.EMPTY, this.level, this.level.palettedContainerFactory(), null);
   }

   private void markPositionReplaceable(ChunkPos chunkPos) {
      this.chunkTypeCache.put(chunkPos.toLong(), (byte)-1);
   }

   private byte markPosition(ChunkPos chunkPos, ChunkType chunkType) {
      return this.chunkTypeCache.put(chunkPos.toLong(), (byte)(chunkType == ChunkType.PROTOCHUNK ? -1 : 1));
   }

   @Override
   public GenerationChunkHolder acquireGeneration(long chunkPos) {
      ChunkHolder chunkHolder = (ChunkHolder)this.updatingChunkMap.get(chunkPos);
      chunkHolder.increaseGenerationRefCount();
      return chunkHolder;
   }

   @Override
   public void releaseGeneration(GenerationChunkHolder chunk) {
      chunk.decreaseGenerationRefCount();
   }

   @Override
   public CompletableFuture<ChunkAccess> applyStep(GenerationChunkHolder chunk, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache) {
      ChunkPos chunkPos = chunk.getPos();
      if (step.targetStatus() == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(chunkPos);
      } else {
         try {
            GenerationChunkHolder generationChunkHolder = cache.get(chunkPos.x, chunkPos.z);
            ChunkAccess chunkAccess = generationChunkHolder.getChunkIfPresentUnchecked(step.targetStatus().getParent());
            if (chunkAccess == null) {
               throw new IllegalStateException("Parent chunk missing");
            } else {
               return step.apply(this.worldGenContext, cache, chunkAccess);
            }
         } catch (Exception var8) {
            var8.getStackTrace();
            CrashReport crashReport = CrashReport.forThrowable(var8, "Exception generating new chunk");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Chunk to be generated");
            crashReportCategory.setDetail("Status being generated", () -> step.targetStatus().getName());
            crashReportCategory.setDetail("Location", String.format(Locale.ROOT, "%d,%d", chunkPos.x, chunkPos.z));
            crashReportCategory.setDetail("Position hash", ChunkPos.asLong(chunkPos.x, chunkPos.z));
            crashReportCategory.setDetail("Generator", this.generator());
            this.mainThreadExecutor.execute(() -> {
               throw new ReportedException(crashReport);
            });
            throw new ReportedException(crashReport);
         }
      }
   }

   @Override
   public ChunkGenerationTask scheduleGenerationTask(ChunkStatus targetStatus, ChunkPos pos) {
      ChunkGenerationTask chunkGenerationTask = ChunkGenerationTask.create(this, targetStatus, pos);
      this.pendingGenerationTasks.add(chunkGenerationTask);
      return chunkGenerationTask;
   }

   private void runGenerationTask(ChunkGenerationTask task) {
      GenerationChunkHolder generationChunkHolder = task.getCenter();
      this.worldgenTaskDispatcher.submit(() -> {
         CompletableFuture<?> completableFuture = task.runUntilWait();
         if (completableFuture != null) {
            completableFuture.thenRun(() -> this.runGenerationTask(task));
         }
      }, generationChunkHolder.getPos().toLong(), generationChunkHolder::getQueueLevel);
   }

   @Override
   public void runGenerationTasks() {
      this.pendingGenerationTasks.forEach(this::runGenerationTask);
      this.pendingGenerationTasks.clear();
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareTickingChunk(ChunkHolder holder) {
      CompletableFuture<ChunkResult<List<ChunkAccess>>> completableFuture = this.getChunkRangeFuture(holder, 1, i -> ChunkStatus.FULL);
      return completableFuture.thenApplyAsync(chunkResult -> chunkResult.map(list -> {
         LevelChunk levelChunk = (LevelChunk)list.get(list.size() / 2);
         levelChunk.postProcessGeneration(this.level);
         this.level.startTickingChunk(levelChunk);
         CompletableFuture<?> completableFuturex = holder.getSendSyncFuture();
         if (completableFuturex.isDone()) {
            this.onChunkReadyToSend(holder, levelChunk);
         } else {
            completableFuturex.thenAcceptAsync(object -> this.onChunkReadyToSend(holder, levelChunk), this.mainThreadExecutor);
         }

         return levelChunk;
      }), this.mainThreadExecutor);
   }

   private void onChunkReadyToSend(ChunkHolder chunkHolder, LevelChunk chunk) {
      ChunkPos chunkPos = chunk.getPos();

      for (ServerPlayer serverPlayer : this.playerMap.getAllPlayers()) {
         if (serverPlayer.getChunkTrackingView().contains(chunkPos)) {
            markChunkPendingToSend(serverPlayer, chunk);
         }
      }

      this.level.getChunkSource().onChunkReadyToSend(chunkHolder);
      this.level.debugSynchronizers().registerChunk(chunk);
   }

   public CompletableFuture<ChunkResult<LevelChunk>> prepareAccessibleChunk(ChunkHolder chunk) {
      return this.getChunkRangeFuture(chunk, 1, ChunkLevel::getStatusAroundFullChunk)
         .thenApply(chunkResult -> chunkResult.map(list -> (LevelChunk)list.get(list.size() / 2)));
   }

   Stream<ChunkHolder> allChunksWithAtLeastStatus(ChunkStatus status) {
      int i = ChunkLevel.byStatus(status);
      return this.visibleChunkMap.values().stream().filter(chunkHolder -> chunkHolder.getTicketLevel() <= i);
   }

   private boolean saveChunkIfNeeded(ChunkHolder chunk, long gametime) {
      if (chunk.wasAccessibleSinceLastSave() && chunk.isReadyForSaving()) {
         ChunkAccess chunkAccess = chunk.getLatestChunk();
         if (!(chunkAccess instanceof ImposterProtoChunk) && !(chunkAccess instanceof LevelChunk)) {
            return false;
         } else if (!chunkAccess.isUnsaved()) {
            return false;
         } else {
            long l = chunkAccess.getPos().toLong();
            long m = this.nextChunkSaveTime.getOrDefault(l, -1L);
            if (gametime < m) {
               return false;
            } else {
               boolean bl = this.save(chunkAccess);
               chunk.refreshAccessibility();
               if (bl) {
                  this.nextChunkSaveTime.put(l, gametime + 10000L);
               }

               return bl;
            }
         }
      } else {
         return false;
      }
   }

   private boolean save(ChunkAccess chunk) {
      this.poiManager.flush(chunk.getPos());
      if (!chunk.tryMarkSaved()) {
         return false;
      } else {
         ChunkPos chunkPos = chunk.getPos();

         try {
            ChunkStatus chunkStatus = chunk.getPersistedStatus();
            if (chunkStatus.getChunkType() != ChunkType.LEVELCHUNK) {
               if (this.isExistingChunkFull(chunkPos)) {
                  return false;
               }

               if (chunkStatus == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                  return false;
               }
            }

            Profiler.get().incrementCounter("chunkSave");
            this.activeChunkWrites.incrementAndGet();
            SerializableChunkData serializableChunkData = SerializableChunkData.copyOf(this.level, chunk);
            CompletableFuture<CompoundTag> completableFuture = CompletableFuture.supplyAsync(serializableChunkData::write, Util.backgroundExecutor());
            this.write(chunkPos, completableFuture::join).handle((void_, throwable) -> {
               if (throwable != null) {
                  this.level.getServer().reportChunkSaveFailure(throwable, this.storageInfo(), chunkPos);
               }

               this.activeChunkWrites.decrementAndGet();
               return null;
            });
            this.markPosition(chunkPos, chunkStatus.getChunkType());
            return true;
         } catch (Exception var6) {
            this.level.getServer().reportChunkSaveFailure(var6, this.storageInfo(), chunkPos);
            return false;
         }
      }
   }

   private boolean isExistingChunkFull(ChunkPos chunkPos) {
      byte b = this.chunkTypeCache.get(chunkPos.toLong());
      if (b != 0) {
         return b == 1;
      } else {
         CompoundTag compoundTag;
         try {
            compoundTag = this.readChunk(chunkPos).join().orElse(null);
            if (compoundTag == null) {
               this.markPositionReplaceable(chunkPos);
               return false;
            }
         } catch (Exception var5) {
            LOGGER.error("Failed to read chunk {}", chunkPos, var5);
            this.markPositionReplaceable(chunkPos);
            return false;
         }

         ChunkType chunkType = SerializableChunkData.getChunkStatusFromTag(compoundTag).getChunkType();
         return this.markPosition(chunkPos, chunkType) == 1;
      }
   }

   protected void setServerViewDistance(int viewDistance) {
      int i = Mth.clamp(viewDistance, 2, 32);
      if (i != this.serverViewDistance) {
         this.serverViewDistance = i;
         this.distanceManager.updatePlayerTickets(this.serverViewDistance);

         for (ServerPlayer serverPlayer : this.playerMap.getAllPlayers()) {
            this.updateChunkTracking(serverPlayer);
         }
      }
   }

   int getPlayerViewDistance(ServerPlayer player) {
      return Mth.clamp(player.requestedViewDistance(), 2, this.serverViewDistance);
   }

   private void markChunkPendingToSend(ServerPlayer player, ChunkPos chunkPos) {
      LevelChunk levelChunk = this.getChunkToSend(chunkPos.toLong());
      if (levelChunk != null) {
         markChunkPendingToSend(player, levelChunk);
      }
   }

   private static void markChunkPendingToSend(ServerPlayer player, LevelChunk chunk) {
      player.connection.chunkSender.markChunkPendingToSend(chunk);
   }

   private static void dropChunk(ServerPlayer player, ChunkPos chunkPos) {
      player.connection.chunkSender.dropChunk(player, chunkPos);
   }

   @Nullable
   public LevelChunk getChunkToSend(long chunkPos) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos);
      return chunkHolder == null ? null : chunkHolder.getChunkToSend();
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   public net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   void dumpChunks(Writer writer) throws IOException {
      CsvOutput csvOutput = CsvOutput.builder()
         .addColumn("x")
         .addColumn("z")
         .addColumn("level")
         .addColumn("in_memory")
         .addColumn("status")
         .addColumn("full_status")
         .addColumn("accessible_ready")
         .addColumn("ticking_ready")
         .addColumn("entity_ticking_ready")
         .addColumn("ticket")
         .addColumn("spawning")
         .addColumn("block_entity_count")
         .addColumn("ticking_ticket")
         .addColumn("ticking_level")
         .addColumn("block_ticks")
         .addColumn("fluid_ticks")
         .build(writer);
      ObjectBidirectionalIterator var3 = this.visibleChunkMap.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<ChunkHolder> entry = (Entry<ChunkHolder>)var3.next();
         long l = entry.getLongKey();
         ChunkPos chunkPos = new ChunkPos(l);
         ChunkHolder chunkHolder = (ChunkHolder)entry.getValue();
         Optional<ChunkAccess> optional = Optional.ofNullable(chunkHolder.getLatestChunk());
         Optional<LevelChunk> optional2 = optional.flatMap(
            chunkAccess -> chunkAccess instanceof LevelChunk ? Optional.of((LevelChunk)chunkAccess) : Optional.empty()
         );
         csvOutput.writeRow(
            chunkPos.x,
            chunkPos.z,
            chunkHolder.getTicketLevel(),
            optional.isPresent(),
            optional.map(ChunkAccess::getPersistedStatus).orElse(null),
            optional2.map(LevelChunk::getFullStatus).orElse(null),
            printFuture(chunkHolder.getFullChunkFuture()),
            printFuture(chunkHolder.getTickingChunkFuture()),
            printFuture(chunkHolder.getEntityTickingChunkFuture()),
            this.ticketStorage.getTicketDebugString(l, false),
            this.anyPlayerCloseEnoughForSpawning(chunkPos),
            optional2.<Integer>map(levelChunk -> levelChunk.getBlockEntities().size()).orElse(0),
            this.ticketStorage.getTicketDebugString(l, true),
            this.distanceManager.getChunkLevel(l, true),
            optional2.<Integer>map(levelChunk -> levelChunk.getBlockTicks().count()).orElse(0),
            optional2.<Integer>map(levelChunk -> levelChunk.getFluidTicks().count()).orElse(0)
         );
      }
   }

   private static String printFuture(CompletableFuture<ChunkResult<LevelChunk>> future) {
      try {
         ChunkResult<LevelChunk> chunkResult = future.getNow(null);
         if (chunkResult != null) {
            return chunkResult.isSuccess() ? "done" : "unloaded";
         } else {
            return "not completed";
         }
      } catch (CompletionException var2) {
         return "failed " + var2.getCause().getMessage();
      } catch (CancellationException var3) {
         return "cancelled";
      }
   }

   private CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos pos) {
      return this.read(pos).thenApplyAsync(optional -> optional.map(this::upgradeChunkTag), Util.backgroundExecutor().forName("upgradeChunk"));
   }

   private CompoundTag upgradeChunkTag(CompoundTag tag) {
      return this.upgradeChunkTag(tag, -1, getChunkDataFixContextTag(this.level.dimension(), this.generator().getTypeNameForDataFixer()));
   }

   public static CompoundTag getChunkDataFixContextTag(ResourceKey<Level> level, Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> generatorKey) {
      CompoundTag compoundTag = new CompoundTag();
      compoundTag.putString("dimension", level.identifier().toString());
      generatorKey.ifPresent(resourceKey -> compoundTag.putString("generator", resourceKey.identifier().toString()));
      return compoundTag;
   }

   void collectSpawningChunks(List<LevelChunk> output) {
      LongIterator longIterator = this.distanceManager.getSpawnCandidateChunks();

      while (longIterator.hasNext()) {
         ChunkHolder chunkHolder = (ChunkHolder)this.visibleChunkMap.get(longIterator.nextLong());
         if (chunkHolder != null) {
            LevelChunk levelChunk = chunkHolder.getTickingChunk();
            if (levelChunk != null && this.anyPlayerCloseEnoughForSpawningInternal(chunkHolder.getPos())) {
               output.add(levelChunk);
            }
         }
      }
   }

   void forEachBlockTickingChunk(Consumer<LevelChunk> action) {
      this.distanceManager.forEachEntityTickingChunk(l -> {
         ChunkHolder chunkHolder = (ChunkHolder)this.visibleChunkMap.get(l);
         if (chunkHolder != null) {
            LevelChunk levelChunk = chunkHolder.getTickingChunk();
            if (levelChunk != null) {
               action.accept(levelChunk);
            }
         }
      });
   }

   boolean anyPlayerCloseEnoughForSpawning(ChunkPos chunkPos) {
      TriState triState = this.distanceManager.hasPlayersNearby(chunkPos.toLong());
      return triState == TriState.DEFAULT ? this.anyPlayerCloseEnoughForSpawningInternal(chunkPos) : triState.toBoolean(true);
   }

   boolean anyPlayerCloseEnoughTo(BlockPos pos, int distance) {
      Vec3 vec3 = new Vec3(pos);

      for (ServerPlayer serverPlayer : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughTo(serverPlayer, vec3, distance)) {
            return true;
         }
      }

      return false;
   }

   private boolean anyPlayerCloseEnoughForSpawningInternal(ChunkPos chunkPos) {
      for (ServerPlayer serverPlayer : this.playerMap.getAllPlayers()) {
         if (this.playerIsCloseEnoughForSpawning(serverPlayer, chunkPos)) {
            return true;
         }
      }

      return false;
   }

   public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos chunkPos) {
      long l = chunkPos.toLong();
      if (!this.distanceManager.hasPlayersNearby(l).toBoolean(true)) {
         return List.of();
      } else {
         Builder<ServerPlayer> builder = ImmutableList.builder();

         for (ServerPlayer serverPlayer : this.playerMap.getAllPlayers()) {
            if (this.playerIsCloseEnoughForSpawning(serverPlayer, chunkPos)) {
               builder.add(serverPlayer);
            }
         }

         return builder.build();
      }
   }

   private boolean playerIsCloseEnoughForSpawning(ServerPlayer player, ChunkPos chunkPos) {
      if (player.isSpectator()) {
         return false;
      } else {
         double d = euclideanDistanceSquared(chunkPos, player.position());
         return d < 16384.0;
      }
   }

   private boolean playerIsCloseEnoughTo(ServerPlayer player, Vec3 pos, int distance) {
      if (player.isSpectator()) {
         return false;
      } else {
         double d = player.position().distanceTo(pos);
         return d < distance;
      }
   }

   private static double euclideanDistanceSquared(ChunkPos chunkPos, Vec3 pos) {
      double d = SectionPos.sectionToBlockCoord(chunkPos.x, 8);
      double e = SectionPos.sectionToBlockCoord(chunkPos.z, 8);
      double f = d - pos.x;
      double g = e - pos.z;
      return f * f + g * g;
   }

   private boolean skipPlayer(ServerPlayer player) {
      return player.isSpectator() && !this.level.getGameRules().get(GameRules.SPECTATORS_GENERATE_CHUNKS);
   }

   void updatePlayerStatus(ServerPlayer player, boolean track) {
      boolean bl = this.skipPlayer(player);
      boolean bl2 = this.playerMap.ignoredOrUnknown(player);
      if (track) {
         this.playerMap.addPlayer(player, bl);
         this.updatePlayerPos(player);
         if (!bl) {
            this.distanceManager.addPlayer(SectionPos.of(player), player);
         }

         player.setChunkTrackingView(ChunkTrackingView.EMPTY);
         this.updateChunkTracking(player);
      } else {
         SectionPos sectionPos = player.getLastSectionPos();
         this.playerMap.removePlayer(player);
         if (!bl2) {
            this.distanceManager.removePlayer(sectionPos, player);
         }

         this.applyChunkTrackingView(player, ChunkTrackingView.EMPTY);
      }
   }

   private void updatePlayerPos(ServerPlayer player) {
      SectionPos sectionPos = SectionPos.of(player);
      player.setLastSectionPos(sectionPos);
   }

   public void move(ServerPlayer player) {
      ObjectIterator sectionPos = this.entityMap.values().iterator();

      while (sectionPos.hasNext()) {
         ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)sectionPos.next();
         if (trackedEntity.entity == player) {
            trackedEntity.updatePlayers(this.level.players());
         } else {
            trackedEntity.updatePlayer(player);
         }
      }

      SectionPos sectionPosx = player.getLastSectionPos();
      SectionPos sectionPos2 = SectionPos.of(player);
      boolean bl = this.playerMap.ignored(player);
      boolean bl2 = this.skipPlayer(player);
      boolean bl3 = sectionPosx.asLong() != sectionPos2.asLong();
      if (bl3 || bl != bl2) {
         this.updatePlayerPos(player);
         if (!bl) {
            this.distanceManager.removePlayer(sectionPosx, player);
         }

         if (!bl2) {
            this.distanceManager.addPlayer(sectionPos2, player);
         }

         if (!bl && bl2) {
            this.playerMap.ignorePlayer(player);
         }

         if (bl && !bl2) {
            this.playerMap.unIgnorePlayer(player);
         }

         this.updateChunkTracking(player);
      }
   }

   private void updateChunkTracking(ServerPlayer player) {
      ChunkPos chunkPos = player.chunkPosition();
      int i = this.getPlayerViewDistance(player);
      if (!(
         player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned positioned
            && positioned.center().equals(chunkPos)
            && positioned.viewDistance() == i
      )) {
         this.applyChunkTrackingView(player, ChunkTrackingView.of(chunkPos, i));
      }
   }

   private void applyChunkTrackingView(ServerPlayer player, ChunkTrackingView chunkTrackingView) {
      if (player.level() == this.level) {
         ChunkTrackingView chunkTrackingView2 = player.getChunkTrackingView();
         if (chunkTrackingView instanceof ChunkTrackingView.Positioned positioned
            && !(chunkTrackingView2 instanceof ChunkTrackingView.Positioned positioned2 && positioned2.center().equals(positioned.center()))) {
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(positioned.center().x, positioned.center().z));
         }

         ChunkTrackingView.difference(
            chunkTrackingView2, chunkTrackingView, chunkPos -> this.markChunkPendingToSend(player, chunkPos), chunkPos -> dropChunk(player, chunkPos)
         );
         player.setChunkTrackingView(chunkTrackingView);
      }
   }

   @Override
   public List<ServerPlayer> getPlayers(ChunkPos pos, boolean boundaryOnly) {
      Set<ServerPlayer> set = this.playerMap.getAllPlayers();
      Builder<ServerPlayer> builder = ImmutableList.builder();

      for (ServerPlayer serverPlayer : set) {
         if (boundaryOnly && this.isChunkOnTrackedBorder(serverPlayer, pos.x, pos.z) || !boundaryOnly && this.isChunkTracked(serverPlayer, pos.x, pos.z)) {
            builder.add(serverPlayer);
         }
      }

      return builder.build();
   }

   protected void addEntity(Entity entity) {
      if (!(entity instanceof EnderDragonPart)) {
         EntityType<?> entityType = entity.getType();
         int i = entityType.clientTrackingRange() * 16;
         if (i != 0) {
            int j = entityType.updateInterval();
            if (this.entityMap.containsKey(entity.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            } else {
               ChunkMap.TrackedEntity trackedEntity = new ChunkMap.TrackedEntity(entity, i, j, entityType.trackDeltas());
               this.entityMap.put(entity.getId(), trackedEntity);
               trackedEntity.updatePlayers(this.level.players());
               if (entity instanceof ServerPlayer serverPlayer) {
                  this.updatePlayerStatus(serverPlayer, true);
                  ObjectIterator var7 = this.entityMap.values().iterator();

                  while (var7.hasNext()) {
                     ChunkMap.TrackedEntity trackedEntity2 = (ChunkMap.TrackedEntity)var7.next();
                     if (trackedEntity2.entity != serverPlayer) {
                        trackedEntity2.updatePlayer(serverPlayer);
                     }
                  }
               }
            }
         }
      }
   }

   protected void removeEntity(Entity entity) {
      if (entity instanceof ServerPlayer serverPlayer) {
         this.updatePlayerStatus(serverPlayer, false);
         ObjectIterator var3 = this.entityMap.values().iterator();

         while (var3.hasNext()) {
            ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)var3.next();
            trackedEntity.removePlayer(serverPlayer);
         }
      }

      ChunkMap.TrackedEntity trackedEntity2 = (ChunkMap.TrackedEntity)this.entityMap.remove(entity.getId());
      if (trackedEntity2 != null) {
         trackedEntity2.broadcastRemoved();
      }
   }

   protected void tick() {
      for (ServerPlayer serverPlayer : this.playerMap.getAllPlayers()) {
         this.updateChunkTracking(serverPlayer);
      }

      List<ServerPlayer> list = Lists.newArrayList();
      List<ServerPlayer> list2 = this.level.players();
      ObjectIterator var3 = this.entityMap.values().iterator();

      while (var3.hasNext()) {
         ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)var3.next();
         SectionPos sectionPos = trackedEntity.lastSectionPos;
         SectionPos sectionPos2 = SectionPos.of(trackedEntity.entity);
         boolean bl = !Objects.equals(sectionPos, sectionPos2);
         if (bl) {
            trackedEntity.updatePlayers(list2);
            Entity entity = trackedEntity.entity;
            if (entity instanceof ServerPlayer) {
               list.add((ServerPlayer)entity);
            }

            trackedEntity.lastSectionPos = sectionPos2;
         }

         if (bl || trackedEntity.entity.needsSync || this.distanceManager.inEntityTickingRange(sectionPos2.chunk().toLong())) {
            trackedEntity.serverEntity.sendChanges();
         }
      }

      if (!list.isEmpty()) {
         var3 = this.entityMap.values().iterator();

         while (var3.hasNext()) {
            ChunkMap.TrackedEntity trackedEntityx = (ChunkMap.TrackedEntity)var3.next();
            trackedEntityx.updatePlayers(list);
         }
      }
   }

   public void sendToTrackingPlayers(Entity entity, Packet<? super ClientGamePacketListener> packet) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.sendToTrackingPlayers(packet);
      }
   }

   public void sendToTrackingPlayersFiltered(Entity entity, Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.sendToTrackingPlayersFiltered(packet, filter);
      }
   }

   protected void sendToTrackingPlayersAndSelf(Entity entity, Packet<? super ClientGamePacketListener> packet) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      if (trackedEntity != null) {
         trackedEntity.sendToTrackingPlayersAndSelf(packet);
      }
   }

   public boolean isTrackedByAnyPlayer(Entity entity) {
      ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)this.entityMap.get(entity.getId());
      return trackedEntity != null ? !trackedEntity.seenBy.isEmpty() : false;
   }

   public void forEachEntityTrackedBy(ServerPlayer player, Consumer<Entity> action) {
      ObjectIterator var3 = this.entityMap.values().iterator();

      while (var3.hasNext()) {
         ChunkMap.TrackedEntity trackedEntity = (ChunkMap.TrackedEntity)var3.next();
         if (trackedEntity.seenBy.contains(player.connection)) {
            action.accept(trackedEntity.entity);
         }
      }
   }

   public void resendBiomesForChunks(List<ChunkAccess> chunks) {
      Map<ServerPlayer, List<LevelChunk>> map = new HashMap<>();

      for (ChunkAccess chunkAccess : chunks) {
         ChunkPos chunkPos = chunkAccess.getPos();
         LevelChunk levelChunk2;
         if (chunkAccess instanceof LevelChunk levelChunk) {
            levelChunk2 = levelChunk;
         } else {
            levelChunk2 = this.level.getChunk(chunkPos.x, chunkPos.z);
         }

         for (ServerPlayer serverPlayer : this.getPlayers(chunkPos, false)) {
            map.computeIfAbsent(serverPlayer, serverPlayerx -> new ArrayList<>()).add(levelChunk2);
         }
      }

      map.forEach((serverPlayerx, list) -> serverPlayerx.connection.send(ClientboundChunksBiomesPacket.forChunks((List<LevelChunk>)list)));
   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   public String getStorageName() {
      return this.storageName;
   }

   void onFullChunkStatusChange(ChunkPos chunkPos, FullChunkStatus fullChunkStatus) {
      this.chunkStatusListener.onChunkStatusChange(chunkPos, fullChunkStatus);
   }

   public void waitForLightBeforeSending(ChunkPos chunkPos, int range) {
      int i = range + 1;
      ChunkPos.rangeClosed(chunkPos, i).forEach(chunkPosx -> {
         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPosx.toLong());
         if (chunkHolder != null) {
            chunkHolder.addSendDependency(this.lightEngine.waitForPendingTasks(chunkPosx.x, chunkPosx.z));
         }
      });
   }

   public void forEachReadyToSendChunk(Consumer<LevelChunk> action) {
      ObjectIterator var2 = this.visibleChunkMap.values().iterator();

      while (var2.hasNext()) {
         ChunkHolder chunkHolder = (ChunkHolder)var2.next();
         LevelChunk levelChunk = chunkHolder.getChunkToSend();
         if (levelChunk != null) {
            action.accept(levelChunk);
         }
      }
   }

   class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(final TicketStorage ticketStorage, final Executor dispatcher, final Executor mainThreadExecutor) {
         super(ticketStorage, dispatcher, mainThreadExecutor);
      }

      @Override
      protected boolean isChunkToRemove(long chunkPos) {
         return ChunkMap.this.toDrop.contains(chunkPos);
      }

      @Nullable
      @Override
      protected ChunkHolder getChunk(long chunkPos) {
         return ChunkMap.this.getUpdatingChunkIfPresent(chunkPos);
      }

      @Nullable
      @Override
      protected ChunkHolder updateChunkScheduling(long chunkPos, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
         return ChunkMap.this.updateChunkScheduling(chunkPos, newLevel, holder, oldLevel);
      }
   }

   class TrackedEntity implements ServerEntity.Synchronizer {
      final ServerEntity serverEntity;
      final Entity entity;
      private final int range;
      SectionPos lastSectionPos;
      final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(final Entity entity, final int range, final int updateInterval, final boolean trackDelta) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, entity, updateInterval, trackDelta, this);
         this.entity = entity;
         this.range = range;
         this.lastSectionPos = SectionPos.of(entity);
      }

      @Override
      public boolean equals(Object object) {
         return object instanceof ChunkMap.TrackedEntity ? ((ChunkMap.TrackedEntity)object).entity.getId() == this.entity.getId() : false;
      }

      @Override
      public int hashCode() {
         return this.entity.getId();
      }

      @Override
      public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
         for (ServerPlayerConnection serverPlayerConnection : this.seenBy) {
            serverPlayerConnection.send(packet);
         }
      }

      @Override
      public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
         this.sendToTrackingPlayers(packet);
         if (this.entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(packet);
         }
      }

      @Override
      public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {
         for (ServerPlayerConnection serverPlayerConnection : this.seenBy) {
            if (filter.test(serverPlayerConnection.getPlayer())) {
               serverPlayerConnection.send(packet);
            }
         }
      }

      public void broadcastRemoved() {
         for (ServerPlayerConnection serverPlayerConnection : this.seenBy) {
            this.serverEntity.removePairing(serverPlayerConnection.getPlayer());
         }
      }

      public void removePlayer(ServerPlayer player) {
         if (this.seenBy.remove(player.connection)) {
            this.serverEntity.removePairing(player);
            if (this.seenBy.isEmpty()) {
               ChunkMap.this.level.debugSynchronizers().dropEntity(this.entity);
            }
         }
      }

      public void updatePlayer(ServerPlayer player) {
         if (player != this.entity) {
            Vec3 vec3 = player.position().subtract(this.entity.position());
            int i = ChunkMap.this.getPlayerViewDistance(player);
            double d = Math.min(this.getEffectiveRange(), i * 16);
            double e = vec3.x * vec3.x + vec3.z * vec3.z;
            double f = d * d;
            boolean bl = e <= f
               && this.entity.broadcastToPlayer(player)
               && ChunkMap.this.isChunkTracked(player, this.entity.chunkPosition().x, this.entity.chunkPosition().z);
            if (bl) {
               if (this.seenBy.add(player.connection)) {
                  this.serverEntity.addPairing(player);
                  if (this.seenBy.size() == 1) {
                     ChunkMap.this.level.debugSynchronizers().registerEntity(this.entity);
                  }

                  ChunkMap.this.level.debugSynchronizers().startTrackingEntity(player, this.entity);
               }
            } else {
               this.removePlayer(player);
            }
         }
      }

      private int scaledRange(int trackingDistance) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(trackingDistance);
      }

      private int getEffectiveRange() {
         int i = this.range;

         for (Entity entity : this.entity.getIndirectPassengers()) {
            int j = entity.getType().clientTrackingRange() * 16;
            if (j > i) {
               i = j;
            }
         }

         return this.scaledRange(i);
      }

      public void updatePlayers(List<ServerPlayer> playersList) {
         for (ServerPlayer serverPlayer : playersList) {
            this.updatePlayer(serverPlayer);
         }
      }
   }
}
