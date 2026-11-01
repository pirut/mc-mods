package net.neoforged.neoforge.registries;

import com.mojang.datafixers.util.Either;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

public class DeferredHolder<R, T extends R> implements Holder<R>, Supplier<T> {
   protected final ResourceKey<R> key;
   @Nullable
   private Holder<R> holder = null;

   public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation valueName) {
      return create(ResourceKey.create(registryKey, valueName));
   }

   public static <R, T extends R> DeferredHolder<R, T> create(ResourceLocation registryName, ResourceLocation valueName) {
      return create(ResourceKey.createRegistryKey(registryName), valueName);
   }

   public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) {
      return new DeferredHolder<>(key);
   }

   protected DeferredHolder(ResourceKey<R> key) {
      this.key = Objects.requireNonNull(key);
      this.bind(false);
   }

   @Override
   public T value() {
      this.bind(true);
      if (this.holder == null) {
         throw new NullPointerException("Trying to access unbound value: " + this.key);
      } else {
         return (T)this.holder.value();
      }
   }

   @Override
   public T get() {
      return this.value();
   }

   public Optional<T> asOptional() {
      return this.isBound() ? Optional.of(this.value()) : Optional.empty();
   }

   @Nullable
   protected Registry<R> getRegistry() {
      return (Registry<R>)BuiltInRegistries.REGISTRY.get(this.key.registry());
   }

   protected final void bind(boolean throwOnMissingRegistry) {
      if (this.holder == null) {
         Registry<R> registry = this.getRegistry();
         if (registry != null) {
            this.holder = registry.getHolder(this.key).orElse(null);
         } else if (throwOnMissingRegistry) {
            throw new IllegalStateException("Registry not present for " + this + ": " + this.key.registry());
         }
      }
   }

   public ResourceLocation getId() {
      return this.key.location();
   }

   @Override
   public ResourceKey<R> getKey() {
      return this.key;
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof Holder<?> h && h.kind() == Holder.Kind.REFERENCE && h.getKey() == this.key;
   }

   @Override
   public int hashCode() {
      return this.key.hashCode();
   }

   @Override
   public String toString() {
      return String.format(Locale.ENGLISH, "DeferredHolder{%s}", this.key);
   }

   @Override
   public boolean isBound() {
      this.bind(false);
      return this.holder != null && this.holder.isBound();
   }

   @Override
   public boolean is(ResourceLocation id) {
      return id.equals(this.key.location());
   }

   @Override
   public boolean is(ResourceKey<R> key) {
      return key == this.key;
   }

   @Override
   public boolean is(Predicate<ResourceKey<R>> filter) {
      return filter.test(this.key);
   }

   @Override
   public boolean is(TagKey<R> tag) {
      this.bind(false);
      return this.holder != null && this.holder.is(tag);
   }

   @Deprecated
   @Override
   public boolean is(Holder<R> holder) {
      this.bind(false);
      return this.holder != null && this.holder.is(holder);
   }

   @Nullable
   @Override
   public <Z> Z getData(DataMapType<R, Z> type) {
      this.bind(false);
      return this.holder == null ? null : this.holder.getData((DataMapType<Z, Z>)type);
   }

   @Override
   public Stream<TagKey<R>> tags() {
      this.bind(false);
      return this.holder != null ? this.holder.tags() : Stream.empty();
   }

   @Override
   public Either<ResourceKey<R>, R> unwrap() {
      return Either.left(this.key);
   }

   @Override
   public Optional<ResourceKey<R>> unwrapKey() {
      return Optional.of(this.key);
   }

   @Override
   public Holder.Kind kind() {
      return Holder.Kind.REFERENCE;
   }

   @Override
   public boolean canSerializeIn(HolderOwner<R> owner) {
      this.bind(false);
      return this.holder != null && this.holder.canSerializeIn(owner);
   }

   @Override
   public Holder<R> getDelegate() {
      this.bind(false);
      return (Holder<R>)(this.holder != null ? this.holder.getDelegate() : this);
   }
}
