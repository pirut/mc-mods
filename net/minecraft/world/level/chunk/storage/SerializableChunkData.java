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
   CompoundTag structureData,
   @Nullable CompoundTag attachmentData,
   @Nullable ListTag auxLightData
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

   @Deprecated
   SerializableChunkData(
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
      this(
         containerFactory,
         chunkPos,
         minSectionY,
         lastUpdateTime,
         inhabitedTime,
         chunkStatus,
         blendingData,
         belowZeroRetrogen,
         upgradeData,
         carvingMask,
         heightmaps,
         packedTicks,
         postProcessingSections,
         lightCorrect,
         sectionData,
         entities,
         blockEntities,
         structureData,
         null,
         null
      );
   }

   public static SerializableChunkData parse(LevelHeightAccessor p_361938_, PalettedContainerFactory p_447248_, CompoundTag p_362040_) {
      if (p_362040_.getString("Status").isEmpty()) {
         return null;
      } else {
         ChunkPos chunkpos = new ChunkPos(p_362040_.getIntOr("xPos", 0), p_362040_.getIntOr("zPos", 0));
         long i = p_362040_.getLongOr("LastUpdate", 0L);
         long j = p_362040_.getLongOr("InhabitedTime", 0L);
         ChunkStatus chunkstatus = p_362040_.<ChunkStatus>read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
         UpgradeData upgradedata = p_362040_.getCompound("UpgradeData").map(p_409546_ -> new UpgradeData(p_409546_, p_361938_)).orElse(UpgradeData.EMPTY);
         boolean flag = p_362040_.getBooleanOr("isLightOn", false);
         BlendingData.Packed blendingdata$packed = p_362040_.<BlendingData.Packed>read("blending_data", BlendingData.Packed.CODEC).orElse(null);
         BelowZeroRetrogen belowzeroretrogen = p_362040_.<BelowZeroRetrogen>read("below_zero_retrogen", BelowZeroRetrogen.CODEC).orElse(null);
         long[] along = p_362040_.getLongArray("carving_mask").orElse(null);
         Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);
         p_362040_.getCompound("Heightmaps").ifPresent(p_409536_ -> {
            for (Heightmap.Types heightmap$types : chunkstatus.heightmapsAfter()) {
               p_409536_.getLongArray(heightmap$types.getSerializationKey()).ifPresent(p_409544_ -> map.put(heightmap$types, p_409544_));
            }
         });
         List<SavedTick<Block>> list = SavedTick.filterTickListForChunk(
            p_362040_.<List<SavedTick<Block>>>read("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()), chunkpos
         );
         List<SavedTick<Fluid>> list1 = SavedTick.filterTickListForChunk(
            p_362040_.<List<SavedTick<Fluid>>>read("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()), chunkpos
         );
         ChunkAccess.PackedTicks chunkaccess$packedticks = new ChunkAccess.PackedTicks(list, list1);
         ListTag listtag = p_362040_.getListOrEmpty("PostProcessing");
         ShortList[] ashortlist = new ShortList[listtag.size()];

         for (int k = 0; k < listtag.size(); k++) {
            ListTag listtag1 = listtag.getList(k).orElse(null);
            if (listtag1 != null && !listtag1.isEmpty()) {
               ShortList shortlist = new ShortArrayList(listtag1.size());

               for (int l = 0; l < listtag1.size(); l++) {
                  shortlist.add(listtag1.getShortOr(l, (short)0));
               }

               ashortlist[k] = shortlist;
            }
         }

         List<CompoundTag> list3 = p_362040_.getList("entities").stream().flatMap(ListTag::compoundStream).toList();
         List<CompoundTag> list4 = p_362040_.getList("block_entities").stream().flatMap(ListTag::compoundStream).toList();
         CompoundTag compoundtag1 = p_362040_.getCompoundOrEmpty("structures");
         ListTag listtag2 = p_362040_.getListOrEmpty("sections");
         List<SerializableChunkData.SectionData> list2 = new ArrayList<>(listtag2.size());
         Codec<PalettedContainerRO<Holder<Biome>>> codec = p_447248_.biomeContainerCodec();
         Codec<PalettedContainer<BlockState>> codec1 = p_447248_.blockStatesContainerCodec();

         for (int i1 = 0; i1 < listtag2.size(); i1++) {
            Optional<CompoundTag> optional = listtag2.getCompound(i1);
            if (!optional.isEmpty()) {
               CompoundTag compoundtag = optional.get();
               int j1 = compoundtag.getByteOr("Y", (byte)0);
               LevelChunkSection levelchunksection;
               if (j1 >= p_361938_.getMinSectionY() && j1 <= p_361938_.getMaxSectionY()) {
                  PalettedContainer<BlockState> palettedcontainer = compoundtag.getCompound("block_states")
                     .map(
                        p_445364_ -> (PalettedContainer<BlockState>)codec1.parse(NbtOps.INSTANCE, p_445364_)
                           .promotePartial(p_361842_ -> logErrors(chunkpos, j1, p_361842_))
                           .getOrThrow(SerializableChunkData.ChunkReadException::new)
                     )
                     .orElseGet(p_447248_::createForBlockStates);
                  PalettedContainerRO<Holder<Biome>> palettedcontainerro = compoundtag.getCompound("biomes")
                     .map(
                        p_409533_ -> (PalettedContainerRO<Holder<Biome>>)codec.parse(NbtOps.INSTANCE, p_409533_)
                           .promotePartial(p_361282_ -> logErrors(chunkpos, j1, p_361282_))
                           .getOrThrow(SerializableChunkData.ChunkReadException::new)
                     )
                     .orElseGet(p_447248_::createForBiomes);
                  levelchunksection = new LevelChunkSection(palettedcontainer, palettedcontainerro);
               } else {
                  levelchunksection = null;
               }

               DataLayer datalayer = compoundtag.getByteArray("BlockLight").map(DataLayer::new).orElse(null);
               DataLayer datalayer1 = compoundtag.getByteArray("SkyLight").map(DataLayer::new).orElse(null);
               list2.add(new SerializableChunkData.SectionData(j1, levelchunksection, datalayer, datalayer1));
            }
         }

         CompoundTag attachmentData = p_362040_.getCompound("neoforge:attachments").orElse(null);
         ListTag auxLightData = p_362040_.getList("neoforge:aux_lights").orElse(null);
         return new SerializableChunkData(
            p_447248_,
            chunkpos,
            p_361938_.getMinSectionY(),
            i,
            j,
            chunkstatus,
            blendingdata$packed,
            belowzeroretrogen,
            upgradedata,
            along,
            map,
            chunkaccess$packedticks,
            ashortlist,
            flag,
            list2,
            list3,
            list4,
            compoundtag1,
            attachmentData,
            auxLightData
         );
      }
   }

   public ProtoChunk read(ServerLevel p_360452_, PoiManager p_364451_, RegionStorageInfo p_364971_, ChunkPos p_360628_) {
      if (!Objects.equals(p_360628_, this.chunkPos)) {
         LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", new Object[]{p_360628_, p_360628_, this.chunkPos});
         p_360452_.getServer().reportMisplacedChunk(this.chunkPos, p_360628_, p_364971_);
      }

      int i = p_360452_.getSectionsCount();
      LevelChunkSection[] alevelchunksection = new LevelChunkSection[i];
      boolean flag = p_360452_.dimensionType().hasSkyLight();
      ChunkSource chunksource = p_360452_.getChunkSource();
      LevelLightEngine levellightengine = chunksource.getLightEngine();
      PalettedContainerFactory palettedcontainerfactory = p_360452_.palettedContainerFactory();
      boolean flag1 = false;

      for (SerializableChunkData.SectionData serializablechunkdata$sectiondata : this.sectionData) {
         SectionPos sectionpos = SectionPos.of(p_360628_, serializablechunkdata$sectiondata.y);
         if (serializablechunkdata$sectiondata.chunkSection != null) {
            alevelchunksection[p_360452_.getSectionIndexFromSectionY(serializablechunkdata$sectiondata.y)] = serializablechunkdata$sectiondata.chunkSection;
            p_364451_.checkConsistencyWithBlocks(sectionpos, serializablechunkdata$sectiondata.chunkSection);
         }

         boolean flag2 = serializablechunkdata$sectiondata.blockLight != null;
         boolean flag3 = flag && serializablechunkdata$sectiondata.skyLight != null;
         if (flag2 || flag3) {
            if (!flag1) {
               levellightengine.retainData(p_360628_, true);
               flag1 = true;
            }

            if (flag2) {
               levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, serializablechunkdata$sectiondata.blockLight);
            }

            if (flag3) {
               levellightengine.queueSectionData(LightLayer.SKY, sectionpos, serializablechunkdata$sectiondata.skyLight);
            }
         }
      }

      ChunkType chunktype = this.chunkStatus.getChunkType();
      ChunkAccess chunkaccess;
      if (chunktype == ChunkType.LEVELCHUNK) {
         LevelChunkTicks<Block> levelchunkticks = new LevelChunkTicks<>(this.packedTicks.blocks());
         LevelChunkTicks<Fluid> levelchunkticks1 = new LevelChunkTicks<>(this.packedTicks.fluids());
         chunkaccess = new LevelChunk(
            p_360452_.getLevel(),
            p_360628_,
            this.upgradeData,
            levelchunkticks,
            levelchunkticks1,
            this.inhabitedTime,
            alevelchunksection,
            postLoadChunk(p_360452_, this.entities, this.blockEntities),
            BlendingData.unpack(this.blendingData)
         );
         if (this.auxLightData != null) {
            ((LevelChunk)chunkaccess).getAuxLightManager(this.chunkPos).deserializeNBT(this.auxLightData);
         }
      } else {
         ProtoChunkTicks<Block> protochunkticks = ProtoChunkTicks.load(this.packedTicks.blocks());
         ProtoChunkTicks<Fluid> protochunkticks1 = ProtoChunkTicks.load(this.packedTicks.fluids());
         ProtoChunk protochunk1 = new ProtoChunk(
            p_360628_,
            this.upgradeData,
            alevelchunksection,
            protochunkticks,
            protochunkticks1,
            p_360452_,
            palettedcontainerfactory,
            BlendingData.unpack(this.blendingData)
         );
         chunkaccess = protochunk1;
         protochunk1.setInhabitedTime(this.inhabitedTime);
         if (this.belowZeroRetrogen != null) {
            protochunk1.setBelowZeroRetrogen(this.belowZeroRetrogen);
         }

         protochunk1.setPersistedStatus(this.chunkStatus);
         if (this.chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
            protochunk1.setLightEngine(levellightengine);
         }
      }

      chunkaccess.setLightCorrect(this.lightCorrect);
      EnumSet<Heightmap.Types> enumset = EnumSet.noneOf(Heightmap.Types.class);

      for (Heightmap.Types heightmap$types : chunkaccess.getPersistedStatus().heightmapsAfter()) {
         long[] along = this.heightmaps.get(heightmap$types);
         if (along != null) {
            chunkaccess.setHeightmap(heightmap$types, along);
         } else {
            enumset.add(heightmap$types);
         }
      }

      Heightmap.primeHeightmaps(chunkaccess, enumset);
      chunkaccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(p_360452_), this.structureData, p_360452_.getSeed()));
      chunkaccess.setAllReferences(unpackStructureReferences(p_360452_.registryAccess(), p_360628_, this.structureData));

      for (int j = 0; j < this.postProcessingSections.length; j++) {
         ShortList shortlist = this.postProcessingSections[j];
         if (shortlist != null) {
            chunkaccess.addPackedPostProcess(shortlist, j);
         }
      }

      if (this.attachmentData != null) {
         chunkaccess.readAttachmentsFromNBT(p_360452_.registryAccess(), this.attachmentData);
      }

      if (chunktype == ChunkType.LEVELCHUNK) {
         return new ImposterProtoChunk((LevelChunk)chunkaccess, false);
      } else {
         ProtoChunk protochunk = (ProtoChunk)chunkaccess;

         for (CompoundTag compoundtag : this.entities) {
            protochunk.addEntity(compoundtag);
         }

         for (CompoundTag compoundtag1 : this.blockEntities) {
            protochunk.setBlockEntityNbt(compoundtag1);
         }

         if (this.carvingMask != null) {
            protochunk.setCarvingMask(new CarvingMask(this.carvingMask, chunkaccess.getMinY()));
         }

         return protochunk;
      }
   }

   private static void logErrors(ChunkPos p_361949_, int p_364275_, String p_360703_) {
      LOGGER.error("Recoverable errors when loading section [{}, {}, {}]: {}", new Object[]{p_361949_.x, p_364275_, p_361949_.z, p_360703_});
   }

   public static SerializableChunkData copyOf(ServerLevel p_365319_, ChunkAccess p_362284_) {
      if (!p_362284_.canBeSerialized()) {
         throw new IllegalArgumentException("Chunk can't be serialized: " + p_362284_);
      } else {
         ChunkPos chunkpos = p_362284_.getPos();
         List<SerializableChunkData.SectionData> list = new ArrayList<>();
         LevelChunkSection[] alevelchunksection = p_362284_.getSections();
         LevelLightEngine levellightengine = p_365319_.getChunkSource().getLightEngine();

         for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
            int j = p_362284_.getSectionIndexFromSectionY(i);
            boolean flag = j >= 0 && j < alevelchunksection.length;
            DataLayer datalayer = levellightengine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkpos, i));
            DataLayer datalayer1 = levellightengine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkpos, i));
            DataLayer datalayer2 = datalayer != null && !datalayer.isEmpty() ? datalayer.copy() : null;
            DataLayer datalayer3 = datalayer1 != null && !datalayer1.isEmpty() ? datalayer1.copy() : null;
            if (flag || datalayer2 != null || datalayer3 != null) {
               LevelChunkSection levelchunksection = flag ? alevelchunksection[j].copy() : null;
               list.add(new SerializableChunkData.SectionData(i, levelchunksection, datalayer2, datalayer3));
            }
         }

         List<CompoundTag> list1 = new ArrayList<>(p_362284_.getBlockEntitiesPos().size());

         for (BlockPos blockpos : p_362284_.getBlockEntitiesPos()) {
            CompoundTag compoundtag = p_362284_.getBlockEntityNbtForSaving(blockpos, p_365319_.registryAccess());
            if (compoundtag != null) {
               list1.add(compoundtag);
            }
         }

         List<CompoundTag> list2 = new ArrayList<>();
         long[] along = null;
         if (p_362284_.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            ProtoChunk protochunk = (ProtoChunk)p_362284_;
            list2.addAll(protochunk.getEntities());
            CarvingMask carvingmask = protochunk.getCarvingMask();
            if (carvingmask != null) {
               along = carvingmask.toArray();
            }
         }

         Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);

         for (Entry<Heightmap.Types, Heightmap> entry : p_362284_.getHeightmaps()) {
            if (p_362284_.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
               long[] along1 = entry.getValue().getRawData();
               map.put(entry.getKey(), (long[])along1.clone());
            }
         }

         ChunkAccess.PackedTicks chunkaccess$packedticks = p_362284_.getTicksForSerialization(p_365319_.getGameTime());
         ShortList[] ashortlist = Arrays.stream(p_362284_.getPostProcessing())
            .map(p_460578_ -> p_460578_ != null && !p_460578_.isEmpty() ? new ShortArrayList(p_460578_) : null)
            .toArray(ShortList[]::new);
         CompoundTag compoundtag1 = packStructureData(
            StructurePieceSerializationContext.fromLevel(p_365319_), chunkpos, p_362284_.getAllStarts(), p_362284_.getAllReferences()
         );
         CompoundTag attachmentData = null;

         try {
            attachmentData = p_362284_.writeAttachmentsToNBT(p_365319_.registryAccess());
         } catch (Exception var16) {
            LOGGER.error(
               "Failed to write chunk attachments. An attachment has likely thrown an exception trying to write state. It will not persist. Report this to the mod author",
               var16
            );
         }

         ListTag auxLightData = null;
         if (p_362284_ instanceof LevelChunk levelChunk) {
            auxLightData = levelChunk.getAuxLightManager(chunkpos).serializeNBT();
         }

         return new SerializableChunkData(
            p_365319_.palettedContainerFactory(),
            chunkpos,
            p_362284_.getMinSectionY(),
            p_365319_.getGameTime(),
            p_362284_.getInhabitedTime(),
            p_362284_.getPersistedStatus(),
            Optionull.map(p_362284_.getBlendingData(), BlendingData::pack),
            p_362284_.getBelowZeroRetrogen(),
            p_362284_.getUpgradeData().copy(),
            along,
            map,
            chunkaccess$packedticks,
            ashortlist,
            p_362284_.isLightCorrect(),
            list,
            list2,
            list1,
            compoundtag1,
            attachmentData,
            auxLightData
         );
      }
   }

   public CompoundTag write() {
      CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
      compoundtag.putInt("xPos", this.chunkPos.x);
      compoundtag.putInt("yPos", this.minSectionY);
      compoundtag.putInt("zPos", this.chunkPos.z);
      compoundtag.putLong("LastUpdate", this.lastUpdateTime);
      compoundtag.putLong("InhabitedTime", this.inhabitedTime);
      compoundtag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(this.chunkStatus).toString());
      compoundtag.storeNullable("blending_data", BlendingData.Packed.CODEC, this.blendingData);
      compoundtag.storeNullable("below_zero_retrogen", BelowZeroRetrogen.CODEC, this.belowZeroRetrogen);
      if (!this.upgradeData.isEmpty()) {
         compoundtag.put("UpgradeData", this.upgradeData.write());
      }

      ListTag listtag = new ListTag();
      Codec<PalettedContainer<BlockState>> codec = this.containerFactory.blockStatesContainerCodec();
      Codec<PalettedContainerRO<Holder<Biome>>> codec1 = this.containerFactory.biomeContainerCodec();

      for (SerializableChunkData.SectionData serializablechunkdata$sectiondata : this.sectionData) {
         CompoundTag compoundtag1 = new CompoundTag();
         LevelChunkSection levelchunksection = serializablechunkdata$sectiondata.chunkSection;
         if (levelchunksection != null) {
            compoundtag1.store("block_states", codec, levelchunksection.getStates());
            compoundtag1.store("biomes", codec1, levelchunksection.getBiomes());
         }

         if (serializablechunkdata$sectiondata.blockLight != null) {
            compoundtag1.putByteArray("BlockLight", serializablechunkdata$sectiondata.blockLight.getData());
         }

         if (serializablechunkdata$sectiondata.skyLight != null) {
            compoundtag1.putByteArray("SkyLight", serializablechunkdata$sectiondata.skyLight.getData());
         }

         if (!compoundtag1.isEmpty()) {
            compoundtag1.putByte("Y", (byte)serializablechunkdata$sectiondata.y);
            listtag.add(compoundtag1);
         }
      }

      compoundtag.put("sections", listtag);
      if (this.lightCorrect) {
         compoundtag.putBoolean("isLightOn", true);
      }

      ListTag listtag1 = new ListTag();
      listtag1.addAll(this.blockEntities);
      compoundtag.put("block_entities", listtag1);
      if (this.chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
         ListTag listtag2 = new ListTag();
         listtag2.addAll(this.entities);
         compoundtag.put("entities", listtag2);
         if (this.carvingMask != null) {
            compoundtag.putLongArray("carving_mask", this.carvingMask);
         }
      }

      saveTicks(compoundtag, this.packedTicks);
      compoundtag.put("PostProcessing", packOffsets(this.postProcessingSections));
      CompoundTag compoundtag2 = new CompoundTag();
      this.heightmaps.forEach((p_362472_, p_363515_) -> compoundtag2.put(p_362472_.getSerializationKey(), new LongArrayTag(p_363515_)));
      compoundtag.put("Heightmaps", compoundtag2);
      compoundtag.put("structures", this.structureData);
      if (this.attachmentData != null) {
         compoundtag.put("neoforge:attachments", this.attachmentData);
      }

      if (this.auxLightData != null) {
         compoundtag.put("neoforge:aux_lights", this.auxLightData);
      }

      return compoundtag;
   }

   private static void saveTicks(CompoundTag p_362920_, ChunkAccess.PackedTicks p_361228_) {
      p_362920_.store("block_ticks", BLOCK_TICKS_CODEC, p_361228_.blocks());
      p_362920_.store("fluid_ticks", FLUID_TICKS_CODEC, p_361228_.fluids());
   }

   public static ChunkStatus getChunkStatusFromTag(@Nullable CompoundTag p_405768_) {
      return p_405768_ != null ? p_405768_.<ChunkStatus>read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY) : ChunkStatus.EMPTY;
   }

   @Nullable
   private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel p_363005_, List<CompoundTag> p_362405_, List<CompoundTag> p_364156_) {
      return p_362405_.isEmpty() && p_364156_.isEmpty()
         ? null
         : p_421450_ -> {
            if (!p_362405_.isEmpty()) {
               try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_421450_.problemPath(), LOGGER)) {
                  p_363005_.addLegacyChunkEntities(
                     EntityType.loadEntitiesRecursive(
                        TagValueInput.create(problemreporter$scopedcollector, p_363005_.registryAccess(), p_362405_), p_363005_, EntitySpawnReason.LOAD
                     )
                  );
               }
            }

            for (CompoundTag compoundtag : p_364156_) {
               boolean flag = compoundtag.getBooleanOr("keepPacked", false);
               if (flag) {
                  p_421450_.setBlockEntityNbt(compoundtag);
               } else {
                  BlockPos blockpos = BlockEntity.getPosFromTag(p_421450_.getPos(), compoundtag);
                  BlockEntity blockentity = BlockEntity.loadStatic(blockpos, p_421450_.getBlockState(blockpos), compoundtag, p_363005_.registryAccess());
                  if (blockentity != null) {
                     p_421450_.setBlockEntity(blockentity);
                  }
               }
            }
         };
   }

   private static CompoundTag packStructureData(
      StructurePieceSerializationContext p_361063_, ChunkPos p_365377_, Map<Structure, StructureStart> p_365385_, Map<Structure, LongSet> p_362410_
   ) {
      CompoundTag compoundtag = new CompoundTag();
      CompoundTag compoundtag1 = new CompoundTag();
      Registry<Structure> registry = p_361063_.registryAccess().lookupOrThrow(Registries.STRUCTURE);

      for (Entry<Structure, StructureStart> entry : p_365385_.entrySet()) {
         Identifier identifier = registry.getKey(entry.getKey());
         compoundtag1.put(identifier.toString(), entry.getValue().createTag(p_361063_, p_365377_));
      }

      compoundtag.put("starts", compoundtag1);
      CompoundTag compoundtag2 = new CompoundTag();

      for (Entry<Structure, LongSet> entry1 : p_362410_.entrySet()) {
         if (!entry1.getValue().isEmpty()) {
            Identifier identifier1 = registry.getKey(entry1.getKey());
            compoundtag2.putLongArray(identifier1.toString(), entry1.getValue().toLongArray());
         }
      }

      compoundtag.put("References", compoundtag2);
      return compoundtag;
   }

   private static Map<Structure, StructureStart> unpackStructureStart(StructurePieceSerializationContext p_360982_, CompoundTag p_363682_, long p_362695_) {
      Map<Structure, StructureStart> map = Maps.newHashMap();
      Registry<Structure> registry = p_360982_.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      CompoundTag compoundtag = p_363682_.getCompoundOrEmpty("starts");

      for (String s : compoundtag.keySet()) {
         Identifier identifier = Identifier.tryParse(s);
         Structure structure = registry.getValue(identifier);
         if (structure == null) {
            LOGGER.error("Unknown structure start: {}", identifier);
         } else {
            StructureStart structurestart = StructureStart.loadStaticStart(p_360982_, compoundtag.getCompoundOrEmpty(s), p_362695_);
            if (structurestart != null) {
               map.put(structure, structurestart);
            }
         }
      }

      return map;
   }

   private static Map<Structure, LongSet> unpackStructureReferences(RegistryAccess p_360856_, ChunkPos p_362116_, CompoundTag p_360575_) {
      Map<Structure, LongSet> map = Maps.newHashMap();
      Registry<Structure> registry = p_360856_.lookupOrThrow(Registries.STRUCTURE);
      CompoundTag compoundtag = p_360575_.getCompoundOrEmpty("References");
      compoundtag.forEach((p_466705_, p_466706_) -> {
         Identifier identifier = Identifier.tryParse(p_466705_);
         Structure structure = registry.getValue(identifier);
         if (structure == null) {
            LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", identifier, p_362116_);
         } else {
            Optional<long[]> optional = p_466706_.asLongArray();
            if (!optional.isEmpty()) {
               map.put(structure, new LongOpenHashSet(Arrays.stream(optional.get()).filter(p_360765_ -> {
                  ChunkPos chunkpos = new ChunkPos(p_360765_);
                  if (chunkpos.getChessboardDistance(p_362116_) > 8) {
                     LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", new Object[]{identifier, chunkpos, p_362116_});
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

   private static ListTag packOffsets(ShortList[] p_364852_) {
      ListTag listtag = new ListTag();

      for (ShortList shortlist : p_364852_) {
         ListTag listtag1 = new ListTag();
         if (shortlist != null) {
            for (int i = 0; i < shortlist.size(); i++) {
               listtag1.add(ShortTag.valueOf(shortlist.getShort(i)));
            }
         }

         listtag.add(listtag1);
      }

      return listtag;
   }

   public static class ChunkReadException extends NbtException {
      public ChunkReadException(String p_361971_) {
         super(p_361971_);
      }
   }

   public record SectionData(int y, @Nullable LevelChunkSection chunkSection, @Nullable DataLayer blockLight, @Nullable DataLayer skyLight) {
   }
}
