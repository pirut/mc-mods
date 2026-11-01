package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess implements DebugValueSource {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
      @Override
      public void tick() {
      }

      @Override
      public boolean isRemoved() {
         return true;
      }

      @Override
      public BlockPos getPos() {
         return BlockPos.ZERO;
      }

      @Override
      public String getType() {
         return "<null>";
      }
   };
   private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
   private boolean loaded;
   final Level level;
   @Nullable
   private Supplier<FullChunkStatus> fullStatus;
   @Nullable
   private LevelChunk.PostLoadProcessor postLoad;
   private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
   private final LevelChunkTicks<Block> blockTicks;
   private final LevelChunkTicks<Fluid> fluidTicks;
   private LevelChunk.UnsavedListener unsavedListener = chunkPos -> {};

   public LevelChunk(Level level, ChunkPos pos) {
      this(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
   }

   public LevelChunk(
      Level level,
      ChunkPos pos,
      UpgradeData data,
      LevelChunkTicks<Block> blockTicks,
      LevelChunkTicks<Fluid> fluidTicks,
      long inhabitedTime,
      @Nullable LevelChunkSection[] sections,
      @Nullable LevelChunk.PostLoadProcessor postLoad,
      @Nullable BlendingData blendingData
   ) {
      super(pos, data, level, level.palettedContainerFactory(), inhabitedTime, sections, blendingData);
      this.level = level;
      this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap();

      for (Heightmap.Types types : Heightmap.Types.values()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(types)) {
            this.heightmaps.put(types, new Heightmap(this, types));
         }
      }

      this.postLoad = postLoad;
      this.blockTicks = blockTicks;
      this.fluidTicks = fluidTicks;
   }

   public LevelChunk(ServerLevel level, ProtoChunk chunk, @Nullable LevelChunk.PostLoadProcessor postLoad) {
      this(
         level,
         chunk.getPos(),
         chunk.getUpgradeData(),
         chunk.unpackBlockTicks(),
         chunk.unpackFluidTicks(),
         chunk.getInhabitedTime(),
         chunk.getSections(),
         postLoad,
         chunk.getBlendingData()
      );
      if (!Collections.disjoint(chunk.pendingBlockEntities.keySet(), chunk.blockEntities.keySet())) {
         LOGGER.error("Chunk at {} contains duplicated block entities", chunk.getPos());
      }

      for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
         this.setBlockEntity(blockEntity);
      }

      this.pendingBlockEntities.putAll(chunk.getBlockEntityNbts());

      for (int i = 0; i < chunk.getPostProcessing().length; i++) {
         this.postProcessing[i] = chunk.getPostProcessing()[i];
      }

      this.setAllStarts(chunk.getAllStarts());
      this.setAllReferences(chunk.getAllReferences());

      for (Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
            this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
         }
      }

      this.skyLightSources = chunk.skyLightSources;
      this.setLightCorrect(chunk.isLightCorrect());
      this.markUnsaved();
   }

   public void setUnsavedListener(LevelChunk.UnsavedListener unsavedListener) {
      this.unsavedListener = unsavedListener;
      if (this.isUnsaved()) {
         unsavedListener.setUnsaved(this.chunkPos);
      }
   }

   @Override
   public void markUnsaved() {
      boolean bl = this.isUnsaved();
      super.markUnsaved();
      if (!bl) {
         this.unsavedListener.setUnsaved(this.chunkPos);
      }
   }

   @Override
   public TickContainerAccess<Block> getBlockTicks() {
      return this.blockTicks;
   }

   @Override
   public TickContainerAccess<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Override
   public ChunkAccess.PackedTicks getTicksForSerialization(long gametime) {
      return new ChunkAccess.PackedTicks(this.blockTicks.pack(gametime), this.fluidTicks.pack(gametime));
   }

   @Override
   public GameEventListenerRegistry getListenerRegistry(int sectionY) {
      return this.level instanceof ServerLevel serverLevel
         ? (GameEventListenerRegistry)this.gameEventListenerRegistrySections
            .computeIfAbsent(sectionY, j -> new EuclideanGameEventListenerRegistry(serverLevel, sectionY, this::removeGameEventListenerRegistry))
         : super.getListenerRegistry(sectionY);
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      int i = pos.getX();
      int j = pos.getY();
      int k = pos.getZ();
      if (this.level.isDebug()) {
         BlockState blockState = null;
         if (j == 60) {
            blockState = Blocks.BARRIER.defaultBlockState();
         }

         if (j == 70) {
            blockState = DebugLevelSource.getBlockStateFor(i, k);
         }

         return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
      } else {
         try {
            int l = this.getSectionIndex(j);
            if (l >= 0 && l < this.sections.length) {
               LevelChunkSection levelChunkSection = this.sections[l];
               if (!levelChunkSection.hasOnlyAir()) {
                  return levelChunkSection.getBlockState(i & 15, j & 15, k & 15);
               }
            }

            return Blocks.AIR.defaultBlockState();
         } catch (Throwable var8) {
            CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
            crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
            throw new ReportedException(crashReport);
         }
      }
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
   }

   public FluidState getFluidState(int x, int y, int z) {
      try {
         int i = this.getSectionIndex(y);
         if (i >= 0 && i < this.sections.length) {
            LevelChunkSection levelChunkSection = this.sections[i];
            if (!levelChunkSection.hasOnlyAir()) {
               return levelChunkSection.getFluidState(x & 15, y & 15, z & 15);
            }
         }

         return Fluids.EMPTY.defaultFluidState();
      } catch (Throwable var7) {
         CrashReport crashReport = CrashReport.forThrowable(var7, "Getting fluid state");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
         crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, x, y, z));
         throw new ReportedException(crashReport);
      }
   }

   @Nullable
   @Override
   public BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
      int i = pos.getY();
      LevelChunkSection levelChunkSection = this.getSection(this.getSectionIndex(i));
      boolean bl = levelChunkSection.hasOnlyAir();
      if (bl && state.isAir()) {
         return null;
      } else {
         int j = pos.getX() & 15;
         int k = i & 15;
         int l = pos.getZ() & 15;
         BlockState blockState = levelChunkSection.setBlockState(j, k, l, state);
         if (blockState == state) {
            return null;
         } else {
            Block block = state.getBlock();
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, i, l, state);
            this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, state);
            this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, i, l, state);
            this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, i, l, state);
            boolean bl2 = levelChunkSection.hasOnlyAir();
            if (bl != bl2) {
               this.level.getChunkSource().getLightEngine().updateSectionStatus(pos, bl2);
               this.level.getChunkSource().onSectionEmptinessChanged(this.chunkPos.x, SectionPos.blockToSectionCoord(i), this.chunkPos.z, bl2);
            }

            if (LightEngine.hasDifferentLightProperties(blockState, state)) {
               ProfilerFiller profilerFiller = Profiler.get();
               profilerFiller.push("updateSkyLightSources");
               this.skyLightSources.update(this, j, i, l);
               profilerFiller.popPush("queueCheckLight");
               this.level.getChunkSource().getLightEngine().checkBlock(pos);
               profilerFiller.pop();
            }

            boolean bl3 = !blockState.is(block);
            boolean bl4 = (flags & 64) != 0;
            boolean bl5 = (flags & 256) == 0;
            if (bl3 && blockState.hasBlockEntity() && !state.shouldChangedStateKeepBlockEntity(blockState)) {
               if (!this.level.isClientSide() && bl5) {
                  BlockEntity blockEntity = this.level.getBlockEntity(pos);
                  if (blockEntity != null) {
                     blockEntity.preRemoveSideEffects(pos, blockState);
                  }
               }

               this.removeBlockEntity(pos);
            }

            if ((bl3 || block instanceof BaseRailBlock) && this.level instanceof ServerLevel serverLevel && ((flags & 1) != 0 || bl4)) {
               blockState.affectNeighborsAfterRemoval(serverLevel, pos, bl4);
            }

            if (!levelChunkSection.getBlockState(j, k, l).is(block)) {
               return null;
            } else {
               if (!this.level.isClientSide() && (flags & 512) == 0) {
                  state.onPlace(this.level, pos, blockState, bl4);
               }

               if (state.hasBlockEntity()) {
                  BlockEntity blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                  if (blockEntity != null && !blockEntity.isValidBlockState(state)) {
                     LOGGER.warn(
                        "Found mismatched block entity @ {}: type = {}, state = {}",
                        new Object[]{pos, blockEntity.getType().builtInRegistryHolder().key().identifier(), state}
                     );
                     this.removeBlockEntity(pos);
                     blockEntity = null;
                  }

                  if (blockEntity == null) {
                     blockEntity = ((EntityBlock)block).newBlockEntity(pos, state);
                     if (blockEntity != null) {
                        this.addAndRegisterBlockEntity(blockEntity);
                     }
                  } else {
                     blockEntity.setBlockState(state);
                     this.updateBlockEntityTicker(blockEntity);
                  }
               }

               this.markUnsaved();
               return blockState;
            }
         }
      }
   }

   @Deprecated
   @Override
   public void addEntity(Entity entity) {
   }

   @Nullable
   private BlockEntity createBlockEntity(BlockPos pos) {
      BlockState blockState = this.getBlockState(pos);
      return !blockState.hasBlockEntity() ? null : ((EntityBlock)blockState.getBlock()).newBlockEntity(pos, blockState);
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationType) {
      BlockEntity blockEntity = this.blockEntities.get(pos);
      if (blockEntity == null) {
         CompoundTag compoundTag = this.pendingBlockEntities.remove(pos);
         if (compoundTag != null) {
            BlockEntity blockEntity2 = this.promotePendingBlockEntity(pos, compoundTag);
            if (blockEntity2 != null) {
               return blockEntity2;
            }
         }
      }

      if (blockEntity == null) {
         if (creationType == LevelChunk.EntityCreationType.IMMEDIATE) {
            blockEntity = this.createBlockEntity(pos);
            if (blockEntity != null) {
               this.addAndRegisterBlockEntity(blockEntity);
            }
         }
      } else if (blockEntity.isRemoved()) {
         this.blockEntities.remove(pos);
         return null;
      }

      return blockEntity;
   }

   public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
      this.setBlockEntity(blockEntity);
      if (this.isInLevel()) {
         if (this.level instanceof ServerLevel serverLevel) {
            this.addGameEventListener(blockEntity, serverLevel);
         }

         this.level.onBlockEntityAdded(blockEntity);
         this.updateBlockEntityTicker(blockEntity);
      }
   }

   private boolean isInLevel() {
      return this.loaded || this.level.isClientSide();
   }

   boolean isTicking(BlockPos pos) {
      if (!this.level.getWorldBorder().isWithinBounds(pos)) {
         return false;
      } else {
         return !(this.level instanceof ServerLevel serverLevel)
            ? true
            : this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverLevel.areEntitiesLoaded(ChunkPos.asLong(pos));
      }
   }

   @Override
   public void setBlockEntity(BlockEntity blockEntity) {
      BlockPos blockPos = blockEntity.getBlockPos();
      BlockState blockState = this.getBlockState(blockPos);
      if (!blockState.hasBlockEntity()) {
         LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{blockEntity, blockPos, blockState});
      } else {
         BlockState blockState2 = blockEntity.getBlockState();
         if (blockState != blockState2) {
            if (!blockEntity.getType().isValid(blockState)) {
               LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{blockEntity, blockPos, blockState});
               return;
            }

            if (blockState.getBlock() != blockState2.getBlock()) {
               LOGGER.warn(
                  "Block state mismatch on block entity {} in position {}, {} != {}, updating", new Object[]{blockEntity, blockPos, blockState, blockState2}
               );
            }

            blockEntity.setBlockState(blockState);
         }

         blockEntity.setLevel(this.level);
         blockEntity.clearRemoved();
         BlockEntity blockEntity2 = this.blockEntities.put(blockPos.immutable(), blockEntity);
         if (blockEntity2 != null && blockEntity2 != blockEntity) {
            blockEntity2.setRemoved();
         }
      }
   }

   @Nullable
   @Override
   public CompoundTag getBlockEntityNbtForSaving(BlockPos pos, HolderLookup.Provider registries) {
      BlockEntity blockEntity = this.getBlockEntity(pos);
      if (blockEntity != null && !blockEntity.isRemoved()) {
         CompoundTag compoundTag = blockEntity.saveWithFullMetadata(this.level.registryAccess());
         compoundTag.putBoolean("keepPacked", false);
         return compoundTag;
      } else {
         CompoundTag compoundTag = this.pendingBlockEntities.get(pos);
         if (compoundTag != null) {
            compoundTag = compoundTag.copy();
            compoundTag.putBoolean("keepPacked", true);
         }

         return compoundTag;
      }
   }

   @Override
   public void removeBlockEntity(BlockPos pos) {
      if (this.isInLevel()) {
         BlockEntity blockEntity = this.blockEntities.remove(pos);
         if (blockEntity != null) {
            if (this.level instanceof ServerLevel serverLevel) {
               this.removeGameEventListener(blockEntity, serverLevel);
               serverLevel.debugSynchronizers().dropBlockEntity(pos);
            }

            blockEntity.setRemoved();
         }
      }

      this.removeBlockEntityTicker(pos);
   }

   private <T extends BlockEntity> void removeGameEventListener(T blockEntity, ServerLevel level) {
      Block block = blockEntity.getBlockState().getBlock();
      if (block instanceof EntityBlock) {
         GameEventListener gameEventListener = ((EntityBlock)block).getListener(level, blockEntity);
         if (gameEventListener != null) {
            int i = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
            GameEventListenerRegistry gameEventListenerRegistry = this.getListenerRegistry(i);
            gameEventListenerRegistry.unregister(gameEventListener);
         }
      }
   }

   private void removeGameEventListenerRegistry(int sectionY) {
      this.gameEventListenerRegistrySections.remove(sectionY);
   }

   private void removeBlockEntityTicker(BlockPos pos) {
      LevelChunk.RebindableTickingBlockEntityWrapper rebindableTickingBlockEntityWrapper = this.tickersInLevel.remove(pos);
      if (rebindableTickingBlockEntityWrapper != null) {
         rebindableTickingBlockEntityWrapper.rebind(NULL_TICKER);
      }
   }

   public void runPostLoad() {
      if (this.postLoad != null) {
         this.postLoad.run(this);
         this.postLoad = null;
      }
   }

   public boolean isEmpty() {
      return false;
   }

   public void replaceWithPacketData(
      FriendlyByteBuf buffer, Map<Heightmap.Types, long[]> heightmaps, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> outputTagConsumer
   ) {
      this.clearAllBlockEntities();

      for (LevelChunkSection levelChunkSection : this.sections) {
         levelChunkSection.read(buffer);
      }

      heightmaps.forEach(this::setHeightmap);
      this.initializeLightSources();

      try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         outputTagConsumer.accept(
            (blockPos, blockEntityType, compoundTag) -> {
               BlockEntity blockEntity = this.getBlockEntity(blockPos, LevelChunk.EntityCreationType.IMMEDIATE);
               if (blockEntity != null && compoundTag != null && blockEntity.getType() == blockEntityType) {
                  blockEntity.loadWithComponents(
                     TagValueInput.create(scopedCollector.forChild(blockEntity.problemPath()), this.level.registryAccess(), compoundTag)
                  );
               }
            }
         );
      }
   }

   public void replaceBiomes(FriendlyByteBuf buffer) {
      for (LevelChunkSection levelChunkSection : this.sections) {
         levelChunkSection.readBiomes(buffer);
      }
   }

   public void setLoaded(boolean loaded) {
      this.loaded = loaded;
   }

   public Level getLevel() {
      return this.level;
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public void postProcessGeneration(ServerLevel level) {
      ChunkPos chunkPos = this.getPos();

      for (int i = 0; i < this.postProcessing.length; i++) {
         ShortList shortList = this.postProcessing[i];
         if (shortList != null) {
            ShortListIterator var5 = shortList.iterator();

            while (var5.hasNext()) {
               Short short_ = (Short)var5.next();
               BlockPos blockPos = ProtoChunk.unpackOffsetCoordinates(short_, this.getSectionYFromSectionIndex(i), chunkPos);
               BlockState blockState = this.getBlockState(blockPos);
               FluidState fluidState = blockState.getFluidState();
               if (!fluidState.isEmpty()) {
                  fluidState.tick(level, blockPos, blockState);
               }

               if (!(blockState.getBlock() instanceof LiquidBlock)) {
                  BlockState blockState2 = Block.updateFromNeighbourShapes(blockState, level, blockPos);
                  if (blockState2 != blockState) {
                     level.setBlock(blockPos, blockState2, 276);
                  }
               }
            }

            shortList.clear();
         }
      }

      UnmodifiableIterator var11 = ImmutableList.copyOf(this.pendingBlockEntities.keySet()).iterator();

      while (var11.hasNext()) {
         BlockPos blockPos2 = (BlockPos)var11.next();
         this.getBlockEntity(blockPos2);
      }

      this.pendingBlockEntities.clear();
      this.upgradeData.upgrade(this);
   }

   @Nullable
   private BlockEntity promotePendingBlockEntity(BlockPos pos, CompoundTag tag) {
      BlockState blockState = this.getBlockState(pos);
      BlockEntity blockEntity;
      if ("DUMMY".equals(tag.getStringOr("id", ""))) {
         if (blockState.hasBlockEntity()) {
            blockEntity = ((EntityBlock)blockState.getBlock()).newBlockEntity(pos, blockState);
         } else {
            blockEntity = null;
            LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, blockState);
         }
      } else {
         blockEntity = BlockEntity.loadStatic(pos, blockState, tag, this.level.registryAccess());
      }

      if (blockEntity != null) {
         blockEntity.setLevel(this.level);
         this.addAndRegisterBlockEntity(blockEntity);
      } else {
         LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockState, pos);
      }

      return blockEntity;
   }

   public void unpackTicks(long pos) {
      this.blockTicks.unpack(pos);
      this.fluidTicks.unpack(pos);
   }

   public void registerTickContainerInLevel(ServerLevel level) {
      level.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
      level.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
   }

   public void unregisterTickContainerFromLevel(ServerLevel level) {
      level.getBlockTicks().removeContainer(this.chunkPos);
      level.getFluidTicks().removeContainer(this.chunkPos);
   }

   @Override
   public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
      if (!this.getAllStarts().isEmpty()) {
         registration.register(DebugSubscriptions.STRUCTURES, () -> {
            List<DebugStructureInfo> list = new ArrayList<>();

            for (StructureStart structureStart : this.getAllStarts().values()) {
               BoundingBox boundingBox = structureStart.getBoundingBox();
               List<StructurePiece> list2 = structureStart.getPieces();
               List<DebugStructureInfo.Piece> list3 = new ArrayList<>(list2.size());

               for (int i = 0; i < list2.size(); i++) {
                  boolean bl = i == 0;
                  list3.add(new DebugStructureInfo.Piece(list2.get(i).getBoundingBox(), bl));
               }

               list.add(new DebugStructureInfo(boundingBox, list3));
            }

            return list;
         });
      }

      registration.register(DebugSubscriptions.RAIDS, () -> level.getRaids().getRaidCentersInChunk(this.chunkPos));
   }

   @Override
   public ChunkStatus getPersistedStatus() {
      return ChunkStatus.FULL;
   }

   public FullChunkStatus getFullStatus() {
      return this.fullStatus == null ? FullChunkStatus.FULL : this.fullStatus.get();
   }

   public void setFullStatus(Supplier<FullChunkStatus> fullStatus) {
      this.fullStatus = fullStatus;
   }

   public void clearAllBlockEntities() {
      this.blockEntities.values().forEach(BlockEntity::setRemoved);
      this.blockEntities.clear();
      this.tickersInLevel.values().forEach(rebindableTickingBlockEntityWrapper -> rebindableTickingBlockEntityWrapper.rebind(NULL_TICKER));
      this.tickersInLevel.clear();
   }

   public void registerAllBlockEntitiesAfterLevelLoad() {
      this.blockEntities.values().forEach(blockEntity -> {
         if (this.level instanceof ServerLevel serverLevel) {
            this.addGameEventListener(blockEntity, serverLevel);
         }

         this.level.onBlockEntityAdded(blockEntity);
         this.updateBlockEntityTicker(blockEntity);
      });
   }

   private <T extends BlockEntity> void addGameEventListener(T blockEntity, ServerLevel level) {
      Block block = blockEntity.getBlockState().getBlock();
      if (block instanceof EntityBlock) {
         GameEventListener gameEventListener = ((EntityBlock)block).getListener(level, blockEntity);
         if (gameEventListener != null) {
            this.getListenerRegistry(SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY())).register(gameEventListener);
         }
      }
   }

   private <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity) {
      BlockState blockState = blockEntity.getBlockState();
      BlockEntityTicker<T> blockEntityTicker = blockState.getTicker(this.level, (BlockEntityType<T>)blockEntity.getType());
      if (blockEntityTicker == null) {
         this.removeBlockEntityTicker(blockEntity.getBlockPos());
      } else {
         this.tickersInLevel
            .compute(
               blockEntity.getBlockPos(),
               (blockPos, rebindableTickingBlockEntityWrapper) -> {
                  TickingBlockEntity tickingBlockEntity = this.createTicker(blockEntity, blockEntityTicker);
                  if (rebindableTickingBlockEntityWrapper != null) {
                     rebindableTickingBlockEntityWrapper.rebind(tickingBlockEntity);
                     return (LevelChunk.RebindableTickingBlockEntityWrapper)rebindableTickingBlockEntityWrapper;
                  } else if (this.isInLevel()) {
                     LevelChunk.RebindableTickingBlockEntityWrapper rebindableTickingBlockEntityWrapper2 = new LevelChunk.RebindableTickingBlockEntityWrapper(
                        tickingBlockEntity
                     );
                     this.level.addBlockEntityTicker(rebindableTickingBlockEntityWrapper2);
                     return rebindableTickingBlockEntityWrapper2;
                  } else {
                     return null;
                  }
               }
            );
      }
   }

   private <T extends BlockEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> ticker) {
      return new LevelChunk.BoundTickingBlockEntity<>(blockEntity, ticker);
   }

   class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
      private final T blockEntity;
      private final BlockEntityTicker<T> ticker;
      private boolean loggedInvalidBlockState;

      BoundTickingBlockEntity(final T blockEntity, final BlockEntityTicker<T> ticker) {
         this.blockEntity = blockEntity;
         this.ticker = ticker;
      }

      @Override
      public void tick() {
         if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
            BlockPos blockPos = this.blockEntity.getBlockPos();
            if (LevelChunk.this.isTicking(blockPos)) {
               try {
                  ProfilerFiller profilerFiller = Profiler.get();
                  profilerFiller.push(this::getType);
                  BlockState blockState = LevelChunk.this.getBlockState(blockPos);
                  if (this.blockEntity.getType().isValid(blockState)) {
                     this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockState, this.blockEntity);
                     this.loggedInvalidBlockState = false;
                  } else if (!this.loggedInvalidBlockState) {
                     this.loggedInvalidBlockState = true;
                     LevelChunk.LOGGER
                        .warn(
                           "Block entity {} @ {} state {} invalid for ticking:",
                           new Object[]{LogUtils.defer(this::getType), LogUtils.defer(this::getPos), blockState}
                        );
                  }

                  profilerFiller.pop();
               } catch (Throwable var5) {
                  CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking block entity");
                  CrashReportCategory crashReportCategory = crashReport.addCategory("Block entity being ticked");
                  this.blockEntity.fillCrashReportCategory(crashReportCategory);
                  throw new ReportedException(crashReport);
               }
            }
         }
      }

      @Override
      public boolean isRemoved() {
         return this.blockEntity.isRemoved();
      }

      @Override
      public BlockPos getPos() {
         return this.blockEntity.getBlockPos();
      }

      @Override
      public String getType() {
         return BlockEntityType.getKey(this.blockEntity.getType()).toString();
      }

      @Override
      public String toString() {
         return "Level ticker for " + this.getType() + "@" + this.getPos();
      }
   }

   public static enum EntityCreationType {
      IMMEDIATE,
      QUEUED,
      CHECK;
   }

   @FunctionalInterface
   public interface PostLoadProcessor {
      void run(LevelChunk levelChunk);
   }

   static class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
      private TickingBlockEntity ticker;

      RebindableTickingBlockEntityWrapper(TickingBlockEntity ticker) {
         this.ticker = ticker;
      }

      void rebind(TickingBlockEntity ticker) {
         this.ticker = ticker;
      }

      @Override
      public void tick() {
         this.ticker.tick();
      }

      @Override
      public boolean isRemoved() {
         return this.ticker.isRemoved();
      }

      @Override
      public BlockPos getPos() {
         return this.ticker.getPos();
      }

      @Override
      public String getType() {
         return this.ticker.getType();
      }

      @Override
      public String toString() {
         return this.ticker + " <wrapped>";
      }
   }

   @FunctionalInterface
   public interface UnsavedListener {
      void setUnsaved(ChunkPos chunkPos);
   }
}
