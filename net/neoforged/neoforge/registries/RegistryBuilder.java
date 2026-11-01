package net.neoforged.neoforge.registries;

import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.callback.AddCallback;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import net.neoforged.neoforge.registries.callback.ClearCallback;
import net.neoforged.neoforge.registries.callback.RegistryCallback;
import org.jspecify.annotations.Nullable;

public class RegistryBuilder<T> {
   private final ResourceKey<? extends Registry<T>> registryKey;
   private final List<RegistryCallback<T>> callbacks = new ArrayList<>();
   @Nullable
   private Identifier defaultKey;
   private boolean intrusiveHolders = false;
   private int maxId = -1;
   private boolean sync = false;
   private boolean registrationCheck = true;

   public RegistryBuilder(ResourceKey<? extends Registry<T>> registryKey) {
      this.registryKey = registryKey;
   }

   public RegistryBuilder<T> defaultKey(Identifier key) {
      this.defaultKey = key;
      return this;
   }

   public RegistryBuilder<T> defaultKey(ResourceKey<T> key) {
      this.defaultKey = key.identifier();
      return this;
   }

   @Deprecated
   public RegistryBuilder<T> withIntrusiveHolders() {
      this.intrusiveHolders = true;
      return this;
   }

   public RegistryBuilder<T> callback(RegistryCallback<T> callback) {
      this.callbacks.add(callback);
      return this;
   }

   public RegistryBuilder<T> onAdd(AddCallback<T> callback) {
      return this.callback(callback);
   }

   public RegistryBuilder<T> onBake(BakeCallback<T> callback) {
      return this.callback(callback);
   }

   public RegistryBuilder<T> onClear(ClearCallback<T> callback) {
      return this.callback(callback);
   }

   public RegistryBuilder<T> maxId(int maxId) {
      if (maxId < 0) {
         throw new IllegalArgumentException("maxId must be greater than or equal to zero");
      } else {
         this.maxId = maxId;
         return this;
      }
   }

   public RegistryBuilder<T> sync(boolean sync) {
      this.sync = sync;
      return this;
   }

   public RegistryBuilder<T> disableRegistrationCheck() {
      this.registrationCheck = false;
      return this;
   }

   public Registry<T> create() {
      BaseMappedRegistry<T> registry = (BaseMappedRegistry<T>)(this.defaultKey != null
         ? new DefaultedMappedRegistry<>(this.defaultKey.toString(), this.registryKey, Lifecycle.stable(), this.intrusiveHolders)
         : new MappedRegistry<>(this.registryKey, Lifecycle.stable(), this.intrusiveHolders));
      this.callbacks.forEach(registry::addCallback);
      if (this.maxId != -1) {
         registry.setMaxId(this.maxId);
      }

      registry.setSync(this.sync);
      if (this.registrationCheck) {
         RegistryManager.trackModdedRegistry(registry.key().identifier());
      }

      return registry;
   }
}
