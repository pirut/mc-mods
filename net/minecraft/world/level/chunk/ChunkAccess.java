package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ChunkAccess implements BiomeManager.NoiseBiomeSource, LightChunk, StructureAccess {
   public static final int NO_FILLED_SECTION = -1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LongSet EMPTY_REFERENCE_SET = new LongOpenHashSet();
   protected final ShortList[] postProcessing;
   private volatile boolean unsaved;
   private volatile boolean isLightCorrect;
   protected final ChunkPos chunkPos;
   private long inhabitedTime;
   @Deprecated
   @Nullable
   private BiomeGenerationSettings carverBiomeSettings;
   @Nullable
   protected NoiseChunk noiseChunk;
   protected final UpgradeData upgradeData;
   @Nullable
   protected BlendingData blendingData;
   protected final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
   protected ChunkSkyLightSources skyLightSources;
   private final Map<Structure, StructureStart> structureStarts = Maps.newHashMap();
   private final Map<Structure, LongSet> structuresRefences = Maps.newHashMap();
   protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
   protected final Map<BlockPos, BlockEntity> blockEntities = new Object2ObjectOpenHashMap();
   protected final LevelHeightAccessor levelHeightAccessor;
   protected final LevelChunkSection[] sections;

   public ChunkAccess(
      ChunkPos chunkPos,
      UpgradeData upgradeData,
      LevelHeightAccessor levelHeightAccessor,
      PalettedContainerFactory palettedContainerFactory,
      long inhabitedTime,
      @Nullable LevelChunkSection[] sections,
      @Nullable BlendingData blendingData
   ) {
      this.chunkPos = chunkPos;
      this.upgradeData = upgradeData;
      this.levelHeightAccessor = levelHeightAccessor;
      this.sections = new LevelChunkSection[levelHeightAccessor.getSectionsCount()];
      this.inhabitedTime = inhabitedTime;
      this.postProcessing = new ShortList[levelHeightAccessor.getSectionsCount()];
      this.blendingData = blendingData;
      this.skyLightSources = new ChunkSkyLightSources(levelHeightAccessor);
      if (sections != null) {
         if (this.sections.length == sections.length) {
            System.arraycopy(sections, 0, this.sections, 0, this.sections.length);
         } else {
            LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sections.length, this.sections.length);
         }
      }

      replaceMissingSections(palettedContainerFactory, this.sections);
   }

   private static void replaceMissingSections(PalettedContainerFactory palettedContainerFactory, LevelChunkSection[] sections) {
      for (int i = 0; i < sections.length; i++) {
         if (sections[i] == null) {
            sections[i] = new LevelChunkSection(palettedContainerFactory);
         }
      }
   }

   public GameEventListenerRegistry getListenerRegistry(int sectionY) {
      return GameEventListenerRegistry.NOOP;
   }

   @Nullable
   public BlockState setBlockState(BlockPos pos, BlockState state) {
      return this.setBlockState(pos, state, 3);
   }

   @Nullable
   public abstract BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags);

   public abstract void setBlockEntity(BlockEntity blockEntity);

   public abstract void addEntity(Entity entity);

   public int getHighestFilledSectionIndex() {
      LevelChunkSection[] levelChunkSections = this.getSections();

      for (int i = levelChunkSections.length - 1; i >= 0; i--) {
         LevelChunkSection levelChunkSection = levelChunkSections[i];
         if (!levelChunkSection.hasOnlyAir()) {
            return i;
         }
      }

      return -1;
   }

   @Deprecated(
      forRemoval = true
   )
   public int getHighestSectionPosition() {
      int i = this.getHighestFilledSectionIndex();
      return i == -1 ? this.getMinY() : SectionPos.sectionToBlockCoord(this.getSectionYFromSectionIndex(i));
   }

   public Set<BlockPos> getBlockEntitiesPos() {
      Set<BlockPos> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
      set.addAll(this.blockEntities.keySet());
      return set;
   }

   public LevelChunkSection[] getSections() {
      return this.sections;
   }

   public LevelChunkSection getSection(int index) {
      return this.getSections()[index];
   }

   public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
      return Collections.unmodifiableSet(this.heightmaps.entrySet());
   }

   public void setHeightmap(Heightmap.Types type, long[] data) {
      this.getOrCreateHeightmapUnprimed(type).setRawData(this, type, data);
   }

   public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
      return this.heightmaps.computeIfAbsent(type, types -> new Heightmap(this, types));
   }

   public boolean hasPrimedHeightmap(Heightmap.Types type) {
      return this.heightmaps.get(type) != null;
   }

   public int getHeight(Heightmap.Types type, int x, int z) {
      Heightmap heightmap = this.heightmaps.get(type);
      if (heightmap == null) {
         if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
            LOGGER.error("Unprimed heightmap: {} {} {}", new Object[]{type, x, z});
         }

         Heightmap.primeHeightmaps(this, EnumSet.of(type));
         heightmap = this.heightmaps.get(type);
      }

      return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
   }

   public ChunkPos getPos() {
      return this.chunkPos;
   }

   @Nullable
   @Override
   public StructureStart getStartForStructure(Structure structure) {
      return this.structureStarts.get(structure);
   }

   @Override
   public void setStartForStructure(Structure structure, StructureStart structureStart) {
      this.structureStarts.put(structure, structureStart);
      this.markUnsaved();
   }

   public Map<Structure, StructureStart> getAllStarts() {
      return Collections.unmodifiableMap(this.structureStarts);
   }

   public void setAllStarts(Map<Structure, StructureStart> structureStarts) {
      this.structureStarts.clear();
      this.structureStarts.putAll(structureStarts);
      this.markUnsaved();
   }

   @Override
   public LongSet getReferencesForStructure(Structure structure) {
      return this.structuresRefences.getOrDefault(structure, EMPTY_REFERENCE_SET);
   }

   @Override
   public void addReferenceForStructure(Structure structure, long reference) {
      this.structuresRefences.computeIfAbsent(structure, structurex -> new LongOpenHashSet()).add(reference);
      this.markUnsaved();
   }

   @Override
   public Map<Structure, LongSet> getAllReferences() {
      return Collections.unmodifiableMap(this.structuresRefences);
   }

   @Override
   public void setAllReferences(Map<Structure, LongSet> structureReferencesMap) {
      this.structuresRefences.clear();
      this.structuresRefences.putAll(structureReferencesMap);
      this.markUnsaved();
   }

   public boolean isYSpaceEmpty(int startY, int endY) {
      if (startY < this.getMinY()) {
         startY = this.getMinY();
      }

      if (endY > this.getMaxY()) {
         endY = this.getMaxY();
      }

      for (int i = startY; i <= endY; i += 16) {
         if (!this.getSection(this.getSectionIndex(i)).hasOnlyAir()) {
            return false;
         }
      }

      return true;
   }

   public void markUnsaved() {
      this.unsaved = true;
   }

   public boolean tryMarkSaved() {
      if (this.unsaved) {
         this.unsaved = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean isUnsaved() {
      return this.unsaved;
   }

   public abstract ChunkStatus getPersistedStatus();

   public ChunkStatus getHighestGeneratedStatus() {
      ChunkStatus chunkStatus = this.getPersistedStatus();
      BelowZeroRetrogen belowZeroRetrogen = this.getBelowZeroRetrogen();
      if (belowZeroRetrogen != null) {
         ChunkStatus chunkStatus2 = belowZeroRetrogen.targetStatus();
         return ChunkStatus.max(chunkStatus2, chunkStatus);
      } else {
         return chunkStatus;
      }
   }

   public abstract void removeBlockEntity(BlockPos pos);

   public void markPosForPostprocessing(BlockPos pos) {
      LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", pos);
   }

   public ShortList[] getPostProcessing() {
      return this.postProcessing;
   }

   public void addPackedPostProcess(ShortList offsets, int index) {
      getOrCreateOffsetList(this.getPostProcessing(), index).addAll(offsets);
   }

   public void setBlockEntityNbt(CompoundTag tag) {
      BlockPos blockPos = BlockEntity.getPosFromTag(this.chunkPos, tag);
      if (!this.blockEntities.containsKey(blockPos)) {
         this.pendingBlockEntities.put(blockPos, tag);
      }
   }

   @Nullable
   public CompoundTag getBlockEntityNbt(BlockPos pos) {
      return this.pendingBlockEntities.get(pos);
   }

   @Nullable
   public abstract CompoundTag getBlockEntityNbtForSaving(BlockPos pos, HolderLookup.Provider registries);

   @Override
   public final void findBlockLightSources(BiConsumer<BlockPos, BlockState> output) {
      this.findBlocks(blockState -> blockState.getLightEmission() != 0, output);
   }

   public void findBlocks(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> output) {
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

      for (int i = this.getMinSectionY(); i <= this.getMaxSectionY(); i++) {
         LevelChunkSection levelChunkSection = this.getSection(this.getSectionIndexFromSectionY(i));
         if (levelChunkSection.maybeHas(predicate)) {
            BlockPos blockPos = SectionPos.of(this.chunkPos, i).origin();

            for (int j = 0; j < 16; j++) {
               for (int k = 0; k < 16; k++) {
                  for (int l = 0; l < 16; l++) {
                     BlockState blockState = levelChunkSection.getBlockState(l, j, k);
                     if (predicate.test(blockState)) {
                        output.accept(mutableBlockPos.setWithOffset(blockPos, l, j, k), blockState);
                     }
                  }
               }
            }
         }
      }
   }

   public abstract TickContainerAccess<Block> getBlockTicks();

   public abstract TickContainerAccess<Fluid> getFluidTicks();

   public boolean canBeSerialized() {
      return true;
   }

   public abstract ChunkAccess.PackedTicks getTicksForSerialization(long gametime);

   public UpgradeData getUpgradeData() {
      return this.upgradeData;
   }

   public boolean isOldNoiseGeneration() {
      return this.blendingData != null;
   }

   @Nullable
   public BlendingData getBlendingData() {
      return this.blendingData;
   }

   public long getInhabitedTime() {
      return this.inhabitedTime;
   }

   public void incrementInhabitedTime(long amount) {
      this.inhabitedTime += amount;
   }

   public void setInhabitedTime(long inhabitedTime) {
      this.inhabitedTime = inhabitedTime;
   }

   public static ShortList getOrCreateOffsetList(ShortList[] packedPositions, int index) {
      ShortList shortList = packedPositions[index];
      if (shortList == null) {
         shortList = new ShortArrayList();
         packedPositions[index] = shortList;
      }

      return shortList;
   }

   public boolean isLightCorrect() {
      return this.isLightCorrect;
   }

   public void setLightCorrect(boolean lightCorrect) {
      this.isLightCorrect = lightCorrect;
      this.markUnsaved();
   }

   @Override
   public int getMinY() {
      return this.levelHeightAccessor.getMinY();
   }

   @Override
   public int getHeight() {
      return this.levelHeightAccessor.getHeight();
   }

   public NoiseChunk getOrCreateNoiseChunk(Function<ChunkAccess, NoiseChunk> noiseChunkCreator) {
      if (this.noiseChunk == null) {
         this.noiseChunk = noiseChunkCreator.apply(this);
      }

      return this.noiseChunk;
   }

   @Deprecated
   public BiomeGenerationSettings carverBiome(Supplier<BiomeGenerationSettings> caverBiomeSettingsSupplier) {
      if (this.carverBiomeSettings == null) {
         this.carverBiomeSettings = caverBiomeSettingsSupplier.get();
      }

      return this.carverBiomeSettings;
   }

   @Override
   public Holder<Biome> getNoiseBiome(int i, int j, int k) {
      try {
         int l = QuartPos.fromBlock(this.getMinY());
         int m = l + QuartPos.fromBlock(this.getHeight()) - 1;
         int n = Mth.clamp(j, l, m);
         int o = this.getSectionIndex(QuartPos.toBlock(n));
         return this.sections[o].getNoiseBiome(i & 3, n & 3, k & 3);
      } catch (Throwable var8) {
         CrashReport crashReport = CrashReport.forThrowable(var8, "Getting biome");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Biome being got");
         crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
         throw new ReportedException(crashReport);
      }
   }

   public void fillBiomesFromNoise(BiomeResolver resolver, Climate.Sampler sampler) {
      ChunkPos chunkPos = this.getPos();
      int i = QuartPos.fromBlock(chunkPos.getMinBlockX());
      int j = QuartPos.fromBlock(chunkPos.getMinBlockZ());
      LevelHeightAccessor levelHeightAccessor = this.getHeightAccessorForGeneration();

      for (int k = levelHeightAccessor.getMinSectionY(); k <= levelHeightAccessor.getMaxSectionY(); k++) {
         LevelChunkSection levelChunkSection = this.getSection(this.getSectionIndexFromSectionY(k));
         int l = QuartPos.fromSection(k);
         levelChunkSection.fillBiomesFromNoise(resolver, sampler, i, l, j);
      }
   }

   public boolean hasAnyStructureReferences() {
      return !this.getAllReferences().isEmpty();
   }

   @Nullable
   public BelowZeroRetrogen getBelowZeroRetrogen() {
      return null;
   }

   public boolean isUpgrading() {
      return this.getBelowZeroRetrogen() != null;
   }

   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return this;
   }

   public void initializeLightSources() {
      this.skyLightSources.fillFrom(this);
   }

   @Override
   public ChunkSkyLightSources getSkyLightSources() {
      return this.skyLightSources;
   }

   public static ProblemReporter.PathElement problemPath(ChunkPos pos) {
      return new ChunkAccess.ChunkPathElement(pos);
   }

   public ProblemReporter.PathElement problemPath() {
      return problemPath(this.getPos());
   }

   record ChunkPathElement(ChunkPos pos) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return "chunk@" + this.pos;
      }
   }

   public record PackedTicks(List<SavedTick<Block>> blocks, List<SavedTick<Fluid>> fluids) {
   }
}
