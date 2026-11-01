package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class StructureCheck {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_STRUCTURE = -1;
   private final ChunkScanAccess storageAccess;
   private final RegistryAccess registryAccess;
   private final StructureTemplateManager structureTemplateManager;
   private final ResourceKey<Level> dimension;
   private final ChunkGenerator chunkGenerator;
   private final RandomState randomState;
   private final LevelHeightAccessor heightAccessor;
   private final BiomeSource biomeSource;
   private final long seed;
   private final DataFixer fixerUpper;
   private final Long2ObjectMap<Object2IntMap<Structure>> loadedChunks = new Long2ObjectOpenHashMap();
   private final Map<Structure, Long2BooleanMap> featureChecks = new HashMap<>();

   public StructureCheck(
      ChunkScanAccess p_226712_,
      RegistryAccess p_226713_,
      StructureTemplateManager p_226714_,
      ResourceKey<Level> p_226715_,
      ChunkGenerator p_226716_,
      RandomState p_226717_,
      LevelHeightAccessor p_226718_,
      BiomeSource p_226719_,
      long p_226720_,
      DataFixer p_226721_
   ) {
      this.storageAccess = p_226712_;
      this.registryAccess = p_226713_;
      this.structureTemplateManager = p_226714_;
      this.dimension = p_226715_;
      this.chunkGenerator = p_226716_;
      this.randomState = p_226717_;
      this.heightAccessor = p_226718_;
      this.biomeSource = p_226719_;
      this.seed = p_226720_;
      this.fixerUpper = p_226721_;
   }

   public StructureCheckResult checkStart(ChunkPos p_226730_, Structure p_226731_, StructurePlacement p_321610_, boolean p_226732_) {
      long $$4 = p_226730_.toLong();
      Object2IntMap<Structure> $$5 = (Object2IntMap<Structure>)this.loadedChunks.get($$4);
      if ($$5 != null) {
         return this.checkStructureInfo($$5, p_226731_, p_226732_);
      } else {
         StructureCheckResult $$6 = this.tryLoadFromStorage(p_226730_, p_226731_, p_226732_, $$4);
         if ($$6 != null) {
            return $$6;
         } else if (!p_321610_.applyAdditionalChunkRestrictions(p_226730_.x, p_226730_.z, this.seed)) {
            return StructureCheckResult.START_NOT_PRESENT;
         } else {
            boolean $$7 = this.featureChecks
               .computeIfAbsent(p_226731_, p_226739_ -> new Long2BooleanOpenHashMap())
               .computeIfAbsent($$4, p_226728_ -> this.canCreateStructure(p_226730_, p_226731_));
            return !$$7 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
         }
      }
   }

   private boolean canCreateStructure(ChunkPos p_226756_, Structure p_226757_) {
      return p_226757_.findValidGenerationPoint(
            new Structure.GenerationContext(
               this.registryAccess,
               this.chunkGenerator,
               this.biomeSource,
               this.randomState,
               this.structureTemplateManager,
               this.seed,
               p_226756_,
               this.heightAccessor,
               p_226757_.biomes()::contains
            )
         )
         .isPresent();
   }

   @Nullable
   private StructureCheckResult tryLoadFromStorage(ChunkPos p_226734_, Structure p_226735_, boolean p_226736_, long p_226737_) {
      CollectFields $$4 = new CollectFields(
         new FieldSelector(IntTag.TYPE, "DataVersion"),
         new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"),
         new FieldSelector("structures", CompoundTag.TYPE, "starts")
      );

      try {
         this.storageAccess.scanChunk(p_226734_, $$4).join();
      } catch (Exception var13) {
         LOGGER.warn("Failed to read chunk {}", p_226734_, var13);
         return StructureCheckResult.CHUNK_LOAD_NEEDED;
      }

      if (!($$4.getResult() instanceof CompoundTag $$7)) {
         return null;
      } else {
         int $$8 = ChunkStorage.getVersion($$7);
         if ($$8 <= 1493) {
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         } else {
            ChunkStorage.injectDatafixingContext($$7, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

            CompoundTag $$9;
            try {
               $$9 = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, $$7, $$8);
            } catch (Exception var12) {
               LOGGER.warn("Failed to partially datafix chunk {}", p_226734_, var12);
               return StructureCheckResult.CHUNK_LOAD_NEEDED;
            }

            Object2IntMap<Structure> $$12 = this.loadStructures($$9);
            if ($$12 == null) {
               return null;
            } else {
               this.storeFullResults(p_226737_, $$12);
               return this.checkStructureInfo($$12, p_226735_, p_226736_);
            }
         }
      }
   }

   @Nullable
   private Object2IntMap<Structure> loadStructures(CompoundTag p_197312_) {
      if (!p_197312_.contains("structures", 10)) {
         return null;
      } else {
         CompoundTag $$1 = p_197312_.getCompound("structures");
         if (!$$1.contains("starts", 10)) {
            return null;
         } else {
            CompoundTag $$2 = $$1.getCompound("starts");
            if ($$2.isEmpty()) {
               return Object2IntMaps.emptyMap();
            } else {
               Object2IntMap<Structure> $$3 = new Object2IntOpenHashMap();
               Registry<Structure> $$4 = this.registryAccess.registryOrThrow(Registries.STRUCTURE);

               for (String $$5 : $$2.getAllKeys()) {
                  ResourceLocation $$6 = ResourceLocation.tryParse($$5);
                  if ($$6 != null) {
                     Structure $$7 = $$4.get($$6);
                     if ($$7 != null) {
                        CompoundTag $$8 = $$2.getCompound($$5);
                        if (!$$8.isEmpty()) {
                           String $$9 = $$8.getString("id");
                           if (!"INVALID".equals($$9)) {
                              int $$10 = $$8.getInt("references");
                              $$3.put($$7, $$10);
                           }
                        }
                     }
                  }
               }

               return $$3;
            }
         }
      }
   }

   private static Object2IntMap<Structure> deduplicateEmptyMap(Object2IntMap<Structure> p_197299_) {
      return p_197299_.isEmpty() ? Object2IntMaps.emptyMap() : p_197299_;
   }

   private StructureCheckResult checkStructureInfo(Object2IntMap<Structure> p_226752_, Structure p_226753_, boolean p_226754_) {
      int $$3 = p_226752_.getOrDefault(p_226753_, -1);
      return $$3 == -1 || p_226754_ && $$3 != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
   }

   public void onStructureLoad(ChunkPos p_197283_, Map<Structure, StructureStart> p_197284_) {
      long $$2 = p_197283_.toLong();
      Object2IntMap<Structure> $$3 = new Object2IntOpenHashMap();
      p_197284_.forEach((p_226749_, p_226750_) -> {
         if (p_226750_.isValid()) {
            $$3.put(p_226749_, p_226750_.getReferences());
         }
      });
      this.storeFullResults($$2, $$3);
   }

   private void storeFullResults(long p_197264_, Object2IntMap<Structure> p_197265_) {
      this.loadedChunks.put(p_197264_, deduplicateEmptyMap(p_197265_));
      this.featureChecks.values().forEach(p_209956_ -> p_209956_.remove(p_197264_));
   }

   public void incrementReference(ChunkPos p_226723_, Structure p_226724_) {
      this.loadedChunks.compute(p_226723_.toLong(), (p_226745_, p_226746_) -> {
         if (p_226746_ == null || p_226746_.isEmpty()) {
            p_226746_ = new Object2IntOpenHashMap();
         }

         p_226746_.computeInt(p_226724_, (p_226741_, p_226742_) -> p_226742_ == null ? 1 : p_226742_ + 1);
         return p_226746_;
      });
   }
}
