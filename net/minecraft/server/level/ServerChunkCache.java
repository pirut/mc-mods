package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerChunkCache extends ChunkSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DistanceManager distanceManager;
   private final ServerLevel level;
   final Thread mainThread;
   final ThreadedLevelLightEngine lightEngine;
   private final ServerChunkCache.MainThreadExecutor mainThreadProcessor;
   public final ChunkMap chunkMap;
   private final DimensionDataStorage dataStorage;
   private final TicketStorage ticketStorage;
   private long lastInhabitedUpdate;
   private boolean spawnEnemies = true;
   private static final int CACHE_SIZE = 4;
   private final long[] lastChunkPos = new long[4];
   private final ChunkStatus[] lastChunkStatus = new ChunkStatus[4];
   private final ChunkAccess[] lastChunk = new ChunkAccess[4];
   private final List<LevelChunk> spawningChunks = new ObjectArrayList();
   private final Set<ChunkHolder> chunkHoldersToBroadcast = new ReferenceOpenHashSet();
   @VisibleForDebug
   @Nullable
   private NaturalSpawner.SpawnState lastSpawnState;

   public ServerChunkCache(
      ServerLevel level,
      LevelStorageSource.LevelStorageAccess storageSource,
      DataFixer fixerUpper,
      StructureTemplateManager structureManager,
      Executor dispatcher,
      ChunkGenerator generator,
      int viewDistance,
      int simulationDistance,
      boolean sync,
      ChunkStatusUpdateListener chunkStatusListener,
      Supplier<DimensionDataStorage> overworldDataStorage
   ) {
      this.level = level;
      this.mainThreadProcessor = new ServerChunkCache.MainThreadExecutor(level);
      this.mainThread = Thread.currentThread();
      Path path = storageSource.getDimensionPath(level.dimension()).resolve("data");

      try {
         FileUtil.createDirectoriesSafe(path);
      } catch (IOException var14) {
         LOGGER.error("Failed to create dimension data storage directory", var14);
      }

      this.dataStorage = new DimensionDataStorage(path, fixerUpper, level.registryAccess());
      this.ticketStorage = this.dataStorage.computeIfAbsent(TicketStorage.TYPE);
      this.chunkMap = new ChunkMap(
         level,
         storageSource,
         fixerUpper,
         structureManager,
         dispatcher,
         this.mainThreadProcessor,
         this,
         generator,
         chunkStatusListener,
         overworldDataStorage,
         this.ticketStorage,
         viewDistance,
         sync
      );
      this.lightEngine = this.chunkMap.getLightEngine();
      this.distanceManager = this.chunkMap.getDistanceManager();
      this.distanceManager.updateSimulationDistance(simulationDistance);
      this.clearCache();
   }

   public ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   private ChunkHolder getVisibleChunkIfPresent(long chunkPos) {
      return this.chunkMap.getVisibleChunkIfPresent(chunkPos);
   }

   private void storeInCache(long chunkPos, @Nullable ChunkAccess chunk, ChunkStatus chunkStatus) {
      for (int i = 3; i > 0; i--) {
         this.lastChunkPos[i] = this.lastChunkPos[i - 1];
         this.lastChunkStatus[i] = this.lastChunkStatus[i - 1];
         this.lastChunk[i] = this.lastChunk[i - 1];
      }

      this.lastChunkPos[0] = chunkPos;
      this.lastChunkStatus[0] = chunkStatus;
      this.lastChunk[0] = chunk;
   }

   @Nullable
   @Override
   public ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
      if (Thread.currentThread() != this.mainThread) {
         return CompletableFuture.<ChunkAccess>supplyAsync(() -> this.getChunk(x, z, chunkStatus, requireChunk), this.mainThreadProcessor).join();
      } else {
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.incrementCounter("getChunk");
         long l = ChunkPos.asLong(x, z);

         for (int i = 0; i < 4; i++) {
            if (l == this.lastChunkPos[i] && chunkStatus == this.lastChunkStatus[i]) {
               ChunkAccess chunkAccess = this.lastChunk[i];
               if (chunkAccess != null || !requireChunk) {
                  return chunkAccess;
               }
            }
         }

         profilerFiller.incrementCounter("getChunkCacheMiss");
         CompletableFuture<ChunkResult<ChunkAccess>> completableFuture = this.getChunkFutureMainThread(x, z, chunkStatus, requireChunk);
         this.mainThreadProcessor.managedBlock(completableFuture::isDone);
         ChunkResult<ChunkAccess> chunkResult = completableFuture.join();
         ChunkAccess chunkAccess2 = chunkResult.orElse(null);
         if (chunkAccess2 == null && requireChunk) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + chunkResult.getError()));
         } else {
            this.storeInCache(l, chunkAccess2, chunkStatus);
            return chunkAccess2;
         }
      }
   }

   @Nullable
   @Override
   public LevelChunk getChunkNow(int chunkX, int chunkZ) {
      if (Thread.currentThread() != this.mainThread) {
         return null;
      } else {
         Profiler.get().incrementCounter("getChunkNow");
         long l = ChunkPos.asLong(chunkX, chunkZ);

         for (int i = 0; i < 4; i++) {
            if (l == this.lastChunkPos[i] && this.lastChunkStatus[i] == ChunkStatus.FULL) {
               ChunkAccess chunkAccess = this.lastChunk[i];
               return chunkAccess instanceof LevelChunk ? (LevelChunk)chunkAccess : null;
            }
         }

         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
         if (chunkHolder == null) {
            return null;
         } else {
            ChunkAccess chunkAccess = chunkHolder.getChunkIfPresent(ChunkStatus.FULL);
            if (chunkAccess != null) {
               this.storeInCache(l, chunkAccess, ChunkStatus.FULL);
               if (chunkAccess instanceof LevelChunk) {
                  return (LevelChunk)chunkAccess;
               }
            }

            return null;
         }
      }
   }

   private void clearCache() {
      Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
      Arrays.fill(this.lastChunkStatus, null);
      Arrays.fill(this.lastChunk, null);
   }

   public CompletableFuture<ChunkResult<ChunkAccess>> getChunkFuture(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
      boolean bl = Thread.currentThread() == this.mainThread;
      CompletableFuture<ChunkResult<ChunkAccess>> completableFuture;
      if (bl) {
         completableFuture = this.getChunkFutureMainThread(x, z, chunkStatus, requireChunk);
         this.mainThreadProcessor.managedBlock(completableFuture::isDone);
      } else {
         completableFuture = CompletableFuture.<CompletableFuture<ChunkResult<ChunkAccess>>>supplyAsync(
               () -> this.getChunkFutureMainThread(x, z, chunkStatus, requireChunk), this.mainThreadProcessor
            )
            .thenCompose(completableFuturex -> completableFuturex);
      }

      return completableFuture;
   }

   private CompletableFuture<ChunkResult<ChunkAccess>> getChunkFutureMainThread(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
      ChunkPos chunkPos = new ChunkPos(x, z);
      long l = chunkPos.toLong();
      int i = ChunkLevel.byStatus(chunkStatus);
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
      if (requireChunk) {
         this.addTicket(new Ticket(TicketType.UNKNOWN, i), chunkPos);
         if (this.chunkAbsent(chunkHolder, i)) {
            ProfilerFiller profilerFiller = Profiler.get();
            profilerFiller.push("chunkLoad");
            this.runDistanceManagerUpdates();
            chunkHolder = this.getVisibleChunkIfPresent(l);
            profilerFiller.pop();
            if (this.chunkAbsent(chunkHolder, i)) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
            }
         }
      }

      return this.chunkAbsent(chunkHolder, i)
         ? GenerationChunkHolder.UNLOADED_CHUNK_FUTURE
         : chunkHolder.scheduleChunkGenerationTask(chunkStatus, this.chunkMap);
   }

   private boolean chunkAbsent(@Nullable ChunkHolder chunkHolder, int status) {
      return chunkHolder == null || chunkHolder.getTicketLevel() > status;
   }

   @Override
   public boolean hasChunk(int chunkX, int chunkZ) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(new ChunkPos(chunkX, chunkZ).toLong());
      int i = ChunkLevel.byStatus(ChunkStatus.FULL);
      return !this.chunkAbsent(chunkHolder, i);
   }

   @Nullable
   @Override
   public LightChunk getChunkForLighting(int chunkX, int chunkZ) {
      long l = ChunkPos.asLong(chunkX, chunkZ);
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
      return chunkHolder == null ? null : chunkHolder.getChunkIfPresentUnchecked(ChunkStatus.INITIALIZE_LIGHT.getParent());
   }

   public Level getLevel() {
      return this.level;
   }

   public boolean pollTask() {
      return this.mainThreadProcessor.pollTask();
   }

   boolean runDistanceManagerUpdates() {
      boolean bl = this.distanceManager.runAllUpdates(this.chunkMap);
      boolean bl2 = this.chunkMap.promoteChunkMap();
      this.chunkMap.runGenerationTasks();
      if (!bl && !bl2) {
         return false;
      } else {
         this.clearCache();
         return true;
      }
   }

   public boolean isPositionTicking(long chunkPos) {
      if (!this.level.shouldTickBlocksAt(chunkPos)) {
         return false;
      } else {
         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos);
         return chunkHolder == null ? false : chunkHolder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).isSuccess();
      }
   }

   public void save(boolean flush) {
      this.runDistanceManagerUpdates();
      this.chunkMap.saveAllChunks(flush);
   }

   @Override
   public void close() throws IOException {
      this.save(true);
      this.dataStorage.close();
      this.lightEngine.close();
      this.chunkMap.close();
   }

   @Override
   public void tick(BooleanSupplier hasTimeLeft, boolean tickChunks) {
      ProfilerFiller profilerFiller = Profiler.get();
      profilerFiller.push("purge");
      if (this.level.tickRateManager().runsNormally() || !tickChunks) {
         this.ticketStorage.purgeStaleTickets(this.chunkMap);
      }

      this.runDistanceManagerUpdates();
      profilerFiller.popPush("chunks");
      if (tickChunks) {
         this.tickChunks();
         this.chunkMap.tick();
      }

      profilerFiller.popPush("unload");
      this.chunkMap.tick(hasTimeLeft);
      profilerFiller.pop();
      this.clearCache();
   }

   private void tickChunks() {
      long l = this.level.getGameTime();
      long m = l - this.lastInhabitedUpdate;
      this.lastInhabitedUpdate = l;
      if (!this.level.isDebug()) {
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.push("pollingChunks");
         if (this.level.tickRateManager().runsNormally()) {
            profilerFiller.push("tickingChunks");
            this.tickChunks(profilerFiller, m);
            profilerFiller.pop();
         }

         this.broadcastChangedChunks(profilerFiller);
         profilerFiller.pop();
      }
   }

   private void broadcastChangedChunks(ProfilerFiller profiler) {
      profiler.push("broadcast");

      for (ChunkHolder chunkHolder : this.chunkHoldersToBroadcast) {
         LevelChunk levelChunk = chunkHolder.getTickingChunk();
         if (levelChunk != null) {
            chunkHolder.broadcastChanges(levelChunk);
         }
      }

      this.chunkHoldersToBroadcast.clear();
      profiler.pop();
   }

   private void tickChunks(ProfilerFiller profiler, long timeInhabited) {
      profiler.push("naturalSpawnCount");
      int i = this.distanceManager.getNaturalSpawnChunkCount();
      NaturalSpawner.SpawnState spawnState = NaturalSpawner.createState(
         i, this.level.getAllEntities(), this::getFullChunk, new LocalMobCapCalculator(this.chunkMap)
      );
      this.lastSpawnState = spawnState;
      boolean bl = this.level.getGameRules().get(GameRules.SPAWN_MOBS);
      int j = this.level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
      List<MobCategory> list;
      if (bl) {
         boolean bl2 = this.level.getGameTime() % 400L == 0L;
         list = NaturalSpawner.getFilteredSpawningCategories(spawnState, true, this.spawnEnemies, bl2);
      } else {
         list = List.of();
      }

      List<LevelChunk> list2 = this.spawningChunks;

      try {
         profiler.popPush("filteringSpawningChunks");
         this.chunkMap.collectSpawningChunks(list2);
         profiler.popPush("shuffleSpawningChunks");
         Util.shuffle(list2, this.level.random);
         profiler.popPush("tickSpawningChunks");

         for (LevelChunk levelChunk : list2) {
            this.tickSpawningChunk(levelChunk, timeInhabited, list, spawnState);
         }
      } finally {
         list2.clear();
      }

      profiler.popPush("tickTickingChunks");
      this.chunkMap.forEachBlockTickingChunk(levelChunkx -> this.level.tickChunk(levelChunkx, j));
      if (bl) {
         profiler.popPush("customSpawners");
         this.level.tickCustomSpawners(this.spawnEnemies);
      }

      profiler.pop();
   }

   private void tickSpawningChunk(LevelChunk chunk, long timeInhabited, List<MobCategory> spawnCategories, NaturalSpawner.SpawnState spawnState) {
      ChunkPos chunkPos = chunk.getPos();
      chunk.incrementInhabitedTime(timeInhabited);
      if (this.distanceManager.inEntityTickingRange(chunkPos.toLong())) {
         this.level.tickThunder(chunk);
      }

      if (!spawnCategories.isEmpty()) {
         if (this.level.canSpawnEntitiesInChunk(chunkPos)) {
            NaturalSpawner.spawnForChunk(this.level, chunk, spawnState, spawnCategories);
         }
      }
   }

   private void getFullChunk(long chunkPos, Consumer<LevelChunk> fullChunkGetter) {
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos);
      if (chunkHolder != null) {
         chunkHolder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).ifSuccess(fullChunkGetter);
      }
   }

   @Override
   public String gatherStats() {
      return Integer.toString(this.getLoadedChunksCount());
   }

   @VisibleForTesting
   public int getPendingTasksCount() {
      return this.mainThreadProcessor.getPendingTasksCount();
   }

   public ChunkGenerator getGenerator() {
      return this.chunkMap.generator();
   }

   public ChunkGeneratorStructureState getGeneratorState() {
      return this.chunkMap.generatorState();
   }

   public RandomState randomState() {
      return this.chunkMap.randomState();
   }

   @Override
   public int getLoadedChunksCount() {
      return this.chunkMap.size();
   }

   public void blockChanged(BlockPos pos) {
      int i = SectionPos.blockToSectionCoord(pos.getX());
      int j = SectionPos.blockToSectionCoord(pos.getZ());
      ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(ChunkPos.asLong(i, j));
      if (chunkHolder != null && chunkHolder.blockChanged(pos)) {
         this.chunkHoldersToBroadcast.add(chunkHolder);
      }
   }

   @Override
   public void onLightUpdate(LightLayer layer, SectionPos pos) {
      this.mainThreadProcessor.execute(() -> {
         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(pos.chunk().toLong());
         if (chunkHolder != null && chunkHolder.sectionLightChanged(layer, pos.y())) {
            this.chunkHoldersToBroadcast.add(chunkHolder);
         }
      });
   }

   public boolean hasActiveTickets() {
      return this.ticketStorage.shouldKeepDimensionActive();
   }

   public void addTicket(Ticket ticket, ChunkPos chunkPos) {
      this.ticketStorage.addTicket(ticket, chunkPos);
   }

   public CompletableFuture<?> addTicketAndLoadWithRadius(TicketType ticketType, ChunkPos chunkPos, int radius) {
      if (!ticketType.doesLoad()) {
         throw new IllegalStateException("Ticket type " + ticketType + " does not trigger chunk loading");
      } else if (ticketType.canExpireIfUnloaded()) {
         throw new IllegalStateException("Ticket type " + ticketType + " can expire before it loads, cannot fetch asynchronously");
      } else {
         this.addTicketWithRadius(ticketType, chunkPos, radius);
         this.runDistanceManagerUpdates();
         ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos.toLong());
         Objects.requireNonNull(chunkHolder, "No chunk was scheduled for loading");
         return this.chunkMap.getChunkRangeFuture(chunkHolder, radius, i -> ChunkStatus.FULL);
      }
   }

   public void addTicketWithRadius(TicketType ticket, ChunkPos chunkPos, int radius) {
      this.ticketStorage.addTicketWithRadius(ticket, chunkPos, radius);
   }

   public void removeTicketWithRadius(TicketType ticket, ChunkPos chunkPos, int radius) {
      this.ticketStorage.removeTicketWithRadius(ticket, chunkPos, radius);
   }

   @Override
   public boolean updateChunkForced(ChunkPos chunkPos, boolean add) {
      return this.ticketStorage.updateChunkForced(chunkPos, add);
   }

   @Override
   public LongSet getForceLoadedChunks() {
      return this.ticketStorage.getForceLoadedChunks();
   }

   public void move(ServerPlayer player) {
      if (!player.isRemoved()) {
         this.chunkMap.move(player);
         if (player.isReceivingWaypoints()) {
            this.level.getWaypointManager().updatePlayer(player);
         }
      }
   }

   public void removeEntity(Entity entity) {
      this.chunkMap.removeEntity(entity);
   }

   public void addEntity(Entity entity) {
      this.chunkMap.addEntity(entity);
   }

   public void sendToTrackingPlayersAndSelf(Entity entity, Packet<? super ClientGamePacketListener> packet) {
      this.chunkMap.sendToTrackingPlayersAndSelf(entity, packet);
   }

   public void sendToTrackingPlayers(Entity entity, Packet<? super ClientGamePacketListener> packet) {
      this.chunkMap.sendToTrackingPlayers(entity, packet);
   }

   public void setViewDistance(int viewDistance) {
      this.chunkMap.setServerViewDistance(viewDistance);
   }

   public void setSimulationDistance(int simulationDistance) {
      this.distanceManager.updateSimulationDistance(simulationDistance);
   }

   @Override
   public void setSpawnSettings(boolean spawnSettings) {
      this.spawnEnemies = spawnSettings;
   }

   public String getChunkDebugData(ChunkPos chunkPos) {
      return this.chunkMap.getChunkDebugData(chunkPos);
   }

   public DimensionDataStorage getDataStorage() {
      return this.dataStorage;
   }

   public PoiManager getPoiManager() {
      return this.chunkMap.getPoiManager();
   }

   public ChunkScanAccess chunkScanner() {
      return this.chunkMap.chunkScanner();
   }

   @VisibleForDebug
   @Nullable
   public NaturalSpawner.SpawnState getLastSpawnState() {
      return this.lastSpawnState;
   }

   public void deactivateTicketsOnClosing() {
      this.ticketStorage.deactivateTicketsOnClosing();
   }

   public void onChunkReadyToSend(ChunkHolder chunkHolder) {
      if (chunkHolder.hasChangesToBroadcast()) {
         this.chunkHoldersToBroadcast.add(chunkHolder);
      }
   }

   final class MainThreadExecutor extends BlockableEventLoop<Runnable> {
      MainThreadExecutor(final Level level) {
         super("Chunk source main thread executor for " + level.dimension().identifier());
      }

      @Override
      public void managedBlock(BooleanSupplier isDone) {
         super.managedBlock(() -> MinecraftServer.throwIfFatalException() && isDone.getAsBoolean());
      }

      @Override
      public Runnable wrapRunnable(Runnable runnable) {
         return runnable;
      }

      @Override
      protected boolean shouldRun(Runnable runnable) {
         return true;
      }

      @Override
      protected boolean scheduleExecutables() {
         return true;
      }

      @Override
      protected Thread getRunningThread() {
         return ServerChunkCache.this.mainThread;
      }

      @Override
      protected void doRunTask(Runnable task) {
         Profiler.get().incrementCounter("runTask");
         super.doRunTask(task);
      }

      @Override
      protected boolean pollTask() {
         if (ServerChunkCache.this.runDistanceManagerUpdates()) {
            return true;
         } else {
            ServerChunkCache.this.lightEngine.tryScheduleUpdate();
            return super.pollTask();
         }
      }
   }
}
