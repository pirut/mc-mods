package net.minecraft.world.entity.animal.sheep;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;

public class SheepColorSpawnRules {
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration TEMPERATE_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(
      weighted(
         builder()
            .add(single(DyeColor.BLACK), 5)
            .add(single(DyeColor.GRAY), 5)
            .add(single(DyeColor.LIGHT_GRAY), 5)
            .add(single(DyeColor.BROWN), 3)
            .add(commonColors(DyeColor.WHITE), 82)
            .build()
      )
   );
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration WARM_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(
      weighted(
         builder()
            .add(single(DyeColor.GRAY), 5)
            .add(single(DyeColor.LIGHT_GRAY), 5)
            .add(single(DyeColor.WHITE), 5)
            .add(single(DyeColor.BLACK), 3)
            .add(commonColors(DyeColor.BROWN), 82)
            .build()
      )
   );
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration COLD_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(
      weighted(
         builder()
            .add(single(DyeColor.LIGHT_GRAY), 5)
            .add(single(DyeColor.GRAY), 5)
            .add(single(DyeColor.WHITE), 5)
            .add(single(DyeColor.BROWN), 3)
            .add(commonColors(DyeColor.BLACK), 82)
            .build()
      )
   );

   private static SheepColorSpawnRules.SheepColorProvider commonColors(DyeColor p_405584_) {
      return weighted(builder().add(single(p_405584_), 499).add(single(DyeColor.PINK), 1).build());
   }

   public static DyeColor getSheepColor(Holder<Biome> p_404960_, RandomSource p_405409_) {
      SheepColorSpawnRules.SheepColorSpawnConfiguration sheepcolorspawnrules$sheepcolorspawnconfiguration = getSheepColorConfiguration(p_404960_);
      return sheepcolorspawnrules$sheepcolorspawnconfiguration.colors().get(p_405409_);
   }

   private static SheepColorSpawnRules.SheepColorSpawnConfiguration getSheepColorConfiguration(Holder<Biome> p_405512_) {
      if (p_405512_.is(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) {
         return WARM_SPAWN_CONFIGURATION;
      } else {
         return p_405512_.is(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS) ? COLD_SPAWN_CONFIGURATION : TEMPERATE_SPAWN_CONFIGURATION;
      }
   }

   private static SheepColorSpawnRules.SheepColorProvider weighted(WeightedList<SheepColorSpawnRules.SheepColorProvider> p_405728_) {
      if (p_405728_.isEmpty()) {
         throw new IllegalArgumentException("List must be non-empty");
      } else {
         return p_404682_ -> p_405728_.getRandomOrThrow(p_404682_).get(p_404682_);
      }
   }

   private static SheepColorSpawnRules.SheepColorProvider single(DyeColor p_405704_) {
      return p_405197_ -> p_405704_;
   }

   private static WeightedList.Builder<SheepColorSpawnRules.SheepColorProvider> builder() {
      return WeightedList.builder();
   }

   @FunctionalInterface
   interface SheepColorProvider {
      DyeColor get(RandomSource var1);
   }

   record SheepColorSpawnConfiguration(SheepColorSpawnRules.SheepColorProvider colors) {
   }
}
