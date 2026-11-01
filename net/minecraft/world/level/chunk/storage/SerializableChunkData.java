package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record SerializableChunkData(
   PalettedContainerFactory containerFactory,
   ChunkPos chunkPos,
   int minSectionY,
   long lastUpdateTime,
   long inhabitedTime,
   ChunkStatus chunkStatus,
   @Nullable BlendingData.Packed blendingData,
   @Nullable BelowZeroRetrogen belowZeroRetrogen,
   UpgradeData upgradeData,
   @Nullable long[] carvingMask,
   Map<Heightmap.Types, long[]> heightmaps,
   ChunkAccess.PackedTicks packedTicks,
   ShortList[] postProcessingSections,
   boolean lightCorrect,
   List<SerializableChunkData.SectionData> sectionData,
   List<CompoundTag> entities,
   List<CompoundTag> blockEntities,
   CompoundTag structureData
) {
   private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
   private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_UPGRADE_DATA = "UpgradeData";
   private static final String BLOCK_TICKS_TAG = "block_ticks";
   private static final String FLUID_TICKS_TAG = "fluid_ticks";
   public static final String X_POS_TAG = "xPos";
   public static final String Z_POS_TAG = "zPos";
   public static final String HEIGHTMAPS_TAG = "Heightmaps";
   public static final String IS_LIGHT_ON_TAG = "isLightOn";
   public static final String SECTIONS_TAG = "sections";
   public static final String BLOCK_LIGHT_TAG = "BlockLight";
   public static final String SKY_LIGHT_TAG = "SkyLight";

   public static SerializableChunkData parse(LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory palettedContainerFactory, CompoundTag tag) {
      if (tag.getString("Status").isEmpty()) {
         return null;
      } else {
         ChunkPos chunkPos = new ChunkPos(tag.getIntOr("xPos", 0), tag.getIntOr("zPos", 0));
         long l = tag.getLongOr("LastUpdate", 0L);
         long m = tag.getLongOr("InhabitedTime", 0L);
         ChunkStatus chunkStatus = tag.<ChunkStatus>read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
         UpgradeData upgradeData = tag.getCompound("UpgradeData")
            .map(compoundTagx -> new UpgradeData(compoundTagx, levelHeightAccessor))
            .orElse(UpgradeData.EMPTY);
         boolean bl = tag.getBooleanOr("isLightOn", false);
         BlendingData.Packed packed = tag.<BlendingData.Packed>read("blending_data", BlendingData.Packed.CODEC).orElse(null);
         BelowZeroRetrogen belowZeroRetrogen = tag.<BelowZeroRetrogen>read("below_zero_retrogen", BelowZeroRetrogen.CODEC).orElse(null);
         long[] ls = tag.getLongArray("carving_mask").orElse(null);
         Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);
         tag.getCompound("Heightmaps").ifPresent(compoundTagx -> {
            for (Heightmap.Types types : chunkStatus.heightmapsAfter()) {
               compoundTagx.getLongArray(types.getSerializationKey()).ifPresent(lsx -> map.put(types, lsx));
            }
         });
         List<SavedTick<Block>> list = SavedTick.filterTickListForChunk(
            tag.<List<SavedTick<Block>>>read("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()), chunkPos
         );
         List<SavedTick<Fluid>> list2 = SavedTick.filterTickListForChunk(
            tag.<List<SavedTick<Fluid>>>read("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()), chunkPos
         );
         ChunkAccess.PackedTicks packedTicks = new ChunkAccess.PackedTicks(list, list2);
         ListTag listTag = tag.getListOrEmpty("PostProcessing");
         ShortList[] shortLists = new ShortList[listTag.size()];

         for (int i = 0; i < listTag.size(); i++) {
            ListTag listTag2 = listTag.getList(i).orElse(null);
            if (listTag2 != null && !listTag2.isEmpty()) {
               ShortList shortList = new ShortArrayList(listTag2.size());

               for (int j = 0; j < listTag2.size(); j++) {
                  shortList.add(listTag2.getShortOr(j, (short)0));
               }

               shortLists[i] = shortList;
            }
         }

         List<CompoundTag> list3 = tag.getList("entities").stream().flatMap(ListTag::compoundStream).toList();
         List<CompoundTag> list4 = tag.getList("block_entities").stream().flatMap(ListTag::compoundStream).toList();
         CompoundTag compoundTag = tag.getCompoundOrEmpty("structures");
         ListTag listTag3 = tag.getListOrEmpty("sections");
         List<SerializableChunkData.SectionData> list5 = new ArrayList<>(listTag3.size());
         Codec<PalettedContainerRO<Holder<Biome>>> codec = palettedContainerFactory.biomeContainerCodec();
         Codec<PalettedContainer<BlockState>> codec2 = palettedContainerFactory.blockStatesContainerCodec();

         for (int k = 0; k < listTag3.size(); k++) {
            Optional<CompoundTag> optional = listTag3.getCompound(k);
            if (!optional.isEmpty()) {
               CompoundTag compoundTag2 = optional.get();
               int n = compoundTag2.getByteOr("Y", (byte)0);
               LevelChunkSection levelChunkSection;
               if (n >= levelHeightAccessor.getMinSectionY() && n <= levelHeightAccessor.getMaxSectionY()) {
                  PalettedContainer<BlockState> palettedContainer = compoundTag2.getCompound("block_states")
                     .map(
                        compoundTagx -> (PalettedContainer<BlockState>)codec2.parse(NbtOps.INSTANCE, compoundTagx)
                           .promotePartial(string -> logErrors(chunkPos, n, string))
                           .getOrThrow(SerializableChunkData.ChunkReadException::new)
                     )
                     .orElseGet(palettedContainerFactory::createForBlockStates);
                  PalettedContainerRO<Holder<Biome>> palettedContainerRO = compoundTag2.getCompound("biomes")
                     .map(
                        compoundTagx -> (PalettedContainerRO<Holder<Biome>>)codec.parse(NbtOps.INSTANCE, compoundTagx)
                           .promotePartial(string -> logErrors(chunkPos, n, string))
                           .getOrThrow(SerializableChunkData.ChunkReadException::new)
                     )
                     .orElseGet(palettedContainerFactory::createForBiomes);
                  levelChunkSection = new LevelChunkSection(palettedContainer, palettedContainerRO);
               } else {
                  levelChunkSection = null;
               }

               DataLayer dataLayer = compoundTag2.getByteArray("BlockLight").map(DataLayer::new).orElse(null);
               DataLayer dataLayer2 = compoundTag2.getByteArray("SkyLight").map(DataLayer::new).orElse(null);
               list5.add(new SerializableChunkData.SectionData(n, levelChunkSection, dataLayer, dataLayer2));
            }
         }

         return new SerializableChunkData(
            palettedContainerFactory,
            chunkPos,
            levelHeightAccessor.getMinSectionY(),
            l,
            m,
            chunkStatus,
            packed,
            belowZeroRetrogen,
            upgradeData,
            ls,
            map,
            packedTicks,
            shortLists,
            bl,
            list5,
            list3,
            list4,
            compoundTag
         );
      }
   }

   public ProtoChunk read(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos) {
      if (!Objects.equals(pos, this.chunkPos)) {
         LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", new Object[]{pos, pos, this.chunkPos});
         level.getServer().reportMisplacedChunk(this.chunkPos, pos, regionStorageInfo);
      }

      int i = level.getSectionsCount();
      LevelChunkSection[] levelChunkSections = new LevelChunkSection[i];
      boolean bl = level.dimensionType().hasSkyLight();
      ChunkSource chunkSource = level.getChunkSource();
      LevelLightEngine levelLightEngine = chunkSource.getLightEngine();
      PalettedContainerFactory palettedContainerFactory = level.palettedContainerFactory();
      boolean bl2 = false;

      for (SerializableChunkData.SectionData sectionData : this.sectionData) {
         SectionPos sectionPos = SectionPos.of(pos, sectionData.y);
         if (sectionData.chunkSection != null) {
            levelChunkSections[level.getSectionIndexFromSectionY(sectionData.y)] = sectionData.chunkSection;
            poiManager.checkConsistencyWithBlocks(sectionPos, sectionData.chunkSection);
         }

         boolean bl3 = sectionData.blockLight != null;
         boolean bl4 = bl && sectionData.skyLight != null;
         if (bl3 || bl4) {
            if (!bl2) {
               levelLightEngine.retainData(pos, true);
               bl2 = true;
            }

            if (bl3) {
               levelLightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, sectionData.blockLight);
            }

            if (bl4) {
               levelLightEngine.queueSectionData(LightLayer.SKY, sectionPos, sectionData.skyLight);
            }
         }
      }

      ChunkType chunkType = this.chunkStatus.getChunkType();
      ChunkAccess chunkAccess;
      if (chunkType == ChunkType.LEVELCHUNK) {
         LevelChunkTicks<Block> levelChunkTicks = new LevelChunkTicks<>(this.packedTicks.blocks());
         LevelChunkTicks<Fluid> levelChunkTicks2 = new LevelChunkTicks<>(this.packedTicks.fluids());
         chunkAccess = new LevelChunk(
            level.getLevel(),
            pos,
            this.upgradeData,
            levelChunkTicks,
            levelChunkTicks2,
            this.inhabitedTime,
            levelChunkSections,
            postLoadChunk(level, this.entities, this.blockEntities),
            BlendingData.unpack(this.blendingData)
         );
      } else {
         ProtoChunkTicks<Block> protoChunkTicks = ProtoChunkTicks.load(this.packedTicks.blocks());
         ProtoChunkTicks<Fluid> protoChunkTicks2 = ProtoChunkTicks.load(this.packedTicks.fluids());
         ProtoChunk protoChunk = new ProtoChunk(
            pos,
            this.upgradeData,
            levelChunkSections,
            protoChunkTicks,
            protoChunkTicks2,
            level,
            palettedContainerFactory,
            BlendingData.unpack(this.blendingData)
         );
         chunkAccess = protoChunk;
         protoChunk.setInhabitedTime(this.inhabitedTime);
         if (this.belowZeroRetrogen != null) {
            protoChunk.setBelowZeroRetrogen(this.belowZeroRetrogen);
         }

         protoChunk.setPersistedStatus(this.chunkStatus);
         if (this.chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
            protoChunk.setLightEngine(levelLightEngine);
         }
      }

      chunkAccess.setLightCorrect(this.lightCorrect);
      EnumSet<Heightmap.Types> enumSet = EnumSet.noneOf(Heightmap.Types.class);

      for (Heightmap.Types types : chunkAccess.getPersistedStatus().heightmapsAfter()) {
         long[] ls = this.heightmaps.get(types);
         if (ls != null) {
            chunkAccess.setHeightmap(types, ls);
         } else {
            enumSet.add(types);
         }
      }

      Heightmap.primeHeightmaps(chunkAccess, enumSet);
      chunkAccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(level), this.structureData, level.getSeed()));
      chunkAccess.setAllReferences(unpackStructureReferences(level.registryAccess(), pos, this.structureData));

      for (int j = 0; j < this.postProcessingSections.length; j++) {
         ShortList shortList = this.postProcessingSections[j];
         if (shortList != null) {
            chunkAccess.addPackedPostProcess(shortList, j);
         }
      }

      if (chunkType == ChunkType.LEVELCHUNK) {
         return new ImposterProtoChunk((LevelChunk)chunkAccess, false);
      } else {
         ProtoChunk protoChunk2 = (ProtoChunk)chunkAccess;

         for (CompoundTag compoundTag : this.entities) {
            protoChunk2.addEntity(compoundTag);
         }

         for (CompoundTag compoundTag : this.blockEntities) {
            protoChunk2.setBlockEntityNbt(compoundTag);
         }

         if (this.carvingMask != null) {
            protoChunk2.setCarvingMask(new CarvingMask(this.carvingMask, chunkAccess.getMinY()));
         }

         return protoChunk2;
      }
   }

   private static void logErrors(ChunkPos chunkPos, int sectionY, String error) {
      LOGGER.error("Recoverable errors when loading section [{}, {}, {}]: {}", new Object[]{chunkPos.x, sectionY, chunkPos.z, error});
   }

   public static SerializableChunkData copyOf(ServerLevel level, ChunkAccess chunk) {
      if (!chunk.canBeSerialized()) {
         throw new IllegalArgumentException("Chunk can't be serialized: " + chunk);
      } else {
         ChunkPos chunkPos = chunk.getPos();
         List<SerializableChunkData.SectionData> list = new ArrayList<>();
         LevelChunkSection[] levelChunkSections = chunk.getSections();
         LevelLightEngine levelLightEngine = level.getChunkSource().getLightEngine();

         for (int i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); i++) {
            int j = chunk.getSectionIndexFromSectionY(i);
            boolean bl = j >= 0 && j < levelChunkSections.length;
            DataLayer dataLayer = levelLightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, i));
            DataLayer dataLayer2 = levelLightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, i));
            DataLayer dataLayer3 = dataLayer != null && !dataLayer.isEmpty() ? dataLayer.copy() : null;
            DataLayer dataLayer4 = dataLayer2 != null && !dataLayer2.isEmpty() ? dataLayer2.copy() : null;
            if (bl || dataLayer3 != null || dataLayer4 != null) {
               LevelChunkSection levelChunkSection = bl ? levelChunkSections[j].copy() : null;
               list.add(new SerializableChunkData.SectionData(i, levelChunkSection, dataLayer3, dataLayer4));
            }
         }

         List<CompoundTag> list2 = new ArrayList<>(chunk.getBlockEntitiesPos().size());

         for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            CompoundTag compoundTag = chunk.getBlockEntityNbtForSaving(blockPos, level.registryAccess());
            if (compoundTag != null) {
               list2.add(compoundTag);
            }
         }

         List<CompoundTag> list3 = new ArrayList<>();
         long[] ls = null;
         if (chunk.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            list3.addAll(protoChunk.getEntities());
            CarvingMask carvingMask = protoChunk.getCarvingMask();
            if (carvingMask != null) {
               ls = carvingMask.toArray();
            }
         }

         Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);

         for (Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
               long[] ms = entry.getValue().getRawData();
               map.put(entry.getKey(), (long[])ms.clone());
            }
         }

         ChunkAccess.PackedTicks packedTicks = chunk.getTicksForSerialization(level.getGameTime());
         ShortList[] shortLists = Arrays.stream(chunk.getPostProcessing())
            .map(shortList -> shortList != null && !shortList.isEmpty() ? new ShortArrayList(shortList) : null)
            .toArray(ShortList[]::new);
         CompoundTag compoundTag2 = packStructureData(
            StructurePieceSerializationContext.fromLevel(level), chunkPos, chunk.getAllStarts(), chunk.getAllReferences()
         );
         return new SerializableChunkData(
            level.palettedContainerFactory(),
            chunkPos,
            chunk.getMinSectionY(),
            level.getGameTime(),
            chunk.getInhabitedTime(),
            chunk.getPersistedStatus(),
            Optionull.map(chunk.getBlendingData(), BlendingData::pack),
            chunk.getBelowZeroRetrogen(),
            chunk.getUpgradeData().copy(),
            ls,
            map,
            packedTicks,
            shortLists,
            chunk.isLightCorrect(),
            list,
            list3,
            list2,
            compoundTag2
         );
      }
   }

   public CompoundTag write() {
      CompoundTag compoundTag = NbtUtils.addCurrentDataVersion(new CompoundTag());
      compoundTag.putInt("xPos", this.chunkPos.x);
      compoundTag.putInt("yPos", this.minSectionY);
      compoundTag.putInt("zPos", this.chunkPos.z);
      compoundTag.putLong("LastUpdate", this.lastUpdateTime);
      compoundTag.putLong("InhabitedTime", this.inhabitedTime);
      compoundTag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(this.chunkStatus).toString());
      compoundTag.storeNullable("blending_data", BlendingData.Packed.CODEC, this.blendingData);
      compoundTag.storeNullable("below_zero_retrogen", BelowZeroRetrogen.CODEC, this.belowZeroRetrogen);
      if (!this.upgradeData.isEmpty()) {
         compoundTag.put("UpgradeData", this.upgradeData.write());
      }

      ListTag listTag = new ListTag();
      Codec<PalettedContainer<BlockState>> codec = this.containerFactory.blockStatesContainerCodec();
      Codec<PalettedContainerRO<Holder<Biome>>> codec2 = this.containerFactory.biomeContainerCodec();

      for (SerializableChunkData.SectionData sectionData : this.sectionData) {
         CompoundTag compoundTag2 = new CompoundTag();
         LevelChunkSection levelChunkSection = sectionData.chunkSection;
         if (levelChunkSection != null) {
            compoundTag2.store("block_states", codec, levelChunkSection.getStates());
            compoundTag2.store("biomes", codec2, levelChunkSection.getBiomes());
         }

         if (sectionData.blockLight != null) {
            compoundTag2.putByteArray("BlockLight", sectionData.blockLight.getData());
         }

         if (sectionData.skyLight != null) {
            compoundTag2.putByteArray("SkyLight", sectionData.skyLight.getData());
         }

         if (!compoundTag2.isEmpty()) {
            compoundTag2.putByte("Y", (byte)sectionData.y);
            listTag.add(compoundTag2);
         }
      }

      compoundTag.put("sections", listTag);
      if (this.lightCorrect) {
         compoundTag.putBoolean("isLightOn", true);
      }

      ListTag listTag2 = new ListTag();
      listTag2.addAll(this.blockEntities);
      compoundTag.put("block_entities", listTag2);
      if (this.chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
         ListTag listTag3 = new ListTag();
         listTag3.addAll(this.entities);
         compoundTag.put("entities", listTag3);
         if (this.carvingMask != null) {
            compoundTag.putLongArray("carving_mask", this.carvingMask);
         }
      }

      saveTicks(compoundTag, this.packedTicks);
      compoundTag.put("PostProcessing", packOffsets(this.postProcessingSections));
      CompoundTag compoundTag3 = new CompoundTag();
      this.heightmaps.forEach((types, ls) -> compoundTag3.put(types.getSerializationKey(), new LongArrayTag(ls)));
      compoundTag.put("Heightmaps", compoundTag3);
      compoundTag.put("structures", this.structureData);
      return compoundTag;
   }

   private static void saveTicks(CompoundTag tag, ChunkAccess.PackedTicks ticks) {
      tag.store("block_ticks", BLOCK_TICKS_CODEC, ticks.blocks());
      tag.store("fluid_ticks", FLUID_TICKS_CODEC, ticks.fluids());
   }

   public static ChunkStatus getChunkStatusFromTag(@Nullable CompoundTag tag) {
      return tag != null ? tag.<ChunkStatus>read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY) : ChunkStatus.EMPTY;
   }

   @Nullable
   private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel level, List<CompoundTag> entities, List<CompoundTag> blockEntities) {
      return entities.isEmpty() && blockEntities.isEmpty()
         ? null
         : levelChunk -> {
            if (!entities.isEmpty()) {
               try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(levelChunk.problemPath(), LOGGER)) {
                  level.addLegacyChunkEntities(
                     EntityType.loadEntitiesRecursive(TagValueInput.create(scopedCollector, level.registryAccess(), entities), level, EntitySpawnReason.LOAD)
                  );
               }
            }

            for (CompoundTag compoundTag : blockEntities) {
               boolean bl = compoundTag.getBooleanOr("keepPacked", false);
               if (bl) {
                  levelChunk.setBlockEntityNbt(compoundTag);
               } else {
                  BlockPos blockPos = BlockEntity.getPosFromTag(levelChunk.getPos(), compoundTag);
                  BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, levelChunk.getBlockState(blockPos), compoundTag, level.registryAccess());
                  if (blockEntity != null) {
                     levelChunk.setBlockEntity(blockEntity);
                  }
               }
            }
         };
   }

   private static CompoundTag packStructureData(
      StructurePieceSerializationContext context, ChunkPos pos, Map<Structure, StructureStart> structureStarts, Map<Structure, LongSet> references
   ) {
      CompoundTag compoundTag = new CompoundTag();
      CompoundTag compoundTag2 = new CompoundTag();
      Registry<Structure> registry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);

      for (Entry<Structure, StructureStart> entry : structureStarts.entrySet()) {
         Identifier identifier = registry.getKey(entry.getKey());
         compoundTag2.put(identifier.toString(), entry.getValue().createTag(context, pos));
      }

      compoundTag.put("starts", compoundTag2);
      CompoundTag compoundTag3 = new CompoundTag();

      for (Entry<Structure, LongSet> entry2 : references.entrySet()) {
         if (!entry2.getValue().isEmpty()) {
            Identifier identifier2 = registry.getKey(entry2.getKey());
            compoundTag3.putLongArray(identifier2.toString(), entry2.getValue().toLongArray());
         }
      }

      compoundTag.put("References", compoundTag3);
      return compoundTag;
   }

   private static Map<Structure, StructureStart> unpackStructureStart(StructurePieceSerializationContext context, CompoundTag tag, long seed) {
      Map<Structure, StructureStart> map = Maps.newHashMap();
      Registry<Structure> registry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      CompoundTag compoundTag = tag.getCompoundOrEmpty("starts");

      for (String string : compoundTag.keySet()) {
         Identifier identifier = Identifier.tryParse(string);
         Structure structure = registry.getValue(identifier);
         if (structure == null) {
            LOGGER.error("Unknown structure start: {}", identifier);
         } else {
            StructureStart structureStart = StructureStart.loadStaticStart(context, compoundTag.getCompoundOrEmpty(string), seed);
            if (structureStart != null) {
               map.put(structure, structureStart);
            }
         }
      }

      return map;
   }

   private static Map<Structure, LongSet> unpackStructureReferences(RegistryAccess registries, ChunkPos pos, CompoundTag tag) {
      Map<Structure, LongSet> map = Maps.newHashMap();
      Registry<Structure> registry = registries.lookupOrThrow(Registries.STRUCTURE);
      CompoundTag compoundTag = tag.getCompoundOrEmpty("References");
      compoundTag.forEach((string, tagx) -> {
         Identifier identifier = Identifier.tryParse(string);
         Structure structure = registry.getValue(identifier);
         if (structure == null) {
            LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", identifier, pos);
         } else {
            Optional<long[]> optional = tagx.asLongArray();
            if (!optional.isEmpty()) {
               map.put(structure, new LongOpenHashSet(Arrays.stream(optional.get()).filter(l -> {
                  ChunkPos chunkPos2 = new ChunkPos(l);
                  if (chunkPos2.getChessboardDistance(pos) > 8) {
                     LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", new Object[]{identifier, chunkPos2, pos});
                     return false;
                  } else {
                     return true;
                  }
               }).toArray()));
            }
         }
      });
      return map;
   }

   private static ListTag packOffsets(ShortList[] offsets) {
      ListTag listTag = new ListTag();

      for (ShortList shortList : offsets) {
         ListTag listTag2 = new ListTag();
         if (shortList != null) {
            for (int i = 0; i < shortList.size(); i++) {
               listTag2.add(ShortTag.valueOf(shortList.getShort(i)));
            }
         }

         listTag.add(listTag2);
      }

      return listTag;
   }

   public static class ChunkReadException extends NbtException {
      public ChunkReadException(String string) {
         super(string);
      }
   }

   public record SectionData(int y, @Nullable LevelChunkSection chunkSection, @Nullable DataLayer blockLight, @Nullable DataLayer skyLight) {
   }
}
