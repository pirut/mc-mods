package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

public interface HolderGetter<T> {
   Optional<Holder.Reference<T>> get(ResourceKey<T> var1);

   default Holder.Reference<T> getOrThrow(ResourceKey<T> p_255990_) {
      return this.get(p_255990_).orElseThrow(() -> new IllegalStateException("Missing element " + p_255990_));
   }

   Optional<HolderSet.Named<T>> get(TagKey<T> var1);

   default HolderSet.Named<T> getOrThrow(TagKey<T> p_256125_) {
      return this.get(p_256125_).orElseThrow(() -> new IllegalStateException("Missing tag " + p_256125_));
   }

   default Optional<Holder<T>> getRandomElementOf(TagKey<T> p_434957_, RandomSource p_433536_) {
      return this.get(p_434957_).flatMap(p_432337_ -> p_432337_.getRandomElement(p_433536_));
   }

   public interface Provider {
      <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

      default <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> p_255881_) {
         return (HolderGetter<T>)this.lookup(p_255881_).orElseThrow(() -> new IllegalStateException("Registry " + p_255881_.identifier() + " not found"));
      }

      default <T> Optional<Holder.Reference<T>> get(ResourceKey<T> p_336145_) {
         return this.lookup(p_336145_.registryKey()).flatMap(p_335174_ -> p_335174_.get(p_336145_));
      }

      default <T> Holder.Reference<T> getOrThrow(ResourceKey<T> p_397923_) {
         return this.lookup(p_397923_.registryKey())
            .flatMap(p_396323_ -> p_396323_.get(p_397923_))
            .orElseThrow(() -> new IllegalStateException("Missing element " + p_397923_));
      }
   }
}
