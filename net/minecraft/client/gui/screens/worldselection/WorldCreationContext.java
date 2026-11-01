package net.minecraft.client.gui.screens.worldselection;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;

@Environment(EnvType.CLIENT)
public record WorldCreationContext(
   WorldOptions options,
   Registry<LevelStem> datapackDimensions,
   WorldDimensions selectedDimensions,
   LayeredRegistryAccess<RegistryLayer> worldgenRegistries,
   ReloadableServerResources dataPackResources,
   WorldDataConfiguration dataConfiguration,
   InitialWorldCreationOptions initialWorldCreationOptions
) {
   public WorldCreationContext(
      WorldGenSettings worldGenSettings,
      LayeredRegistryAccess<RegistryLayer> worldGenRegistries,
      ReloadableServerResources dataPackResources,
      WorldDataConfiguration dataConfiguration
   ) {
      this(
         worldGenSettings.options(),
         worldGenSettings.dimensions(),
         worldGenRegistries,
         dataPackResources,
         dataConfiguration,
         new InitialWorldCreationOptions(WorldCreationUiState.SelectedGameMode.SURVIVAL, GameRuleMap.of(), null)
      );
   }

   public WorldCreationContext(
      WorldOptions worldOptions,
      WorldDimensions worldDimensions,
      LayeredRegistryAccess<RegistryLayer> worldGenRegistries,
      ReloadableServerResources dataPackResources,
      WorldDataConfiguration dataConfiguration,
      InitialWorldCreationOptions initialWorldCreationOptions
   ) {
      this(
         worldOptions,
         worldGenRegistries.getLayer(RegistryLayer.DIMENSIONS).lookupOrThrow(Registries.LEVEL_STEM),
         worldDimensions,
         worldGenRegistries.replaceFrom(RegistryLayer.DIMENSIONS),
         dataPackResources,
         dataConfiguration,
         initialWorldCreationOptions
      );
   }

   public WorldCreationContext withSettings(WorldOptions options, WorldDimensions selectedDimensions) {
      return new WorldCreationContext(
         options,
         this.datapackDimensions,
         selectedDimensions,
         this.worldgenRegistries,
         this.dataPackResources,
         this.dataConfiguration,
         this.initialWorldCreationOptions
      );
   }

   public WorldCreationContext withOptions(WorldCreationContext.OptionsModifier optionsModifier) {
      return new WorldCreationContext(
         optionsModifier.apply(this.options),
         this.datapackDimensions,
         this.selectedDimensions,
         this.worldgenRegistries,
         this.dataPackResources,
         this.dataConfiguration,
         this.initialWorldCreationOptions
      );
   }

   public WorldCreationContext withDimensions(WorldCreationContext.DimensionsUpdater dimensionsUpdater) {
      return new WorldCreationContext(
         this.options,
         this.datapackDimensions,
         dimensionsUpdater.apply(this.worldgenLoadContext(), this.selectedDimensions),
         this.worldgenRegistries,
         this.dataPackResources,
         this.dataConfiguration,
         this.initialWorldCreationOptions
      );
   }

   public RegistryAccess.Frozen worldgenLoadContext() {
      return this.worldgenRegistries.compositeAccess();
   }

   public void validate() {
      for (LevelStem levelStem : this.datapackDimensions()) {
         levelStem.generator().validate();
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface DimensionsUpdater extends BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> {
   }

   @Environment(EnvType.CLIENT)
   public interface OptionsModifier extends UnaryOperator<WorldOptions> {
   }
}
