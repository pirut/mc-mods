package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final Identifier EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("empty");
   public static final Sound EMPTY_SOUND = new Sound(EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
   public static final Identifier INTENTIONALLY_EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("intentionally_empty");
   public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
   public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
      INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
   );
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON = new GsonBuilder().registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer()).create();
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {};
   private final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;
   private final Map<Identifier, Resource> soundCache = new HashMap<>();

   public SoundManager(Options options) {
      this.soundEngine = new SoundEngine(this, options, ResourceProvider.fromMap(this.soundCache));
   }

   protected SoundManager.Preparations prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
      SoundManager.Preparations preparations = new SoundManager.Preparations();

      try (Zone zone = profiler.zone("list")) {
         preparations.listResources(resourceManager);
      }

      for (String string : resourceManager.getNamespaces()) {
         try (Zone zone2 = profiler.zone(string)) {
            for (Resource resource : resourceManager.getResourceStack(Identifier.fromNamespaceAndPath(string, "sounds.json"))) {
               profiler.push(resource.sourcePackId());

               try (Reader reader = resource.openAsReader()) {
                  profiler.push("parse");
                  Map<String, SoundEventRegistration> map = GsonHelper.fromJson(GSON, reader, SOUND_EVENT_REGISTRATION_TYPE);
                  profiler.popPush("register");

                  for (Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                     preparations.handleRegistration(Identifier.fromNamespaceAndPath(string, entry.getKey()), entry.getValue());
                  }

                  profiler.pop();
               } catch (RuntimeException var19) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", new Object[]{"sounds.json", resource.sourcePackId(), var19});
               }

               profiler.pop();
            }
         } catch (IOException var21) {
         }
      }

      return preparations;
   }

   protected void apply(SoundManager.Preparations object, ResourceManager resourceManager, ProfilerFiller profiler) {
      object.apply(this.registry, this.soundCache, this.soundEngine);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         for (Identifier identifier : this.registry.keySet()) {
            WeighedSoundEvents weighedSoundEvents = this.registry.get(identifier);
            if (!ComponentUtils.isTranslationResolvable(weighedSoundEvents.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey(identifier)) {
               LOGGER.error("Missing subtitle {} for sound event: {}", weighedSoundEvents.getSubtitle(), identifier);
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         for (Identifier identifierx : this.registry.keySet()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(identifierx)) {
               LOGGER.debug("Not having sound event for: {}", identifierx);
            }
         }
      }

      this.soundEngine.reload();
   }

   public List<String> getAvailableSoundDevices() {
      return this.soundEngine.getAvailableSoundDevices();
   }

   public ListenerTransform getListenerTransform() {
      return this.soundEngine.getListenerTransform();
   }

   static boolean validateSoundResource(Sound sound, Identifier location, ResourceProvider resourceProvider) {
      Identifier identifier = sound.getPath();
      if (resourceProvider.getResource(identifier).isEmpty()) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", identifier, location);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeighedSoundEvents getSoundEvent(Identifier location) {
      return this.registry.get(location);
   }

   public Collection<Identifier> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance tickableSound) {
      this.soundEngine.queueTickingSound(tickableSound);
   }

   public SoundEngine.PlayResult play(SoundInstance sound) {
      return this.soundEngine.play(sound);
   }

   public void playDelayed(SoundInstance sound, int delay) {
      this.soundEngine.playDelayed(sound, delay);
   }

   public void updateSource(Camera activeRenderInfo) {
      this.soundEngine.updateSource(activeRenderInfo);
   }

   public void pauseAllExcept(SoundSource... soundSources) {
      this.soundEngine.pauseAllExcept(soundSources);
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void emergencyShutdown() {
      this.soundEngine.emergencyShutdown();
   }

   public void tick(boolean isGamePaused) {
      this.soundEngine.tick(isGamePaused);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void refreshCategoryVolume(SoundSource soundSource) {
      this.soundEngine.refreshCategoryVolume(soundSource);
   }

   public void stop(SoundInstance sound) {
      this.soundEngine.stop(sound);
   }

   public void updateCategoryVolume(SoundSource soundSource, float volume) {
      this.soundEngine.updateCategoryVolume(soundSource, volume);
   }

   public boolean isActive(SoundInstance sound) {
      return this.soundEngine.isActive(sound);
   }

   public void addListener(SoundEventListener listener) {
      this.soundEngine.addEventListener(listener);
   }

   public void removeListener(SoundEventListener listener) {
      this.soundEngine.removeEventListener(listener);
   }

   public void stop(@Nullable Identifier id, @Nullable SoundSource category) {
      this.soundEngine.stop(id, category);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   public void reload() {
      this.soundEngine.reload();
   }

   @Environment(EnvType.CLIENT)
   protected static class Preparations {
      final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
      private Map<Identifier, Resource> soundCache = Map.of();

      void listResources(ResourceManager resourceManager) {
         this.soundCache = Sound.SOUND_LISTER.listMatchingResources(resourceManager);
      }

      void handleRegistration(Identifier location, SoundEventRegistration registration) {
         WeighedSoundEvents weighedSoundEvents = this.registry.get(location);
         boolean bl = weighedSoundEvents == null;
         if (bl || registration.isReplace()) {
            if (!bl) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", location);
            }

            weighedSoundEvents = new WeighedSoundEvents(location, registration.getSubtitle());
            this.registry.put(location, weighedSoundEvents);
         }

         ResourceProvider resourceProvider = ResourceProvider.fromMap(this.soundCache);

         for (final Sound sound : registration.getSounds()) {
            final Identifier identifier = sound.getLocation();
            Weighted<Sound> weighted;
            switch (sound.getType()) {
               case FILE:
                  if (!SoundManager.validateSoundResource(sound, location, resourceProvider)) {
                     continue;
                  }

                  weighted = sound;
                  break;
               case SOUND_EVENT:
                  weighted = new Weighted<Sound>() {
                     @Override
                     public int getWeight() {
                        WeighedSoundEvents weighedSoundEventsx = Preparations.this.registry.get(identifier);
                        return weighedSoundEventsx == null ? 0 : weighedSoundEventsx.getWeight();
                     }

                     public Sound getSound(RandomSource randomSource) {
                        WeighedSoundEvents weighedSoundEventsx = Preparations.this.registry.get(identifier);
                        if (weighedSoundEventsx == null) {
                           return SoundManager.EMPTY_SOUND;
                        } else {
                           Sound soundx = weighedSoundEventsx.getSound(randomSource);
                           return new Sound(
                              soundx.getLocation(),
                              new MultipliedFloats(soundx.getVolume(), sound.getVolume()),
                              new MultipliedFloats(soundx.getPitch(), sound.getPitch()),
                              sound.getWeight(),
                              Sound.Type.FILE,
                              soundx.shouldStream() || sound.shouldStream(),
                              soundx.shouldPreload(),
                              soundx.getAttenuationDistance()
                           );
                        }
                     }

                     @Override
                     public void preloadIfRequired(SoundEngine engine) {
                        WeighedSoundEvents weighedSoundEventsx = Preparations.this.registry.get(identifier);
                        if (weighedSoundEventsx != null) {
                           weighedSoundEventsx.preloadIfRequired(engine);
                        }
                     }
                  };
                  break;
               default:
                  throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getType());
            }

            weighedSoundEvents.addSound(weighted);
         }
      }

      public void apply(Map<Identifier, WeighedSoundEvents> soundRegistry, Map<Identifier, Resource> cache, SoundEngine soundEngine) {
         soundRegistry.clear();
         cache.clear();
         cache.putAll(this.soundCache);

         for (Entry<Identifier, WeighedSoundEvents> entry : this.registry.entrySet()) {
            soundRegistry.put(entry.getKey(), entry.getValue());
            entry.getValue().preloadIfRequired(soundEngine);
         }
      }
   }
}
