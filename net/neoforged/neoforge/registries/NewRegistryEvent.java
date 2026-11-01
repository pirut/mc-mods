package net.neoforged.neoforge.registries;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class NewRegistryEvent extends Event implements IModBusEvent {
   private final List<Registry<?>> registries = new ArrayList<>();

   NewRegistryEvent() {
   }

   public <T> Registry<T> create(RegistryBuilder<T> builder) {
      Registry<T> registry = builder.create();
      this.register(registry);
      return registry;
   }

   public <T> void register(Registry<T> registry) {
      this.registries.add(registry);
   }

   void fill() {
      ((BaseMappedRegistry)BuiltInRegistries.REGISTRY).unfreeze(true);

      for (Registry<?> registry : this.registries) {
         this.registerToRootRegistry(registry);
      }

      ((WritableRegistry)BuiltInRegistries.REGISTRY).freeze();
   }

   private void registerToRootRegistry(Registry<?> registry) {
      Identifier registryName = registry.key().identifier();
      if (BuiltInRegistries.REGISTRY.containsKey(registryName)) {
         throw new IllegalStateException("Attempted duplicate registration of registry " + registryName);
      } else {
         ((WritableRegistry)BuiltInRegistries.REGISTRY).register(registry.key(), registry, RegistrationInfo.BUILT_IN);
      }
   }
}
