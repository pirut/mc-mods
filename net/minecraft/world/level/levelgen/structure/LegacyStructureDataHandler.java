package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.LegacyTagFixer;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jspecify.annotations.Nullable;

public class LegacyStructureDataHandler implements LegacyTagFixer {
   public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
   private static final Map<String, String> CURRENT_TO_LEGACY_MAP = Util.make(Maps.newHashMap(), hashMap -> {
      hashMap.put("Village", "Village");
      hashMap.put("Mineshaft", "Mineshaft");
      hashMap.put("Mansion", "Mansion");
      hashMap.put("Igloo", "Temple");
      hashMap.put("Desert_Pyramid", "Temple");
      hashMap.put("Jungle_Pyramid", "Temple");
      hashMap.put("Swamp_Hut", "Temple");
      hashMap.put("Stronghold", "Stronghold");
      hashMap.put("Monument", "Monument");
      hashMap.put("Fortress", "Fortress");
      hashMap.put("EndCity", "EndCity");
   });
   private static final Map<String, String> LEGACY_TO_CURRENT_MAP = Util.make(Maps.newHashMap(), hashMap -> {
      hashMap.put("Iglu", "Igloo");
      hashMap.put("TeDP", "Desert_Pyramid");
      hashMap.put("TeJP", "Jungle_Pyramid");
      hashMap.put("TeSH", "Swamp_Hut");
   });
   private static final Set<String> OLD_STRUCTURE_REGISTRY_KEYS = Set.of(
      "pillager_outpost",
      "mineshaft",
      "mansion",
      "jungle_pyramid",
      "desert_pyramid",
      "igloo",
      "ruined_portal",
      "shipwreck",
      "swamp_hut",
      "stronghold",
      "monument",
      "ocean_ruin",
      "fortress",
      "endcity",
      "buried_treasure",
      "village",
      "nether_fossil",
      "bastion_remnant"
   );
   private final boolean hasLegacyData;
   private final Map<String, Long2ObjectMap<CompoundTag>> dataMap = Maps.newHashMap();
   private final Map<String, StructureFeatureIndexSavedData> indexMap = Maps.newHashMap();
   @Nullable
   private final DimensionDataStorage dimensionDataStorage;
   private final List<String> legacyKeys;
   private final List<String> currentKeys;
   private final DataFixer dataFixer;
   private boolean cachesInitialized;

   public LegacyStructureDataHandler(@Nullable DimensionDataStorage storage, List<String> legacyKeys, List<String> currentKeys, DataFixer dataFixer) {
      this.dimensionDataStorage = storage;
      this.legacyKeys = legacyKeys;
      this.currentKeys = currentKeys;
      this.dataFixer = dataFixer;
      boolean bl = false;

      for (String string : this.currentKeys) {
         bl |= this.dataMap.get(string) != null;
      }

      this.hasLegacyData = bl;
   }

   @Override
   public void markChunkDone(ChunkPos chunkPos) {
      long l = chunkPos.toLong();

      for (String string : this.legacyKeys) {
         StructureFeatureIndexSavedData structureFeatureIndexSavedData = this.indexMap.get(string);
         if (structureFeatureIndexSavedData != null && structureFeatureIndexSavedData.hasUnhandledIndex(l)) {
            structureFeatureIndexSavedData.removeIndex(l);
         }
      }
   }

   @Override
   public int targetDataVersion() {
      return 1493;
   }

   @Override
   public CompoundTag applyFix(CompoundTag compoundTag) {
      if (!this.cachesInitialized && this.dimensionDataStorage != null) {
         this.populateCaches(this.dimensionDataStorage);
      }

      int i = NbtUtils.getDataVersion(compoundTag);
      if (i < 1493) {
         compoundTag = DataFixTypes.CHUNK.update(this.dataFixer, compoundTag, i, 1493);
         if (compoundTag.getCompound("Level").flatMap(compoundTagx -> compoundTagx.getBoolean("hasLegacyStructureData")).orElse(false)) {
            compoundTag = this.updateFromLegacy(compoundTag);
         }
      }

      return compoundTag;
   }

   private CompoundTag updateFromLegacy(CompoundTag tag) {
      CompoundTag compoundTag = tag.getCompoundOrEmpty("Level");
      ChunkPos chunkPos = new ChunkPos(compoundTag.getIntOr("xPos", 0), compoundTag.getIntOr("zPos", 0));
      if (this.isUnhandledStructureStart(chunkPos.x, chunkPos.z)) {
         tag = this.updateStructureStart(tag, chunkPos);
      }

      CompoundTag compoundTag2 = compoundTag.getCompoundOrEmpty("Structures");
      CompoundTag compoundTag3 = compoundTag2.getCompoundOrEmpty("References");

      for (String string : this.currentKeys) {
         boolean bl = OLD_STRUCTURE_REGISTRY_KEYS.contains(string.toLowerCase(Locale.ROOT));
         if (!compoundTag3.getLongArray(string).isPresent() && bl) {
            int i = 8;
            LongList longList = new LongArrayList();

            for (int j = chunkPos.x - 8; j <= chunkPos.x + 8; j++) {
               for (int k = chunkPos.z - 8; k <= chunkPos.z + 8; k++) {
                  if (this.hasLegacyStart(j, k, string)) {
                     longList.add(ChunkPos.asLong(j, k));
                  }
               }
            }

            compoundTag3.putLongArray(string, longList.toLongArray());
         }
      }

      compoundTag2.put("References", compoundTag3);
      compoundTag.put("Structures", compoundTag2);
      tag.put("Level", compoundTag);
      return tag;
   }

   private boolean hasLegacyStart(int chunkX, int chunkZ, String key) {
      return !this.hasLegacyData
         ? false
         : this.dataMap.get(key) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(key)).hasStartIndex(ChunkPos.asLong(chunkX, chunkZ));
   }

   private boolean isUnhandledStructureStart(int chunkX, int chunkZ) {
      if (!this.hasLegacyData) {
         return false;
      } else {
         for (String string : this.currentKeys) {
            if (this.dataMap.get(string) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(string)).hasUnhandledIndex(ChunkPos.asLong(chunkX, chunkZ))) {
               return true;
            }
         }

         return false;
      }
   }

   private CompoundTag updateStructureStart(CompoundTag tag, ChunkPos chunkPos) {
      CompoundTag compoundTag = tag.getCompoundOrEmpty("Level");
      CompoundTag compoundTag2 = compoundTag.getCompoundOrEmpty("Structures");
      CompoundTag compoundTag3 = compoundTag2.getCompoundOrEmpty("Starts");

      for (String string : this.currentKeys) {
         Long2ObjectMap<CompoundTag> long2ObjectMap = this.dataMap.get(string);
         if (long2ObjectMap != null) {
            long l = chunkPos.toLong();
            if (this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(string)).hasUnhandledIndex(l)) {
               CompoundTag compoundTag4 = (CompoundTag)long2ObjectMap.get(l);
               if (compoundTag4 != null) {
                  compoundTag3.put(string, compoundTag4);
               }
            }
         }
      }

      compoundTag2.put("Starts", compoundTag3);
      compoundTag.put("Structures", compoundTag2);
      tag.put("Level", compoundTag);
      return tag;
   }

   private synchronized void populateCaches(DimensionDataStorage storage) {
      if (!this.cachesInitialized) {
         for (String string : this.legacyKeys) {
            CompoundTag compoundTag = new CompoundTag();

            try {
               compoundTag = storage.readTagFromDisk(string, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES, 1493)
                  .getCompoundOrEmpty("data")
                  .getCompoundOrEmpty("Features");
               if (compoundTag.isEmpty()) {
                  continue;
               }
            } catch (IOException var8) {
            }

            compoundTag.forEach(
               (stringx, tag) -> {
                  if (tag instanceof CompoundTag compoundTagx) {
                     long l = ChunkPos.asLong(compoundTagx.getIntOr("ChunkX", 0), compoundTagx.getIntOr("ChunkZ", 0));
                     ListTag listTag = compoundTagx.getListOrEmpty("Children");
                     if (!listTag.isEmpty()) {
                        Optional<String> optional = listTag.getCompound(0).flatMap(compoundTagxx -> compoundTagxx.getString("id"));
                        optional.map(LEGACY_TO_CURRENT_MAP::get).ifPresent(stringxx -> compoundTagx.putString("id", stringxx));
                     }

                     compoundTagx.getString("id")
                        .ifPresent(stringxx -> this.dataMap.computeIfAbsent(stringxx, stringxxx -> new Long2ObjectOpenHashMap()).put(l, compoundTagx));
                  }
               }
            );
            String string2 = string + "_index";
            StructureFeatureIndexSavedData structureFeatureIndexSavedData = storage.computeIfAbsent(StructureFeatureIndexSavedData.type(string2));
            if (structureFeatureIndexSavedData.getAll().isEmpty()) {
               StructureFeatureIndexSavedData structureFeatureIndexSavedData2 = new StructureFeatureIndexSavedData();
               this.indexMap.put(string, structureFeatureIndexSavedData2);
               compoundTag.forEach((stringx, tag) -> {
                  if (tag instanceof CompoundTag compoundTagx) {
                     structureFeatureIndexSavedData2.addIndex(ChunkPos.asLong(compoundTagx.getIntOr("ChunkX", 0), compoundTagx.getIntOr("ChunkZ", 0)));
                  }
               });
            } else {
               this.indexMap.put(string, structureFeatureIndexSavedData);
            }
         }

         this.cachesInitialized = true;
      }
   }

   public static Supplier<LegacyTagFixer> getLegacyTagFixer(ResourceKey<Level> dimension, Supplier<DimensionDataStorage> storageSupplier, DataFixer dataFixer) {
      if (dimension == Level.OVERWORLD) {
         return () -> new LegacyStructureDataHandler(
            storageSupplier.get(),
            ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
            ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"),
            dataFixer
         );
      } else if (dimension == Level.NETHER) {
         List<String> list = ImmutableList.of("Fortress");
         return () -> new LegacyStructureDataHandler(storageSupplier.get(), list, list, dataFixer);
      } else if (dimension == Level.END) {
         List<String> list = ImmutableList.of("EndCity");
         return () -> new LegacyStructureDataHandler(storageSupplier.get(), list, list, dataFixer);
      } else {
         return LegacyTagFixer.EMPTY;
      }
   }
}
