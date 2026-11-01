package net.neoforged.neoforge.common.world;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

public class ModifiableBiomeInfo {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ModifiableBiomeInfo.BiomeInfo originalBiomeInfo;
   @Nullable
   private ModifiableBiomeInfo.BiomeInfo modifiedBiomeInfo = null;

   public ModifiableBiomeInfo(ModifiableBiomeInfo.BiomeInfo originalBiomeInfo) {
      this.originalBiomeInfo = originalBiomeInfo;
   }

   public ModifiableBiomeInfo.BiomeInfo get() {
      return this.modifiedBiomeInfo == null ? this.originalBiomeInfo : this.modifiedBiomeInfo;
   }

   public ModifiableBiomeInfo.BiomeInfo getOriginalBiomeInfo() {
      return this.originalBiomeInfo;
   }

   @Nullable
   public ModifiableBiomeInfo.BiomeInfo getModifiedBiomeInfo() {
      return this.modifiedBiomeInfo;
   }

   @Internal
   public boolean applyBiomeModifiers(Holder<Biome> biome, List<BiomeModifier> biomeModifiers, RegistryAccess registryAccess) {
      if (this.modifiedBiomeInfo != null) {
         return true;
      } else {
         ModifiableBiomeInfo.BiomeInfo original = this.getOriginalBiomeInfo();
         ModifiableBiomeInfo.BiomeInfo.Builder builder = ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(original);

         for (BiomeModifier.Phase phase : BiomeModifier.Phase.values()) {
            for (BiomeModifier modifier : biomeModifiers) {
               modifier.modify(biome, phase, builder);
            }
         }

         DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
         JsonElement originalJson = (JsonElement)Biome.NETWORK_CODEC.encodeStart(ops, biome.value()).result().orElse(null);
         this.modifiedBiomeInfo = builder.build();
         JsonElement modifiedJson = (JsonElement)Biome.NETWORK_CODEC.encodeStart(ops, biome.value()).result().orElse(null);
         if (originalJson != null && modifiedJson != null) {
            return !originalJson.equals(modifiedJson);
         } else {
            LOGGER.warn("Failed to determine whether biome {} was modified", biome);
            return true;
         }
      }
   }

   public record BiomeInfo(
      Biome.ClimateSettings climateSettings, BiomeSpecialEffects effects, BiomeGenerationSettings generationSettings, MobSpawnSettings mobSpawnSettings
   ) {
      public static class Builder {
         private ClimateSettingsBuilder climateSettings;
         private BiomeSpecialEffectsBuilder effects;
         private BiomeGenerationSettingsBuilder generationSettings;
         private MobSpawnSettingsBuilder mobSpawnSettings;

         public static ModifiableBiomeInfo.BiomeInfo.Builder copyOf(ModifiableBiomeInfo.BiomeInfo original) {
            ClimateSettingsBuilder climateBuilder = ClimateSettingsBuilder.copyOf(original.climateSettings());
            BiomeSpecialEffectsBuilder effectsBuilder = BiomeSpecialEffectsBuilder.copyOf(original.effects());
            BiomeGenerationSettingsBuilder generationBuilder = new BiomeGenerationSettingsBuilder(original.generationSettings());
            MobSpawnSettingsBuilder mobSpawnBuilder = new MobSpawnSettingsBuilder(original.mobSpawnSettings());
            return new ModifiableBiomeInfo.BiomeInfo.Builder(climateBuilder, effectsBuilder, generationBuilder, mobSpawnBuilder);
         }

         private Builder(
            ClimateSettingsBuilder climateSettings,
            BiomeSpecialEffectsBuilder effects,
            BiomeGenerationSettingsBuilder generationSettings,
            MobSpawnSettingsBuilder mobSpawnSettings
         ) {
            this.climateSettings = climateSettings;
            this.effects = effects;
            this.generationSettings = generationSettings;
            this.mobSpawnSettings = mobSpawnSettings;
         }

         public ModifiableBiomeInfo.BiomeInfo build() {
            return new ModifiableBiomeInfo.BiomeInfo(
               this.climateSettings.build(), this.effects.build(), this.generationSettings.build(), this.mobSpawnSettings.build()
            );
         }

         public ClimateSettingsBuilder getClimateSettings() {
            return this.climateSettings;
         }

         public BiomeSpecialEffectsBuilder getSpecialEffects() {
            return this.effects;
         }

         public BiomeGenerationSettingsBuilder getGenerationSettings() {
            return this.generationSettings;
         }

         public MobSpawnSettingsBuilder getMobSpawnSettings() {
            return this.mobSpawnSettings;
         }
      }
   }
}
