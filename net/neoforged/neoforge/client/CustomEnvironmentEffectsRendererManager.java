package net.neoforged.neoforge.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.common.world.NeoForgeEnvironmentAttributes;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public final class CustomEnvironmentEffectsRendererManager {
   @Nullable
   private static Map<Identifier, CustomCloudsRenderer> CUSTOM_CLOUD_RENDERERS;
   @Nullable
   private static Map<Identifier, CustomSkyboxRenderer> CUSTOM_SKYBOX_RENDERERS;
   @Nullable
   private static Map<Identifier, CustomWeatherEffectRenderer> CUSTOM_WEATHER_EFFECT_RENDERERS;

   @Nullable
   public static CustomCloudsRenderer getCustomCloudsRenderer(Identifier id) {
      return NeoForgeEnvironmentAttributes.DEFAULT_CUSTOM_CLOUDS.equals(id) ? null : Objects.requireNonNull(CUSTOM_CLOUD_RENDERERS).get(id);
   }

   @Nullable
   public static CustomCloudsRenderer getCustomCloudsRenderer(Level level, Vec3 position) {
      Identifier id = level.environmentAttributes().getValue(NeoForgeEnvironmentAttributes.CUSTOM_CLOUDS, position);
      return getCustomCloudsRenderer(id);
   }

   @Nullable
   public static CustomSkyboxRenderer getCustomSkyboxRenderer(Identifier id) {
      return NeoForgeEnvironmentAttributes.DEFAULT_CUSTOM_SKYBOX.equals(id) ? null : Objects.requireNonNull(CUSTOM_SKYBOX_RENDERERS).get(id);
   }

   @Nullable
   public static CustomSkyboxRenderer getCustomSkyboxRenderer(Level level, Vec3 position) {
      Identifier id = level.environmentAttributes().getValue(NeoForgeEnvironmentAttributes.CUSTOM_SKYBOX, position);
      return getCustomSkyboxRenderer(id);
   }

   @Nullable
   public static CustomWeatherEffectRenderer getCustomWeatherEffectRenderer(Identifier id) {
      return NeoForgeEnvironmentAttributes.DEFAULT_CUSTOM_WEATHER_EFFECTS.equals(id) ? null : Objects.requireNonNull(CUSTOM_WEATHER_EFFECT_RENDERERS).get(id);
   }

   @Nullable
   public static CustomWeatherEffectRenderer getCustomWeatherEffectRenderer(Level level, Vec3 position) {
      Identifier id = level.environmentAttributes().getValue(NeoForgeEnvironmentAttributes.CUSTOM_WEATHER_EFFECTS, position);
      return getCustomWeatherEffectRenderer(id);
   }

   @Internal
   public static void init() {
      if (CUSTOM_CLOUD_RENDERERS != null) {
         throw new IllegalStateException("Already initialized.");
      } else {
         HashMap<Identifier, CustomCloudsRenderer> customCloudRenderers = new HashMap<>();
         HashMap<Identifier, CustomSkyboxRenderer> customSkyboxRenderers = new HashMap<>();
         HashMap<Identifier, CustomWeatherEffectRenderer> customWeatherEffectRenderers = new HashMap<>();
         ModLoader.postEventWrapContainerInModOrder(
            new RegisterCustomEnvironmentEffectRendererEvent(customCloudRenderers, customSkyboxRenderers, customWeatherEffectRenderers)
         );
         CUSTOM_CLOUD_RENDERERS = Map.copyOf(customCloudRenderers);
         CUSTOM_SKYBOX_RENDERERS = Map.copyOf(customSkyboxRenderers);
         CUSTOM_WEATHER_EFFECT_RENDERERS = Map.copyOf(customWeatherEffectRenderers);
      }
   }

   private CustomEnvironmentEffectsRendererManager() {
   }
}
