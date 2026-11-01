package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class LegacyStructureDataHandler {
   private static final Map<String, String> CURRENT_TO_LEGACY_MAP = Util.make(Maps.newHashMap(), p_71337_ -> {
      p_71337_.put("Village", "Village");
      p_71337_.put("Mineshaft", "Mineshaft");
      p_71337_.put("Mansion", "Mansion");
      p_71337_.put("Igloo", "Temple");
      p_71337_.put("Desert_Pyramid", "Temple");
      p_71337_.put("Jungle_Pyramid", "Temple");
      p_71337_.put("Swamp_Hut", "Temple");
      p_71337_.put("Stronghold", "Stronghold");
      p_71337_.put("Monument", "Monument");
      p_71337_.put("Fortress", "Fortress");
      p_71337_.put("EndCity", "EndCity");
   });
   private static final Map<String, String> LEGACY_TO_CURRENT_MAP = Util.make(Maps.newHashMap(), p_71325_ -> {
      p_71325_.put("Iglu", "Igloo");
      p_71325_.put("TeDP", "Desert_Pyramid");
      p_71325_.put("TeJP", "Jungle_Pyramid");
      p_71325_.put("TeSH", "Swamp_Hut");
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
   private final List<String> legacyKeys;
   private final List<String> currentKeys;

   public LegacyStructureDataHandler(@Nullable DimensionDataStorage p_71308_, List<String> p_71309_, List<String> p_71310_) {
      this.legacyKeys = p_71309_;
      this.currentKeys = p_71310_;
      this.populateCaches(p_71308_);
      boolean $$3 = false;

      for (String $$4 : this.currentKeys) {
         $$3 |= this.dataMap.get($$4) != null;
      }

      this.hasLegacyData = $$3;
   }

   public void removeIndex(long p_71319_) {
      for (String $$1 : this.legacyKeys) {
         StructureFeatureIndexSavedData $$2 = this.indexMap.get($$1);
         if ($$2 != null && $$2.hasUnhandledIndex(p_71319_)) {
            $$2.removeIndex(p_71319_);
            $$2.setDirty();
         }
      }
   }

   public CompoundTag updateFromLegacy(CompoundTag p_71327_) {
      CompoundTag $$1 = p_71327_.getCompound("Level");
      ChunkPos $$2 = new ChunkPos($$1.getInt("xPos"), $$1.getInt("zPos"));
      if (this.isUnhandledStructureStart($$2.x, $$2.z)) {
         p_71327_ = this.updateStructureStart(p_71327_, $$2);
      }

      CompoundTag $$3 = $$1.getCompound("Structures");
      CompoundTag $$4 = $$3.getCompound("References");

      for (String $$5 : this.currentKeys) {
         boolean $$6 = OLD_STRUCTURE_REGISTRY_KEYS.contains($$5.toLowerCase(Locale.ROOT));
         if (!$$4.contains($$5, 12) && $$6) {
            int $$7 = 8;
            LongList $$8 = new LongArrayList();

            for (int $$9 = $$2.x - 8; $$9 <= $$2.x + 8; $$9++) {
               for (int $$10 = $$2.z - 8; $$10 <= $$2.z + 8; $$10++) {
                  if (this.hasLegacyStart($$9, $$10, $$5)) {
                     $$8.add(ChunkPos.asLong($$9, $$10));
                  }
               }
            }

            $$4.putLongArray($$5, $$8);
         }
      }

      $$3.put("References", $$4);
      $$1.put("Structures", $$3);
      p_71327_.put("Level", $$1);
      return p_71327_;
   }

   private boolean hasLegacyStart(int p_71315_, int p_71316_, String p_71317_) {
      return !this.hasLegacyData
         ? false
         : this.dataMap.get(p_71317_) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(p_71317_)).hasStartIndex(ChunkPos.asLong(p_71315_, p_71316_));
   }

   private boolean isUnhandledStructureStart(int p_71312_, int p_71313_) {
      if (!this.hasLegacyData) {
         return false;
      } else {
         for (String $$2 : this.currentKeys) {
            if (this.dataMap.get($$2) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get($$2)).hasUnhandledIndex(ChunkPos.asLong(p_71312_, p_71313_))) {
               return true;
            }
         }

         return false;
      }
   }

   private CompoundTag updateStructureStart(CompoundTag p_71329_, ChunkPos p_71330_) {
      CompoundTag $$2 = p_71329_.getCompound("Level");
      CompoundTag $$3 = $$2.getCompound("Structures");
      CompoundTag $$4 = $$3.getCompound("Starts");

      for (String $$5 : this.currentKeys) {
         Long2ObjectMap<CompoundTag> $$6 = this.dataMap.get($$5);
         if ($$6 != null) {
            long $$7 = p_71330_.toLong();
            if (this.indexMap.get(CURRENT_TO_LEGACY_MAP.get($$5)).hasUnhandledIndex($$7)) {
               CompoundTag $$8 = (CompoundTag)$$6.get($$7);
               if ($$8 != null) {
                  $$4.put($$5, $$8);
               }
            }
         }
      }

      $$3.put("Starts", $$4);
      $$2.put("Structures", $$3);
      p_71329_.put("Level", $$2);
      return p_71329_;
   }

   private void populateCaches(@Nullable DimensionDataStorage p_71321_) {
      if (p_71321_ != null) {
         for (String $$1 : this.legacyKeys) {
            CompoundTag $$2 = new CompoundTag();

            try {
               $$2 = p_71321_.readTagFromDisk($$1, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES, 1493).getCompound("data").getCompound("Features");
               if ($$2.isEmpty()) {
                  continue;
               }
            } catch (IOException var13) {
            }

            for (String $$3 : $$2.getAllKeys()) {
               CompoundTag $$4 = $$2.getCompound($$3);
               long $$5 = ChunkPos.asLong($$4.getInt("ChunkX"), $$4.getInt("ChunkZ"));
               ListTag $$6 = $$4.getList("Children", 10);
               if (!$$6.isEmpty()) {
                  String $$7 = $$6.getCompound(0).getString("id");
                  String $$8 = LEGACY_TO_CURRENT_MAP.get($$7);
                  if ($$8 != null) {
                     $$4.putString("id", $$8);
                  }
               }

               String $$9 = $$4.getString("id");
               this.dataMap.computeIfAbsent($$9, p_71335_ -> new Long2ObjectOpenHashMap()).put($$5, $$4);
            }

            String $$10 = $$1 + "_index";
            StructureFeatureIndexSavedData $$11 = p_71321_.computeIfAbsent(StructureFeatureIndexSavedData.factory(), $$10);
            if (!$$11.getAll().isEmpty()) {
               this.indexMap.put($$1, $$11);
            } else {
               StructureFeatureIndexSavedData $$12 = new StructureFeatureIndexSavedData();
               this.indexMap.put($$1, $$12);

               for (String $$13 : $$2.getAllKeys()) {
                  CompoundTag $$14 = $$2.getCompound($$13);
                  $$12.addIndex(ChunkPos.asLong($$14.getInt("ChunkX"), $$14.getInt("ChunkZ")));
               }

               $$12.setDirty();
            }
         }
      }
   }

   public static LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> p_71332_, @Nullable DimensionDataStorage p_71333_) {
      if (p_71332_ == Level.OVERWORLD) {
         return new LegacyStructureDataHandler(
            p_71333_,
            ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
            ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument")
         );
      } else if (p_71332_ == Level.NETHER) {
         List<String> $$2 = ImmutableList.of("Fortress");
         return new LegacyStructureDataHandler(p_71333_, $$2, $$2);
      } else if (p_71332_ == Level.END) {
         List<String> $$3 = ImmutableList.of("EndCity");
         return new LegacyStructureDataHandler(p_71333_, $$3, $$3);
      } else {
         throw new RuntimeException(String.format(Locale.ROOT, "Unknown dimension type : %s", p_71332_));
      }
   }
}
