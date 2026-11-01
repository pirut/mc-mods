package net.neoforged.neoforge.registries;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jspecify.annotations.Nullable;

public class RegisterEvent extends Event implements IModBusEvent {
   private final ResourceKey<? extends Registry<?>> registryKey;
   private final Registry<?> registry;

   RegisterEvent(ResourceKey<? extends Registry<?>> registryKey, Registry<?> registry) {
      this.registryKey = registryKey;
      this.registry = registry;
   }

   public <T> void register(ResourceKey<? extends Registry<T>> registryKey, Identifier name, Supplier<T> valueSupplier) {
      if (this.registryKey.equals(registryKey)) {
         Registry.register(this.registry, name, valueSupplier.get());
      }
   }

   public <T> void register(ResourceKey<? extends Registry<T>> registryKey, Consumer<RegisterEvent.RegisterHelper<T>> consumer) {
      if (this.registryKey.equals(registryKey)) {
         consumer.accept((name, value) -> Registry.register(this.registry, name, value));
      }
   }

   public ResourceKey<? extends Registry<?>> getRegistryKey() {
      return this.registryKey;
   }

   public Registry<?> getRegistry() {
      return this.registry;
   }

   @Nullable
   public <T> Registry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
      return (Registry<T>)(key == this.registryKey ? this.registry : null);
   }

   @FunctionalInterface
   public interface RegisterHelper<T> {
      default void register(ResourceKey<T> key, T value) {
         this.register(key.identifier(), value);
      }

      void register(Identifier var1, T var2);
   }
}
