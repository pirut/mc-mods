package net.neoforged.neoforge.common.world;

import java.util.Optional;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

public class BiomeSpecialEffectsBuilder extends BiomeSpecialEffects.Builder {
   public static BiomeSpecialEffectsBuilder copyOf(BiomeSpecialEffects baseEffects) {
      BiomeSpecialEffectsBuilder builder = create(baseEffects.waterColor());
      builder.grassColorModifier = baseEffects.grassColorModifier();
      baseEffects.foliageColorOverride().ifPresent(builder::foliageColorOverride);
      baseEffects.dryFoliageColorOverride().ifPresent(builder::dryFoliageColorOverride);
      baseEffects.grassColorOverride().ifPresent(builder::grassColorOverride);
      return builder;
   }

   public static BiomeSpecialEffectsBuilder create(int waterColor) {
      return new BiomeSpecialEffectsBuilder(waterColor);
   }

   protected BiomeSpecialEffectsBuilder(int waterColor) {
      this.waterColor(waterColor);
   }

   public int waterColor() {
      return this.waterColor.getAsInt();
   }

   public BiomeSpecialEffects.GrassColorModifier getGrassColorModifier() {
      return this.grassColorModifier;
   }

   public Optional<Integer> getFoliageColorOverride() {
      return this.foliageColorOverride;
   }

   public Optional<Integer> getDryFoliageColorOverride() {
      return this.dryFoliageColorOverride;
   }

   public Optional<Integer> getGrassColorOverride() {
      return this.grassColorOverride;
   }
}
