package net.neoforged.neoforge.capabilities;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.resources.ResourceLocation;

public class CapabilityRegistry<C> {
   private final ConcurrentMap<ResourceLocation, CapabilityRegistry.StoredCap<C>> caps = new ConcurrentHashMap<>();
   private final CapabilityRegistry.CapabilityConstructor<C> constructor;

   public CapabilityRegistry(CapabilityRegistry.CapabilityConstructor<C> constructor) {
      Objects.requireNonNull(constructor);
      this.constructor = constructor;
   }

   public C create(ResourceLocation name, Class<?> typeClass, Class<?> contextClass) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(typeClass);
      Objects.requireNonNull(contextClass);
      CapabilityRegistry.StoredCap<C> ret = this.caps.get(name);
      if (ret == null) {
         ret = this.caps
            .computeIfAbsent(name, n -> new CapabilityRegistry.StoredCap<>(this.constructor.create(n, typeClass, contextClass), typeClass, contextClass));
      }

      if (ret.typeClass != typeClass) {
         throw new IllegalStateException("Attempted to register capability " + name + " with existing type class " + ret.typeClass + " != " + typeClass);
      } else if (ret.contextClass != contextClass) {
         throw new IllegalStateException(
            "Attempted to register capability " + name + " with existing context class " + ret.contextClass + " != " + contextClass
         );
      } else {
         return ret.cap;
      }
   }

   public List<C> getAll() {
      return this.caps.values().stream().map(CapabilityRegistry.StoredCap::cap).toList();
   }

   @FunctionalInterface
   public interface CapabilityConstructor<C> {
      C create(ResourceLocation var1, Class<?> var2, Class<?> var3);
   }

   private record StoredCap<C>(C cap, Class<?> typeClass, Class<?> contextClass) {
   }
}
