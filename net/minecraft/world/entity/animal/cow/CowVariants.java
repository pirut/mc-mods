package net.minecraft.world.entity.animal.cow;

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

public class CowVariants {
   public static final ResourceKey<CowVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<CowVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<CowVariant> COLD = createKey(TemperatureVariants.COLD);
   public static final ResourceKey<CowVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<CowVariant> createKey(Identifier p_478077_) {
      return ResourceKey.create(Registries.COW_VARIANT, p_478077_);
   }

   public static void bootstrap(BootstrapContext<CowVariant> p_478787_) {
      register(p_478787_, TEMPERATE, CowVariant.ModelType.NORMAL, "temperate_cow", SpawnPrioritySelectors.fallback(0));
      register(p_478787_, WARM, CowVariant.ModelType.WARM, "warm_cow", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(p_478787_, COLD, CowVariant.ModelType.COLD, "cold_cow", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      BootstrapContext<CowVariant> p_478848_, ResourceKey<CowVariant> p_482066_, CowVariant.ModelType p_481111_, String p_482166_, TagKey<Biome> p_479628_
   ) {
      HolderSet<Biome> holderset = p_478848_.lookup(Registries.BIOME).getOrThrow(p_479628_);
      register(p_478848_, p_482066_, p_481111_, p_482166_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<CowVariant> p_480736_,
      ResourceKey<CowVariant> p_479430_,
      CowVariant.ModelType p_478970_,
      String p_477917_,
      SpawnPrioritySelectors p_478228_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/cow/" + p_477917_);
      p_480736_.register(p_479430_, new CowVariant(new ModelAndTexture<>(p_478970_, identifier), p_478228_));
   }
}
