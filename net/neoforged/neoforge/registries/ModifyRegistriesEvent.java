package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class ModifyRegistriesEvent extends Event implements IModBusEvent {
   ModifyRegistriesEvent() {
   }

   public Iterable<? extends Registry<?>> getRegistries() {
      return BuiltInRegistries.REGISTRY;
   }

   public <T> Registry<T> getRegistry(ResourceKey<? extends Registry<T>> key) {
      Registry<T> registry = (Registry<T>)BuiltInRegistries.REGISTRY.get(key.location());
      if (registry == null) {
         throw new IllegalArgumentException("No registry with key " + key);
      } else {
         return registry;
      }
   }
}
