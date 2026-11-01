package net.neoforged.neoforge.client.event;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.CustomWeatherEffectRenderer;
import net.neoforged.neoforge.common.world.NeoForgeEnvironmentAttributes;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterCustomEnvironmentEffectRendererEvent extends Event implements IModBusEvent {
   private final Map<Identifier, CustomCloudsRenderer> cloudRenderers;
   private final Map<Identifier, CustomSkyboxRenderer> skyboxRenderers;
   private final Map<Identifier, CustomWeatherEffectRenderer> weatherEffectsRenderers;

   @Internal
   public RegisterCustomEnvironmentEffectRendererEvent(
      Map<Identifier, CustomCloudsRenderer> cloudRenderers,
      Map<Identifier, CustomSkyboxRenderer> skyboxRenderers,
      Map<Identifier, CustomWeatherEffectRenderer> weatherEffectsRenderers
   ) {
      this.cloudRenderers = cloudRenderers;
      this.skyboxRenderers = skyboxRenderers;
      this.weatherEffectsRenderers = weatherEffectsRenderers;
   }

   public void registerCloudRenderer(Identifier id, CustomCloudsRenderer effects) {
      if (NeoForgeEnvironmentAttributes.DEFAULT_CUSTOM_CLOUDS.equals(id)) {
         throw new IllegalArgumentException("You cannot register a renderer for the default clouds");
      } else {
         this.cloudRenderers.put(id, effects);
      }
   }

   public void registerSkyboxRenderer(Identifier id, CustomSkyboxRenderer effects) {
      if (NeoForgeEnvironmentAttributes.DEFAULT_CUSTOM_SKYBOX.equals(id)) {
         throw new IllegalArgumentException("You cannot register a renderer for the default skybox");
      } else {
         this.skyboxRenderers.put(id, effects);
      }
   }

   public void registerWeatherEffectRenderer(Identifier id, CustomWeatherEffectRenderer effects) {
      if (NeoForgeEnvironmentAttributes.DEFAULT_CUSTOM_WEATHER_EFFECTS.equals(id)) {
         throw new IllegalArgumentException("You cannot register a renderer for the default weather effects");
      } else {
         this.weatherEffectsRenderers.put(id, effects);
      }
   }
}
