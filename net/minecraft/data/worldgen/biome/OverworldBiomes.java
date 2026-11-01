package net.minecraft.data.worldgen.biome;

import javax.annotation.Nullable;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class OverworldBiomes {
   protected static final int NORMAL_WATER_COLOR = 4159204;
   protected static final int NORMAL_WATER_FOG_COLOR = 329011;
   private static final int OVERWORLD_FOG_COLOR = 12638463;
   @Nullable
   private static final Music NORMAL_MUSIC = null;
   public static final int SWAMP_SKELETON_WEIGHT = 70;

   protected static int calculateSkyColor(float p_194844_) {
      float $$1 = p_194844_ / 3.0F;
      $$1 = Mth.clamp($$1, -1.0F, 1.0F);
      return Mth.hsvToRgb(0.62222224F - $$1 * 0.05F, 0.5F + $$1 * 0.1F, 1.0F);
   }

   private static Biome biome(
      boolean p_265746_,
      float p_265800_,
      float p_265276_,
      MobSpawnSettings.Builder p_265425_,
      BiomeGenerationSettings.Builder p_265371_,
      @Nullable Music p_265636_
   ) {
      return biome(p_265746_, p_265800_, p_265276_, 4159204, 329011, null, null, p_265425_, p_265371_, p_265636_);
   }

   private static Biome biome(
      boolean p_273483_,
      float p_272621_,
      float p_273588_,
      int p_273605_,
      int p_272756_,
      @Nullable Integer p_272889_,
      @Nullable Integer p_272657_,
      MobSpawnSettings.Builder p_273300_,
      BiomeGenerationSettings.Builder p_272700_,
      @Nullable Music p_272996_
   ) {
      BiomeSpecialEffects.Builder $$10 = new BiomeSpecialEffects.Builder()
         .waterColor(p_273605_)
         .waterFogColor(p_272756_)
         .fogColor(12638463)
         .skyColor(calculateSkyColor(p_272621_))
         .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
         .backgroundMusic(p_272996_);
      if (p_272889_ != null) {
         $$10.grassColorOverride(p_272889_);
      }

      if (p_272657_ != null) {
         $$10.foliageColorOverride(p_272657_);
      }

      return new Biome.BiomeBuilder()
         .hasPrecipitation(p_273483_)
         .temperature(p_272621_)
         .downfall(p_273588_)
         .specialEffects($$10.build())
         .mobSpawnSettings(p_273300_.build())
         .generationSettings(p_272700_.build())
         .build();
   }

   private static void globalOverworldGeneration(BiomeGenerationSettings.Builder p_194870_) {
      BiomeDefaultFeatures.addDefaultCarversAndLakes(p_194870_);
      BiomeDefaultFeatures.addDefaultCrystalFormations(p_194870_);
      BiomeDefaultFeatures.addDefaultMonsterRoom(p_194870_);
      BiomeDefaultFeatures.addDefaultUndergroundVariety(p_194870_);
      BiomeDefaultFeatures.addDefaultSprings(p_194870_);
      BiomeDefaultFeatures.addSurfaceFreezing(p_194870_);
   }

   public static Biome oldGrowthTaiga(HolderGetter<PlacedFeature> p_255849_, HolderGetter<ConfiguredWorldCarver<?>> p_256578_, boolean p_194877_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$3);
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4));
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
      if (p_194877_) {
         BiomeDefaultFeatures.commonSpawns($$3);
      } else {
         BiomeDefaultFeatures.caveSpawns($$3);
         BiomeDefaultFeatures.monsters($$3, 100, 25, 100, false);
      }

      BiomeGenerationSettings.Builder $$4 = new BiomeGenerationSettings.Builder(p_255849_, p_256578_);
      globalOverworldGeneration($$4);
      BiomeDefaultFeatures.addMossyStoneBlock($$4);
      BiomeDefaultFeatures.addFerns($$4);
      BiomeDefaultFeatures.addDefaultOres($$4);
      BiomeDefaultFeatures.addDefaultSoftDisks($$4);
      $$4.addFeature(
         GenerationStep.Decoration.VEGETAL_DECORATION,
         p_194877_ ? VegetationPlacements.TREES_OLD_GROWTH_SPRUCE_TAIGA : VegetationPlacements.TREES_OLD_GROWTH_PINE_TAIGA
      );
      BiomeDefaultFeatures.addDefaultFlowers($$4);
      BiomeDefaultFeatures.addGiantTaigaVegetation($$4);
      BiomeDefaultFeatures.addDefaultMushrooms($$4);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$4);
      BiomeDefaultFeatures.addCommonBerryBushes($$4);
      Music $$5 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_OLD_GROWTH_TAIGA);
      return biome(true, p_194877_ ? 0.25F : 0.3F, 0.8F, $$3, $$4, $$5);
   }

   public static Biome sparseJungle(HolderGetter<PlacedFeature> p_255977_, HolderGetter<ConfiguredWorldCarver<?>> p_256531_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns($$2);
      $$2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 2, 4));
      return baseJungle(p_255977_, p_256531_, 0.8F, false, true, false, $$2, Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SPARSE_JUNGLE));
   }

   public static Biome jungle(HolderGetter<PlacedFeature> p_256033_, HolderGetter<ConfiguredWorldCarver<?>> p_255651_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns($$2);
      $$2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 40, 1, 2))
         .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 2, 1, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 1, 1, 2));
      return baseJungle(p_256033_, p_255651_, 0.9F, false, false, true, $$2, Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JUNGLE));
   }

   public static Biome bambooJungle(HolderGetter<PlacedFeature> p_255817_, HolderGetter<ConfiguredWorldCarver<?>> p_256096_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.baseJungleSpawns($$2);
      $$2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PARROT, 40, 1, 2))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.PANDA, 80, 1, 2))
         .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.OCELOT, 2, 1, 1));
      return baseJungle(p_255817_, p_256096_, 0.9F, true, false, true, $$2, Musics.createGameMusic(SoundEvents.MUSIC_BIOME_BAMBOO_JUNGLE));
   }

   private static Biome baseJungle(
      HolderGetter<PlacedFeature> p_285208_,
      HolderGetter<ConfiguredWorldCarver<?>> p_285276_,
      float p_285079_,
      boolean p_285393_,
      boolean p_285109_,
      boolean p_285122_,
      MobSpawnSettings.Builder p_285449_,
      Music p_285440_
   ) {
      BiomeGenerationSettings.Builder $$8 = new BiomeGenerationSettings.Builder(p_285208_, p_285276_);
      globalOverworldGeneration($$8);
      BiomeDefaultFeatures.addDefaultOres($$8);
      BiomeDefaultFeatures.addDefaultSoftDisks($$8);
      if (p_285393_) {
         BiomeDefaultFeatures.addBambooVegetation($$8);
      } else {
         if (p_285122_) {
            BiomeDefaultFeatures.addLightBambooVegetation($$8);
         }

         if (p_285109_) {
            BiomeDefaultFeatures.addSparseJungleTrees($$8);
         } else {
            BiomeDefaultFeatures.addJungleTrees($$8);
         }
      }

      BiomeDefaultFeatures.addWarmFlowers($$8);
      BiomeDefaultFeatures.addJungleGrass($$8);
      BiomeDefaultFeatures.addDefaultMushrooms($$8);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$8);
      BiomeDefaultFeatures.addJungleVines($$8);
      if (p_285109_) {
         BiomeDefaultFeatures.addSparseJungleMelons($$8);
      } else {
         BiomeDefaultFeatures.addJungleMelons($$8);
      }

      return biome(true, 0.95F, p_285079_, p_285449_, $$8, p_285440_);
   }

   public static Biome windsweptHills(HolderGetter<PlacedFeature> p_255703_, HolderGetter<ConfiguredWorldCarver<?>> p_256239_, boolean p_194887_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$3);
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 5, 4, 6));
      BiomeDefaultFeatures.commonSpawns($$3);
      BiomeGenerationSettings.Builder $$4 = new BiomeGenerationSettings.Builder(p_255703_, p_256239_);
      globalOverworldGeneration($$4);
      BiomeDefaultFeatures.addDefaultOres($$4);
      BiomeDefaultFeatures.addDefaultSoftDisks($$4);
      if (p_194887_) {
         BiomeDefaultFeatures.addMountainForestTrees($$4);
      } else {
         BiomeDefaultFeatures.addMountainTrees($$4);
      }

      BiomeDefaultFeatures.addDefaultFlowers($$4);
      BiomeDefaultFeatures.addDefaultGrass($$4);
      BiomeDefaultFeatures.addDefaultMushrooms($$4);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$4);
      BiomeDefaultFeatures.addExtraEmeralds($$4);
      BiomeDefaultFeatures.addInfestedStone($$4);
      return biome(true, 0.2F, 0.3F, $$3, $$4, NORMAL_MUSIC);
   }

   public static Biome desert(HolderGetter<PlacedFeature> p_256064_, HolderGetter<ConfiguredWorldCarver<?>> p_255852_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.desertSpawns($$2);
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_256064_, p_255852_);
      BiomeDefaultFeatures.addFossilDecoration($$3);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      BiomeDefaultFeatures.addDefaultFlowers($$3);
      BiomeDefaultFeatures.addDefaultGrass($$3);
      BiomeDefaultFeatures.addDesertVegetation($$3);
      BiomeDefaultFeatures.addDefaultMushrooms($$3);
      BiomeDefaultFeatures.addDesertExtraVegetation($$3);
      BiomeDefaultFeatures.addDesertExtraDecoration($$3);
      return biome(false, 2.0F, 0.0F, $$2, $$3, Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DESERT));
   }

   public static Biome plains(
      HolderGetter<PlacedFeature> p_256382_, HolderGetter<ConfiguredWorldCarver<?>> p_256173_, boolean p_194882_, boolean p_194883_, boolean p_194884_
   ) {
      MobSpawnSettings.Builder $$5 = new MobSpawnSettings.Builder();
      BiomeGenerationSettings.Builder $$6 = new BiomeGenerationSettings.Builder(p_256382_, p_256173_);
      globalOverworldGeneration($$6);
      if (p_194883_) {
         $$5.creatureGenerationProbability(0.07F);
         BiomeDefaultFeatures.snowySpawns($$5);
         if (p_194884_) {
            $$6.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_SPIKE);
            $$6.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_PATCH);
         }
      } else {
         BiomeDefaultFeatures.plainsSpawns($$5);
         BiomeDefaultFeatures.addPlainGrass($$6);
         if (p_194882_) {
            $$6.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUNFLOWER);
         }
      }

      BiomeDefaultFeatures.addDefaultOres($$6);
      BiomeDefaultFeatures.addDefaultSoftDisks($$6);
      if (p_194883_) {
         BiomeDefaultFeatures.addSnowyTrees($$6);
         BiomeDefaultFeatures.addDefaultFlowers($$6);
         BiomeDefaultFeatures.addDefaultGrass($$6);
      } else {
         BiomeDefaultFeatures.addPlainVegetation($$6);
      }

      BiomeDefaultFeatures.addDefaultMushrooms($$6);
      if (p_194882_) {
         $$6.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE);
         $$6.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
      } else {
         BiomeDefaultFeatures.addDefaultExtraVegetation($$6);
      }

      float $$7 = p_194883_ ? 0.0F : 0.8F;
      return biome(true, $$7, p_194883_ ? 0.5F : 0.4F, $$5, $$6, NORMAL_MUSIC);
   }

   public static Biome mushroomFields(HolderGetter<PlacedFeature> p_255775_, HolderGetter<ConfiguredWorldCarver<?>> p_256480_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.mooshroomSpawns($$2);
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_255775_, p_256480_);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      BiomeDefaultFeatures.addMushroomFieldVegetation($$3);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$3);
      return biome(true, 0.9F, 1.0F, $$2, $$3, NORMAL_MUSIC);
   }

   public static Biome savanna(HolderGetter<PlacedFeature> p_256294_, HolderGetter<ConfiguredWorldCarver<?>> p_256583_, boolean p_194879_, boolean p_194880_) {
      BiomeGenerationSettings.Builder $$4 = new BiomeGenerationSettings.Builder(p_256294_, p_256583_);
      globalOverworldGeneration($$4);
      if (!p_194879_) {
         BiomeDefaultFeatures.addSavannaGrass($$4);
      }

      BiomeDefaultFeatures.addDefaultOres($$4);
      BiomeDefaultFeatures.addDefaultSoftDisks($$4);
      if (p_194879_) {
         BiomeDefaultFeatures.addShatteredSavannaTrees($$4);
         BiomeDefaultFeatures.addDefaultFlowers($$4);
         BiomeDefaultFeatures.addShatteredSavannaGrass($$4);
      } else {
         BiomeDefaultFeatures.addSavannaTrees($$4);
         BiomeDefaultFeatures.addWarmFlowers($$4);
         BiomeDefaultFeatures.addSavannaExtraGrass($$4);
      }

      BiomeDefaultFeatures.addDefaultMushrooms($$4);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$4);
      MobSpawnSettings.Builder $$5 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$5);
      $$5.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.HORSE, 1, 2, 6))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DONKEY, 1, 1, 1))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.ARMADILLO, 10, 2, 3));
      BiomeDefaultFeatures.commonSpawns($$5);
      if (p_194880_) {
         $$5.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.LLAMA, 8, 4, 4));
         $$5.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 8));
      }

      return biome(false, 2.0F, 0.0F, $$5, $$4, NORMAL_MUSIC);
   }

   public static Biome badlands(HolderGetter<PlacedFeature> p_256309_, HolderGetter<ConfiguredWorldCarver<?>> p_256430_, boolean p_194897_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns($$3);
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.ARMADILLO, 6, 1, 2));
      $$3.creatureGenerationProbability(0.03F);
      if (p_194897_) {
         $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 2, 4, 8));
         $$3.creatureGenerationProbability(0.04F);
      }

      BiomeGenerationSettings.Builder $$4 = new BiomeGenerationSettings.Builder(p_256309_, p_256430_);
      globalOverworldGeneration($$4);
      BiomeDefaultFeatures.addDefaultOres($$4);
      BiomeDefaultFeatures.addExtraGold($$4);
      BiomeDefaultFeatures.addDefaultSoftDisks($$4);
      if (p_194897_) {
         BiomeDefaultFeatures.addBadlandsTrees($$4);
      }

      BiomeDefaultFeatures.addBadlandGrass($$4);
      BiomeDefaultFeatures.addDefaultMushrooms($$4);
      BiomeDefaultFeatures.addBadlandExtraVegetation($$4);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(false)
         .temperature(2.0F)
         .downfall(0.0F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(4159204)
               .waterFogColor(329011)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(2.0F))
               .foliageColorOverride(10387789)
               .grassColorOverride(9470285)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_BADLANDS))
               .build()
         )
         .mobSpawnSettings($$3.build())
         .generationSettings($$4.build())
         .build();
   }

   private static Biome baseOcean(MobSpawnSettings.Builder p_194872_, int p_194873_, int p_194874_, BiomeGenerationSettings.Builder p_194875_) {
      return biome(true, 0.5F, 0.5F, p_194873_, p_194874_, null, null, p_194872_, p_194875_, NORMAL_MUSIC);
   }

   private static BiomeGenerationSettings.Builder baseOceanGeneration(HolderGetter<PlacedFeature> p_256289_, HolderGetter<ConfiguredWorldCarver<?>> p_256514_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_256289_, p_256514_);
      globalOverworldGeneration($$2);
      BiomeDefaultFeatures.addDefaultOres($$2);
      BiomeDefaultFeatures.addDefaultSoftDisks($$2);
      BiomeDefaultFeatures.addWaterTrees($$2);
      BiomeDefaultFeatures.addDefaultFlowers($$2);
      BiomeDefaultFeatures.addDefaultGrass($$2);
      BiomeDefaultFeatures.addDefaultMushrooms($$2);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$2);
      return $$2;
   }

   public static Biome coldOcean(HolderGetter<PlacedFeature> p_256141_, HolderGetter<ConfiguredWorldCarver<?>> p_255841_, boolean p_194900_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.oceanSpawns($$3, 3, 4, 15);
      $$3.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5));
      BiomeGenerationSettings.Builder $$4 = baseOceanGeneration(p_256141_, p_255841_);
      $$4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, p_194900_ ? AquaticPlacements.SEAGRASS_DEEP_COLD : AquaticPlacements.SEAGRASS_COLD);
      BiomeDefaultFeatures.addDefaultSeagrass($$4);
      BiomeDefaultFeatures.addColdOceanExtraVegetation($$4);
      return baseOcean($$3, 4020182, 329011, $$4);
   }

   public static Biome ocean(HolderGetter<PlacedFeature> p_256265_, HolderGetter<ConfiguredWorldCarver<?>> p_256537_, boolean p_255752_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.oceanSpawns($$3, 1, 4, 10);
      $$3.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 1, 1, 2));
      BiomeGenerationSettings.Builder $$4 = baseOceanGeneration(p_256265_, p_256537_);
      $$4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, p_255752_ ? AquaticPlacements.SEAGRASS_DEEP : AquaticPlacements.SEAGRASS_NORMAL);
      BiomeDefaultFeatures.addDefaultSeagrass($$4);
      BiomeDefaultFeatures.addColdOceanExtraVegetation($$4);
      return baseOcean($$3, 4159204, 329011, $$4);
   }

   public static Biome lukeWarmOcean(HolderGetter<PlacedFeature> p_255660_, HolderGetter<ConfiguredWorldCarver<?>> p_256231_, boolean p_194906_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      if (p_194906_) {
         BiomeDefaultFeatures.oceanSpawns($$3, 8, 4, 8);
      } else {
         BiomeDefaultFeatures.oceanSpawns($$3, 10, 2, 15);
      }

      $$3.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 5, 1, 3))
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8))
         .addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.DOLPHIN, 2, 1, 2));
      BiomeGenerationSettings.Builder $$4 = baseOceanGeneration(p_255660_, p_256231_);
      $$4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, p_194906_ ? AquaticPlacements.SEAGRASS_DEEP_WARM : AquaticPlacements.SEAGRASS_WARM);
      if (p_194906_) {
         BiomeDefaultFeatures.addDefaultSeagrass($$4);
      }

      BiomeDefaultFeatures.addLukeWarmKelp($$4);
      return baseOcean($$3, 4566514, 267827, $$4);
   }

   public static Biome warmOcean(HolderGetter<PlacedFeature> p_256477_, HolderGetter<ConfiguredWorldCarver<?>> p_256024_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 15, 1, 3));
      BiomeDefaultFeatures.warmOceanSpawns($$2, 10, 4);
      BiomeGenerationSettings.Builder $$3 = baseOceanGeneration(p_256477_, p_256024_)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.WARM_OCEAN_VEGETATION)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM)
         .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
      return baseOcean($$2, 4445678, 270131, $$3);
   }

   public static Biome frozenOcean(HolderGetter<PlacedFeature> p_256482_, HolderGetter<ConfiguredWorldCarver<?>> p_256660_, boolean p_194909_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 1, 1, 4))
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.POLAR_BEAR, 1, 1, 2));
      BiomeDefaultFeatures.commonSpawns($$3);
      $$3.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 5, 1, 1));
      float $$4 = p_194909_ ? 0.5F : 0.0F;
      BiomeGenerationSettings.Builder $$5 = new BiomeGenerationSettings.Builder(p_256482_, p_256660_);
      BiomeDefaultFeatures.addIcebergs($$5);
      globalOverworldGeneration($$5);
      BiomeDefaultFeatures.addBlueIce($$5);
      BiomeDefaultFeatures.addDefaultOres($$5);
      BiomeDefaultFeatures.addDefaultSoftDisks($$5);
      BiomeDefaultFeatures.addWaterTrees($$5);
      BiomeDefaultFeatures.addDefaultFlowers($$5);
      BiomeDefaultFeatures.addDefaultGrass($$5);
      BiomeDefaultFeatures.addDefaultMushrooms($$5);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$5);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature($$4)
         .temperatureAdjustment(Biome.TemperatureModifier.FROZEN)
         .downfall(0.5F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(3750089)
               .waterFogColor(329011)
               .fogColor(12638463)
               .skyColor(calculateSkyColor($$4))
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .build()
         )
         .mobSpawnSettings($$3.build())
         .generationSettings($$5.build())
         .build();
   }

   public static Biome forest(
      HolderGetter<PlacedFeature> p_255788_, HolderGetter<ConfiguredWorldCarver<?>> p_256461_, boolean p_194892_, boolean p_194893_, boolean p_194894_
   ) {
      BiomeGenerationSettings.Builder $$5 = new BiomeGenerationSettings.Builder(p_255788_, p_256461_);
      globalOverworldGeneration($$5);
      Music $$6;
      if (p_194894_) {
         $$6 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FLOWER_FOREST);
         $$5.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FOREST_FLOWERS);
      } else {
         $$6 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FOREST);
         BiomeDefaultFeatures.addForestFlowers($$5);
      }

      BiomeDefaultFeatures.addDefaultOres($$5);
      BiomeDefaultFeatures.addDefaultSoftDisks($$5);
      if (p_194894_) {
         $$5.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_FLOWER_FOREST);
         $$5.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
         BiomeDefaultFeatures.addDefaultGrass($$5);
      } else {
         if (p_194892_) {
            if (p_194893_) {
               BiomeDefaultFeatures.addTallBirchTrees($$5);
            } else {
               BiomeDefaultFeatures.addBirchTrees($$5);
            }
         } else {
            BiomeDefaultFeatures.addOtherBirchTrees($$5);
         }

         BiomeDefaultFeatures.addDefaultFlowers($$5);
         BiomeDefaultFeatures.addForestGrass($$5);
      }

      BiomeDefaultFeatures.addDefaultMushrooms($$5);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$5);
      MobSpawnSettings.Builder $$8 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$8);
      BiomeDefaultFeatures.commonSpawns($$8);
      if (p_194894_) {
         $$8.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
      } else if (!p_194892_) {
         $$8.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));
      }

      float $$9 = p_194892_ ? 0.6F : 0.7F;
      return biome(true, $$9, p_194892_ ? 0.6F : 0.8F, $$8, $$5, $$6);
   }

   public static Biome taiga(HolderGetter<PlacedFeature> p_256177_, HolderGetter<ConfiguredWorldCarver<?>> p_255727_, boolean p_194912_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$3);
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
      BiomeDefaultFeatures.commonSpawns($$3);
      float $$4 = p_194912_ ? -0.5F : 0.25F;
      BiomeGenerationSettings.Builder $$5 = new BiomeGenerationSettings.Builder(p_256177_, p_255727_);
      globalOverworldGeneration($$5);
      BiomeDefaultFeatures.addFerns($$5);
      BiomeDefaultFeatures.addDefaultOres($$5);
      BiomeDefaultFeatures.addDefaultSoftDisks($$5);
      BiomeDefaultFeatures.addTaigaTrees($$5);
      BiomeDefaultFeatures.addDefaultFlowers($$5);
      BiomeDefaultFeatures.addTaigaGrass($$5);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$5);
      if (p_194912_) {
         BiomeDefaultFeatures.addRareBerryBushes($$5);
      } else {
         BiomeDefaultFeatures.addCommonBerryBushes($$5);
      }

      return biome(true, $$4, p_194912_ ? 0.4F : 0.8F, p_194912_ ? 4020182 : 4159204, 329011, null, null, $$3, $$5, NORMAL_MUSIC);
   }

   public static Biome darkForest(HolderGetter<PlacedFeature> p_256140_, HolderGetter<ConfiguredWorldCarver<?>> p_256223_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$2);
      BiomeDefaultFeatures.commonSpawns($$2);
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_256140_, p_256223_);
      globalOverworldGeneration($$3);
      $$3.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.DARK_FOREST_VEGETATION);
      BiomeDefaultFeatures.addForestFlowers($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      BiomeDefaultFeatures.addDefaultFlowers($$3);
      BiomeDefaultFeatures.addForestGrass($$3);
      BiomeDefaultFeatures.addDefaultMushrooms($$3);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$3);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FOREST);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(0.7F)
         .downfall(0.8F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(4159204)
               .waterFogColor(329011)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(0.7F))
               .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic($$4)
               .build()
         )
         .mobSpawnSettings($$2.build())
         .generationSettings($$3.build())
         .build();
   }

   public static Biome swamp(HolderGetter<PlacedFeature> p_256058_, HolderGetter<ConfiguredWorldCarver<?>> p_256016_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.farmAnimals($$2);
      BiomeDefaultFeatures.commonSpawns($$2, 70);
      $$2.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1, 1));
      $$2.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.BOGGED, 30, 4, 4));
      $$2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FROG, 10, 2, 5));
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_256058_, p_256016_);
      BiomeDefaultFeatures.addFossilDecoration($$3);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addSwampClayDisk($$3);
      BiomeDefaultFeatures.addSwampVegetation($$3);
      BiomeDefaultFeatures.addDefaultMushrooms($$3);
      BiomeDefaultFeatures.addSwampExtraVegetation($$3);
      $$3.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SWAMP);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(0.8F)
         .downfall(0.9F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(6388580)
               .waterFogColor(2302743)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(0.8F))
               .foliageColorOverride(6975545)
               .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic($$4)
               .build()
         )
         .mobSpawnSettings($$2.build())
         .generationSettings($$3.build())
         .build();
   }

   public static Biome mangroveSwamp(HolderGetter<PlacedFeature> p_256353_, HolderGetter<ConfiguredWorldCarver<?>> p_256103_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns($$2, 70);
      $$2.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1, 1));
      $$2.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.BOGGED, 30, 4, 4));
      $$2.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FROG, 10, 2, 5));
      $$2.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8));
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_256353_, p_256103_);
      BiomeDefaultFeatures.addFossilDecoration($$3);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addMangroveSwampDisks($$3);
      BiomeDefaultFeatures.addMangroveSwampVegetation($$3);
      $$3.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SWAMP);
      return new Biome.BiomeBuilder()
         .hasPrecipitation(true)
         .temperature(0.8F)
         .downfall(0.9F)
         .specialEffects(
            new BiomeSpecialEffects.Builder()
               .waterColor(3832426)
               .waterFogColor(5077600)
               .fogColor(12638463)
               .skyColor(calculateSkyColor(0.8F))
               .foliageColorOverride(9285927)
               .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
               .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
               .backgroundMusic($$4)
               .build()
         )
         .mobSpawnSettings($$2.build())
         .generationSettings($$3.build())
         .build();
   }

   public static Biome river(HolderGetter<PlacedFeature> p_256613_, HolderGetter<ConfiguredWorldCarver<?>> p_256581_, boolean p_194915_) {
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder()
         .addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 2, 1, 4))
         .addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 5, 1, 5));
      BiomeDefaultFeatures.commonSpawns($$3);
      $$3.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.DROWNED, p_194915_ ? 1 : 100, 1, 1));
      BiomeGenerationSettings.Builder $$4 = new BiomeGenerationSettings.Builder(p_256613_, p_256581_);
      globalOverworldGeneration($$4);
      BiomeDefaultFeatures.addDefaultOres($$4);
      BiomeDefaultFeatures.addDefaultSoftDisks($$4);
      BiomeDefaultFeatures.addWaterTrees($$4);
      BiomeDefaultFeatures.addDefaultFlowers($$4);
      BiomeDefaultFeatures.addDefaultGrass($$4);
      BiomeDefaultFeatures.addDefaultMushrooms($$4);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$4);
      if (!p_194915_) {
         $$4.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
      }

      float $$5 = p_194915_ ? 0.0F : 0.5F;
      return biome(true, $$5, 0.5F, p_194915_ ? 3750089 : 4159204, 329011, null, null, $$3, $$4, NORMAL_MUSIC);
   }

   public static Biome beach(HolderGetter<PlacedFeature> p_256157_, HolderGetter<ConfiguredWorldCarver<?>> p_255712_, boolean p_194889_, boolean p_194890_) {
      MobSpawnSettings.Builder $$4 = new MobSpawnSettings.Builder();
      boolean $$5 = !p_194890_ && !p_194889_;
      if ($$5) {
         $$4.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.TURTLE, 5, 2, 5));
      }

      BiomeDefaultFeatures.commonSpawns($$4);
      BiomeGenerationSettings.Builder $$6 = new BiomeGenerationSettings.Builder(p_256157_, p_255712_);
      globalOverworldGeneration($$6);
      BiomeDefaultFeatures.addDefaultOres($$6);
      BiomeDefaultFeatures.addDefaultSoftDisks($$6);
      BiomeDefaultFeatures.addDefaultFlowers($$6);
      BiomeDefaultFeatures.addDefaultGrass($$6);
      BiomeDefaultFeatures.addDefaultMushrooms($$6);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$6);
      float $$7;
      if (p_194889_) {
         $$7 = 0.05F;
      } else if (p_194890_) {
         $$7 = 0.2F;
      } else {
         $$7 = 0.8F;
      }

      return biome(true, $$7, $$5 ? 0.4F : 0.3F, p_194889_ ? 4020182 : 4159204, 329011, null, null, $$4, $$6, NORMAL_MUSIC);
   }

   public static Biome theVoid(HolderGetter<PlacedFeature> p_256509_, HolderGetter<ConfiguredWorldCarver<?>> p_256544_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_256509_, p_256544_);
      $$2.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.VOID_START_PLATFORM);
      return biome(false, 0.5F, 0.5F, new MobSpawnSettings.Builder(), $$2, NORMAL_MUSIC);
   }

   public static Biome meadowOrCherryGrove(HolderGetter<PlacedFeature> p_273564_, HolderGetter<ConfiguredWorldCarver<?>> p_273374_, boolean p_273710_) {
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_273564_, p_273374_);
      MobSpawnSettings.Builder $$4 = new MobSpawnSettings.Builder();
      $$4.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(p_273710_ ? EntityType.PIG : EntityType.DONKEY, 1, 1, 2))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 2, 6))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 2, 4));
      BiomeDefaultFeatures.commonSpawns($$4);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addPlainGrass($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      if (p_273710_) {
         BiomeDefaultFeatures.addCherryGroveVegetation($$3);
      } else {
         BiomeDefaultFeatures.addMeadowVegetation($$3);
      }

      BiomeDefaultFeatures.addExtraEmeralds($$3);
      BiomeDefaultFeatures.addInfestedStone($$3);
      Music $$5 = Musics.createGameMusic(p_273710_ ? SoundEvents.MUSIC_BIOME_CHERRY_GROVE : SoundEvents.MUSIC_BIOME_MEADOW);
      return p_273710_
         ? biome(true, 0.5F, 0.8F, 6141935, 6141935, 11983713, 11983713, $$4, $$3, $$5)
         : biome(true, 0.5F, 0.8F, 937679, 329011, null, null, $$4, $$3, $$5);
   }

   public static Biome frozenPeaks(HolderGetter<PlacedFeature> p_255713_, HolderGetter<ConfiguredWorldCarver<?>> p_256092_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_255713_, p_256092_);
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
      BiomeDefaultFeatures.commonSpawns($$3);
      globalOverworldGeneration($$2);
      BiomeDefaultFeatures.addFrozenSprings($$2);
      BiomeDefaultFeatures.addDefaultOres($$2);
      BiomeDefaultFeatures.addDefaultSoftDisks($$2);
      BiomeDefaultFeatures.addExtraEmeralds($$2);
      BiomeDefaultFeatures.addInfestedStone($$2);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FROZEN_PEAKS);
      return biome(true, -0.7F, 0.9F, $$3, $$2, $$4);
   }

   public static Biome jaggedPeaks(HolderGetter<PlacedFeature> p_256512_, HolderGetter<ConfiguredWorldCarver<?>> p_255908_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_256512_, p_255908_);
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
      BiomeDefaultFeatures.commonSpawns($$3);
      globalOverworldGeneration($$2);
      BiomeDefaultFeatures.addFrozenSprings($$2);
      BiomeDefaultFeatures.addDefaultOres($$2);
      BiomeDefaultFeatures.addDefaultSoftDisks($$2);
      BiomeDefaultFeatures.addExtraEmeralds($$2);
      BiomeDefaultFeatures.addInfestedStone($$2);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JAGGED_PEAKS);
      return biome(true, -0.7F, 0.9F, $$3, $$2, $$4);
   }

   public static Biome stonyPeaks(HolderGetter<PlacedFeature> p_256490_, HolderGetter<ConfiguredWorldCarver<?>> p_255694_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_256490_, p_255694_);
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.commonSpawns($$3);
      globalOverworldGeneration($$2);
      BiomeDefaultFeatures.addDefaultOres($$2);
      BiomeDefaultFeatures.addDefaultSoftDisks($$2);
      BiomeDefaultFeatures.addExtraEmeralds($$2);
      BiomeDefaultFeatures.addInfestedStone($$2);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_STONY_PEAKS);
      return biome(true, 1.0F, 0.3F, $$3, $$2, $$4);
   }

   public static Biome snowySlopes(HolderGetter<PlacedFeature> p_255927_, HolderGetter<ConfiguredWorldCarver<?>> p_255982_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_255927_, p_255982_);
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3));
      BiomeDefaultFeatures.commonSpawns($$3);
      globalOverworldGeneration($$2);
      BiomeDefaultFeatures.addFrozenSprings($$2);
      BiomeDefaultFeatures.addDefaultOres($$2);
      BiomeDefaultFeatures.addDefaultSoftDisks($$2);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$2);
      BiomeDefaultFeatures.addExtraEmeralds($$2);
      BiomeDefaultFeatures.addInfestedStone($$2);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SNOWY_SLOPES);
      return biome(true, -0.3F, 0.9F, $$3, $$2, $$4);
   }

   public static Biome grove(HolderGetter<PlacedFeature> p_256094_, HolderGetter<ConfiguredWorldCarver<?>> p_256431_) {
      BiomeGenerationSettings.Builder $$2 = new BiomeGenerationSettings.Builder(p_256094_, p_256431_);
      MobSpawnSettings.Builder $$3 = new MobSpawnSettings.Builder();
      $$3.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 1, 1, 1))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 8, 2, 3))
         .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 4, 2, 4));
      BiomeDefaultFeatures.commonSpawns($$3);
      globalOverworldGeneration($$2);
      BiomeDefaultFeatures.addFrozenSprings($$2);
      BiomeDefaultFeatures.addDefaultOres($$2);
      BiomeDefaultFeatures.addDefaultSoftDisks($$2);
      BiomeDefaultFeatures.addGroveTrees($$2);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$2);
      BiomeDefaultFeatures.addExtraEmeralds($$2);
      BiomeDefaultFeatures.addInfestedStone($$2);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_GROVE);
      return biome(true, -0.2F, 0.8F, $$3, $$2, $$4);
   }

   public static Biome lushCaves(HolderGetter<PlacedFeature> p_255944_, HolderGetter<ConfiguredWorldCarver<?>> p_255654_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      $$2.addSpawn(MobCategory.AXOLOTLS, new MobSpawnSettings.SpawnerData(EntityType.AXOLOTL, 10, 4, 6));
      $$2.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 25, 8, 8));
      BiomeDefaultFeatures.commonSpawns($$2);
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_255944_, p_255654_);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addPlainGrass($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addLushCavesSpecialOres($$3);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      BiomeDefaultFeatures.addLushCavesVegetationFeatures($$3);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_LUSH_CAVES);
      return biome(true, 0.5F, 0.5F, $$2, $$3, $$4);
   }

   public static Biome dripstoneCaves(HolderGetter<PlacedFeature> p_256253_, HolderGetter<ConfiguredWorldCarver<?>> p_255644_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeDefaultFeatures.dripstoneCavesSpawns($$2);
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_256253_, p_255644_);
      globalOverworldGeneration($$3);
      BiomeDefaultFeatures.addPlainGrass($$3);
      BiomeDefaultFeatures.addDefaultOres($$3, true);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      BiomeDefaultFeatures.addPlainVegetation($$3);
      BiomeDefaultFeatures.addDefaultMushrooms($$3);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$3);
      BiomeDefaultFeatures.addDripstone($$3);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES);
      return biome(true, 0.8F, 0.4F, $$2, $$3, $$4);
   }

   public static Biome deepDark(HolderGetter<PlacedFeature> p_256073_, HolderGetter<ConfiguredWorldCarver<?>> p_256212_) {
      MobSpawnSettings.Builder $$2 = new MobSpawnSettings.Builder();
      BiomeGenerationSettings.Builder $$3 = new BiomeGenerationSettings.Builder(p_256073_, p_256212_);
      $$3.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE);
      $$3.addCarver(GenerationStep.Carving.AIR, Carvers.CAVE_EXTRA_UNDERGROUND);
      $$3.addCarver(GenerationStep.Carving.AIR, Carvers.CANYON);
      BiomeDefaultFeatures.addDefaultCrystalFormations($$3);
      BiomeDefaultFeatures.addDefaultMonsterRoom($$3);
      BiomeDefaultFeatures.addDefaultUndergroundVariety($$3);
      BiomeDefaultFeatures.addSurfaceFreezing($$3);
      BiomeDefaultFeatures.addPlainGrass($$3);
      BiomeDefaultFeatures.addDefaultOres($$3);
      BiomeDefaultFeatures.addDefaultSoftDisks($$3);
      BiomeDefaultFeatures.addPlainVegetation($$3);
      BiomeDefaultFeatures.addDefaultMushrooms($$3);
      BiomeDefaultFeatures.addDefaultExtraVegetation($$3);
      BiomeDefaultFeatures.addSculk($$3);
      Music $$4 = Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DEEP_DARK);
      return biome(true, 0.8F, 0.4F, $$2, $$3, $$4);
   }
}
