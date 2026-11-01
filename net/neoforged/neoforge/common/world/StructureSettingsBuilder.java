package net.neoforged.neoforge.common.world;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.HolderSet;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jspecify.annotations.Nullable;

public class StructureSettingsBuilder {
   private HolderSet<Biome> biomes;
   private final Map<MobCategory, StructureSettingsBuilder.StructureSpawnOverrideBuilder> spawnOverrides;
   private GenerationStep.Decoration step;
   private TerrainAdjustment terrainAdaptation;

   public static StructureSettingsBuilder copyOf(Structure.StructureSettings settings) {
      return new StructureSettingsBuilder(settings.biomes(), settings.spawnOverrides(), settings.step(), settings.terrainAdaptation());
   }

   private StructureSettingsBuilder(
      HolderSet<Biome> biomes, Map<MobCategory, StructureSpawnOverride> spawnOverrides, GenerationStep.Decoration step, TerrainAdjustment terrainAdaptation
   ) {
      this.biomes = biomes;
      this.spawnOverrides = spawnOverrides.entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, entry -> StructureSettingsBuilder.StructureSpawnOverrideBuilder.copyOf(entry.getValue())));
      this.step = step;
      this.terrainAdaptation = terrainAdaptation;
   }

   public Structure.StructureSettings build() {
      Map<MobCategory, StructureSpawnOverride> overrides = Collections.unmodifiableMap(
         this.spawnOverrides.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().build()))
      );
      return new Structure.StructureSettings(this.biomes, overrides, this.step, this.terrainAdaptation);
   }

   public HolderSet<Biome> getBiomes() {
      return this.biomes;
   }

   public void setBiomes(HolderSet<Biome> biomes) {
      this.biomes = biomes;
   }

   @Nullable
   public StructureSettingsBuilder.StructureSpawnOverrideBuilder getSpawnOverrides(MobCategory category) {
      return this.spawnOverrides.get(category);
   }

   public StructureSettingsBuilder.StructureSpawnOverrideBuilder getOrAddSpawnOverrides(MobCategory category) {
      return this.spawnOverrides
         .computeIfAbsent(
            category, c -> new StructureSettingsBuilder.StructureSpawnOverrideBuilder(StructureSpawnOverride.BoundingBoxType.PIECE, WeightedList.of())
         );
   }

   public void removeSpawnOverrides(MobCategory category) {
      this.spawnOverrides.remove(category);
   }

   public GenerationStep.Decoration getDecorationStep() {
      return this.step;
   }

   public void setDecorationStep(GenerationStep.Decoration step) {
      this.step = step;
   }

   public TerrainAdjustment getTerrainAdaptation() {
      return this.terrainAdaptation;
   }

   public void setTerrainAdaptation(TerrainAdjustment terrainAdaptation) {
      this.terrainAdaptation = terrainAdaptation;
   }

   public static class StructureSpawnOverrideBuilder {
      private StructureSpawnOverride.BoundingBoxType boundingBox;
      private final WeightedList.Builder<MobSpawnSettings.SpawnerData> spawns = WeightedList.builder();
      private final List<Weighted<MobSpawnSettings.SpawnerData>> spawnsView;

      public static StructureSettingsBuilder.StructureSpawnOverrideBuilder copyOf(StructureSpawnOverride override) {
         return new StructureSettingsBuilder.StructureSpawnOverrideBuilder(override.boundingBox(), override.spawns());
      }

      private StructureSpawnOverrideBuilder(StructureSpawnOverride.BoundingBoxType boundingBox, WeightedList<MobSpawnSettings.SpawnerData> spawns) {
         this.boundingBox = boundingBox;
         this.spawns.addAll(spawns);
         this.spawnsView = this.spawns.getList();
      }

      public StructureSpawnOverride.BoundingBoxType getBoundingBox() {
         return this.boundingBox;
      }

      public void setBoundingBox(StructureSpawnOverride.BoundingBoxType boundingBox) {
         this.boundingBox = boundingBox;
      }

      public List<Weighted<MobSpawnSettings.SpawnerData>> getSpawns() {
         return this.spawnsView;
      }

      public void addSpawn(Weighted<MobSpawnSettings.SpawnerData> spawn) {
         this.spawns.add(spawn);
      }

      public void addSpawn(MobSpawnSettings.SpawnerData spawn, int weight) {
         this.spawns.add(spawn, weight);
      }

      public void removeSpawn(Weighted<MobSpawnSettings.SpawnerData> spawn) {
         this.spawns.remove(spawn);
      }

      public void removeSpawn(MobSpawnSettings.SpawnerData spawn) {
         this.spawns.remove(spawn);
      }

      public void removeSpawns(Predicate<Weighted<MobSpawnSettings.SpawnerData>> spawnPredicate) {
         this.spawns.removeIf(spawnPredicate);
      }

      public StructureSpawnOverride build() {
         return new StructureSpawnOverride(this.boundingBox, this.spawns.build());
      }
   }
}
