package net.minecraft.world.entity.animal.chicken;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public class ChickenVariants {
   public static final ResourceKey<ChickenVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<ChickenVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<ChickenVariant> COLD = createKey(TemperatureVariants.COLD);
   public static final ResourceKey<ChickenVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<ChickenVariant> createKey(Identifier p_478919_) {
      return ResourceKey.create(Registries.CHICKEN_VARIANT, p_478919_);
   }

   public static void bootstrap(BootstrapContext<ChickenVariant> p_480634_) {
      register(p_480634_, TEMPERATE, ChickenVariant.ModelType.NORMAL, "temperate_chicken", SpawnPrioritySelectors.fallback(0));
      register(p_480634_, WARM, ChickenVariant.ModelType.NORMAL, "warm_chicken", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(p_480634_, COLD, ChickenVariant.ModelType.COLD, "cold_chicken", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      BootstrapContext<ChickenVariant> p_480867_,
      ResourceKey<ChickenVariant> p_479619_,
      ChickenVariant.ModelType p_481665_,
      String p_477975_,
      TagKey<Biome> p_480843_
   ) {
      HolderSet<Biome> holderset = p_480867_.lookup(Registries.BIOME).getOrThrow(p_480843_);
      register(p_480867_, p_479619_, p_481665_, p_477975_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<ChickenVariant> p_478201_,
      ResourceKey<ChickenVariant> p_480969_,
      ChickenVariant.ModelType p_479770_,
      String p_479431_,
      SpawnPrioritySelectors p_478802_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/chicken/" + p_479431_);
      p_478201_.register(p_480969_, new ChickenVariant(new ModelAndTexture<>(p_479770_, identifier), p_478802_));
   }
}
