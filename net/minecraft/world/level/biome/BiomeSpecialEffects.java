package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;

public record BiomeSpecialEffects(
   int waterColor,
   Optional<Integer> foliageColorOverride,
   Optional<Integer> dryFoliageColorOverride,
   Optional<Integer> grassColorOverride,
   BiomeSpecialEffects.GrassColorModifier grassColorModifier
) {
   public static final Codec<BiomeSpecialEffects> CODEC = RecordCodecBuilder.create(
      p_460565_ -> p_460565_.group(
            ExtraCodecs.STRING_RGB_COLOR.fieldOf("water_color").forGetter(BiomeSpecialEffects::waterColor),
            ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("foliage_color").forGetter(BiomeSpecialEffects::foliageColorOverride),
            ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("dry_foliage_color").forGetter(BiomeSpecialEffects::dryFoliageColorOverride),
            ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("grass_color").forGetter(BiomeSpecialEffects::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC
               .optionalFieldOf("grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.NONE)
               .forGetter(BiomeSpecialEffects::grassColorModifier)
         )
         .apply(p_460565_, BiomeSpecialEffects::new)
   );

   public static class Builder {
      protected OptionalInt waterColor = OptionalInt.empty();
      protected Optional<Integer> foliageColorOverride = Optional.empty();
      protected Optional<Integer> dryFoliageColorOverride = Optional.empty();
      protected Optional<Integer> grassColorOverride = Optional.empty();
      protected BiomeSpecialEffects.GrassColorModifier grassColorModifier = BiomeSpecialEffects.GrassColorModifier.NONE;

      public BiomeSpecialEffects.Builder waterColor(int p_48035_) {
         this.waterColor = OptionalInt.of(p_48035_);
         return this;
      }

      public BiomeSpecialEffects.Builder foliageColorOverride(int p_48044_) {
         this.foliageColorOverride = Optional.of(p_48044_);
         return this;
      }

      public BiomeSpecialEffects.Builder dryFoliageColorOverride(int p_406250_) {
         this.dryFoliageColorOverride = Optional.of(p_406250_);
         return this;
      }

      public BiomeSpecialEffects.Builder grassColorOverride(int p_48046_) {
         this.grassColorOverride = Optional.of(p_48046_);
         return this;
      }

      public BiomeSpecialEffects.Builder grassColorModifier(BiomeSpecialEffects.GrassColorModifier p_48032_) {
         this.grassColorModifier = p_48032_;
         return this;
      }

      public BiomeSpecialEffects build() {
         return new BiomeSpecialEffects(
            this.waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")),
            this.foliageColorOverride,
            this.dryFoliageColorOverride,
            this.grassColorOverride,
            this.grassColorModifier
         );
      }
   }

   @NetworkedEnum(NetworkCheck.CLIENTBOUND)
   @NamedEnum
   public static enum GrassColorModifier implements StringRepresentable, IExtensibleEnum {
      NONE("none") {
         @Override
         public int modifyColor(double p_48081_, double p_48082_, int p_48083_) {
            return p_48083_;
         }
      },
      DARK_FOREST("dark_forest") {
         @Override
         public int modifyColor(double p_48089_, double p_48090_, int p_48091_) {
            return (p_48091_ & 16711422) + 2634762 >> 1;
         }
      },
      SWAMP("swamp") {
         @Override
         public int modifyColor(double p_48097_, double p_48098_, int p_48099_) {
            double d0 = Biome.BIOME_INFO_NOISE.getValue(p_48097_ * 0.0225, p_48098_ * 0.0225, false);
            return d0 < -0.1 ? 5011004 : 6975545;
         }
      };

      private final String name;
      private final BiomeSpecialEffects.GrassColorModifier.ColorModifier delegate;
      public static final Codec<BiomeSpecialEffects.GrassColorModifier> CODEC = StringRepresentable.fromEnum(BiomeSpecialEffects.GrassColorModifier::values);

      public int modifyColor(double p_48065_, double p_48066_, int p_48067_) {
         return this.delegate.modifyGrassColor(p_48065_, p_48066_, p_48067_);
      }

      @ReservedConstructor
      private GrassColorModifier(String p_48058_) {
         this.name = p_48058_;
         this.delegate = null;
      }

      private GrassColorModifier(String name, BiomeSpecialEffects.GrassColorModifier.ColorModifier delegate) {
         this.name = name;
         this.delegate = delegate;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public static ExtensionInfo getExtensionInfo() {
         return ExtensionInfo.nonExtended(BiomeSpecialEffects.GrassColorModifier.class);
      }

      @FunctionalInterface
      public interface ColorModifier {
         int modifyGrassColor(double var1, double var3, int var5);
      }
   }
}
