package net.neoforged.neoforge.common.data;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SoundDefinitionsProvider implements DataProvider {
   private static final Logger LOGGER = LogManager.getLogger();
   private final PackOutput output;
   private final String modId;
   private final Map<String, SoundDefinition> sounds = new LinkedHashMap<>();

   protected SoundDefinitionsProvider(PackOutput output, String modId) {
      this.output = output;
      this.modId = modId;
   }

   public abstract void registerSounds();

   @Override
   public CompletableFuture<?> run(CachedOutput cache) {
      this.sounds.clear();
      this.registerSounds();
      this.validate();
      return !this.sounds.isEmpty()
         ? this.save(cache, this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(this.modId).resolve("sounds.json"))
         : CompletableFuture.allOf();
   }

   @Override
   public String getName() {
      return "Sound Definitions";
   }

   protected static SoundDefinition definition() {
      return SoundDefinition.definition();
   }

   protected static SoundDefinition.Sound sound(Identifier name, SoundDefinition.SoundType type) {
      return SoundDefinition.Sound.sound(name, type);
   }

   protected static SoundDefinition.Sound sound(Identifier name) {
      return sound(name, SoundDefinition.SoundType.SOUND);
   }

   protected static SoundDefinition.Sound sound(String name, SoundDefinition.SoundType type) {
      return sound(Identifier.parse(name), type);
   }

   protected static SoundDefinition.Sound sound(String name) {
      return sound(Identifier.parse(name));
   }

   protected void add(Holder<SoundEvent> soundEvent, SoundDefinition definition) {
      this.add(soundEvent.value(), definition);
   }

   protected void add(SoundEvent soundEvent, SoundDefinition definition) {
      this.add(soundEvent.location(), definition);
   }

   protected void add(Identifier soundEvent, SoundDefinition definition) {
      this.addSounds(soundEvent.getPath(), definition);
   }

   protected void add(String soundEvent, SoundDefinition definition) {
      this.add(Identifier.parse(soundEvent), definition);
   }

   private void addSounds(String soundEvent, SoundDefinition definition) {
      if (this.sounds.put(soundEvent, definition) != null) {
         throw new IllegalStateException("Sound event '" + this.modId + ":" + soundEvent + "' already exists");
      }
   }

   private void validate() {
      List<String> notValid = this.sounds
         .entrySet()
         .stream()
         .filter(it -> !this.validate(it.getKey(), it.getValue()))
         .map(Entry::getKey)
         .map(it -> this.modId + ":" + it)
         .toList();
      if (!notValid.isEmpty()) {
         throw new IllegalStateException("Found invalid sound events: " + notValid);
      }
   }

   private boolean validate(String name, SoundDefinition def) {
      return def.soundList().stream().filter(it -> it.type() == SoundDefinition.SoundType.EVENT).allMatch(it -> {
         boolean valid = this.sounds.containsKey(name) || BuiltInRegistries.SOUND_EVENT.containsKey(it.name());
         if (!valid) {
            LOGGER.warn("Unable to find event '{}' referenced from '{}'", it.name(), name);
         }

         return valid;
      });
   }

   private CompletableFuture<?> save(CachedOutput cache, Path targetFile) {
      return DataProvider.saveStable(cache, this.mapToJson(this.sounds), targetFile);
   }

   private JsonObject mapToJson(Map<String, SoundDefinition> map) {
      JsonObject obj = new JsonObject();
      map.forEach((k, v) -> obj.add(k, v.serialize()));
      return obj;
   }
}
