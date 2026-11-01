package net.minecraft.world.entity.animal.nautilus;

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

public class ZombieNautilusVariants {
   public static final ResourceKey<ZombieNautilusVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<ZombieNautilusVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<ZombieNautilusVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<ZombieNautilusVariant> createKey(Identifier p_479359_) {
      return ResourceKey.create(Registries.ZOMBIE_NAUTILUS_VARIANT, p_479359_);
   }

   public static void bootstrap(BootstrapContext<ZombieNautilusVariant> p_480472_) {
      register(p_480472_, TEMPERATE, ZombieNautilusVariant.ModelType.NORMAL, "zombie_nautilus", SpawnPrioritySelectors.fallback(0));
      register(p_480472_, WARM, ZombieNautilusVariant.ModelType.WARM, "zombie_nautilus_coral", BiomeTags.SPAWNS_CORAL_VARIANT_ZOMBIE_NAUTILUS);
   }

   private static void register(
      BootstrapContext<ZombieNautilusVariant> p_480723_,
      ResourceKey<ZombieNautilusVariant> p_478839_,
      ZombieNautilusVariant.ModelType p_480116_,
      String p_480491_,
      TagKey<Biome> p_479670_
   ) {
      HolderSet<Biome> holderset = p_480723_.lookup(Registries.BIOME).getOrThrow(p_479670_);
      register(p_480723_, p_478839_, p_480116_, p_480491_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<ZombieNautilusVariant> p_481244_,
      ResourceKey<ZombieNautilusVariant> p_481857_,
      ZombieNautilusVariant.ModelType p_479671_,
      String p_481050_,
      SpawnPrioritySelectors p_480664_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/nautilus/" + p_481050_);
      p_481244_.register(p_481857_, new ZombieNautilusVariant(new ModelAndTexture<>(p_479671_, identifier), p_480664_));
   }
}
