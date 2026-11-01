package net.minecraft.data.worldgen;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface BootstrapContext<T> {
   Holder.Reference<T> register(ResourceKey<T> var1, T var2, Lifecycle var3);

   default Holder.Reference<T> register(ResourceKey<T> p_321660_, T p_321479_) {
      return this.register(p_321660_, p_321479_, Lifecycle.stable());
   }

   <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> var1);

   default <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
      return Optional.empty();
   }
}
