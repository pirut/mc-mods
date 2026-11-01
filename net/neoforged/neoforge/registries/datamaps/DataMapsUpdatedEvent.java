package net.neoforged.neoforge.registries.datamaps;

import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class DataMapsUpdatedEvent extends Event {
   private final RegistryAccess registryAccess;
   private final Registry<?> registry;
   private final DataMapsUpdatedEvent.UpdateCause cause;

   @Internal
   public DataMapsUpdatedEvent(RegistryAccess registryAccess, Registry<?> registry, DataMapsUpdatedEvent.UpdateCause cause) {
      this.registryAccess = registryAccess;
      this.registry = registry;
      this.cause = cause;
   }

   public RegistryAccess getRegistries() {
      return this.registryAccess;
   }

   public Registry<?> getRegistry() {
      return this.registry;
   }

   public ResourceKey<? extends Registry<?>> getRegistryKey() {
      return this.registry.key();
   }

   public <T> void ifRegistry(ResourceKey<Registry<T>> type, Consumer<Registry<T>> consumer) {
      if (this.getRegistryKey() == type) {
         consumer.accept((Registry<T>)this.registry);
      }
   }

   public DataMapsUpdatedEvent.UpdateCause getCause() {
      return this.cause;
   }

   public static enum UpdateCause {
      CLIENT_SYNC,
      SERVER_RELOAD;
   }
}
