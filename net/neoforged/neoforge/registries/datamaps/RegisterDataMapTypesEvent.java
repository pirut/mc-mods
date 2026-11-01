package net.neoforged.neoforge.registries.datamaps;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterDataMapTypesEvent extends Event implements IModBusEvent {
   private final Map<ResourceKey<Registry<?>>, Map<ResourceLocation, DataMapType<?, ?>>> attachments;

   @Internal
   public RegisterDataMapTypesEvent(Map<ResourceKey<Registry<?>>, Map<ResourceLocation, DataMapType<?, ?>>> attachments) {
      this.attachments = attachments;
   }

   public <T, R> void register(DataMapType<R, T> type) {
      ResourceKey<Registry<R>> registry = type.registryKey();
      if (DataPackRegistriesHooks.getDataPackRegistries().stream().anyMatch(data -> data.key().equals(registry))
         && type.networkCodec() != null
         && DataPackRegistriesHooks.<R>getSyncedRegistry(registry) == null) {
         throw new UnsupportedOperationException(
            "Cannot register synced data map " + type.id() + " for datapack registry " + registry.location() + " that is not synced!"
         );
      } else {
         Map map = this.attachments.computeIfAbsent(registry, k -> new HashMap<>());
         if (map.containsKey(type.id())) {
            throw new IllegalArgumentException("Tried to register data map type with ID " + type.id() + " to registry " + registry.location() + " twice");
         } else {
            map.put(type.id(), type);
         }
      }
   }
}
