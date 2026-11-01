package net.minecraft.world.entity.animal.frog;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public interface FrogVariants {
   ResourceKey<FrogVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   ResourceKey<FrogVariant> WARM = createKey(TemperatureVariants.WARM);
   ResourceKey<FrogVariant> COLD = createKey(TemperatureVariants.COLD);

   private static ResourceKey<FrogVariant> createKey(Identifier p_469829_) {
      return ResourceKey.create(Registries.FROG_VARIANT, p_469829_);
   }

   static void bootstrap(BootstrapContext<FrogVariant> p_399856_) {
      register(p_399856_, TEMPERATE, "entity/frog/temperate_frog", SpawnPrioritySelectors.fallback(0));
      register(p_399856_, WARM, "entity/frog/warm_frog", BiomeTags.SPAWNS_WARM_VARIANT_FROGS);
      register(p_399856_, COLD, "entity/frog/cold_frog", BiomeTags.SPAWNS_COLD_VARIANT_FROGS);
   }

   private static void register(BootstrapContext<FrogVariant> p_400026_, ResourceKey<FrogVariant> p_399464_, String p_399971_, TagKey<Biome> p_399473_) {
      HolderSet<Biome> holderset = p_400026_.lookup(Registries.BIOME).getOrThrow(p_399473_);
      register(p_400026_, p_399464_, p_399971_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(BootstrapContext<FrogVariant> p_399696_, ResourceKey<FrogVariant> p_399649_, String p_400239_, SpawnPrioritySelectors p_399940_) {
      p_399696_.register(p_399649_, new FrogVariant(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(p_400239_)), p_399940_));
   }
}
