package net.minecraft.world.entity.animal.pig;

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

public class PigVariants {
   public static final ResourceKey<PigVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<PigVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<PigVariant> COLD = createKey(TemperatureVariants.COLD);
   public static final ResourceKey<PigVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<PigVariant> createKey(Identifier p_479629_) {
      return ResourceKey.create(Registries.PIG_VARIANT, p_479629_);
   }

   public static void bootstrap(BootstrapContext<PigVariant> p_478957_) {
      register(p_478957_, TEMPERATE, PigVariant.ModelType.NORMAL, "temperate_pig", SpawnPrioritySelectors.fallback(0));
      register(p_478957_, WARM, PigVariant.ModelType.NORMAL, "warm_pig", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(p_478957_, COLD, PigVariant.ModelType.COLD, "cold_pig", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      BootstrapContext<PigVariant> p_478576_, ResourceKey<PigVariant> p_480485_, PigVariant.ModelType p_482188_, String p_478497_, TagKey<Biome> p_478907_
   ) {
      HolderSet<Biome> holderset = p_478576_.lookup(Registries.BIOME).getOrThrow(p_478907_);
      register(p_478576_, p_480485_, p_482188_, p_478497_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<PigVariant> p_479169_,
      ResourceKey<PigVariant> p_478610_,
      PigVariant.ModelType p_481839_,
      String p_478287_,
      SpawnPrioritySelectors p_481491_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/pig/" + p_478287_);
      p_479169_.register(p_478610_, new PigVariant(new ModelAndTexture<>(p_481839_, identifier), p_481491_));
   }
}
