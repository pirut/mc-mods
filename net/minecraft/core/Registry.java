package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public interface Registry<T> extends Keyable, HolderLookup.RegistryLookup<T>, IdMap<T> {
   @Override
   ResourceKey<? extends Registry<T>> key();

   default Codec<T> byNameCodec() {
      return this.referenceHolderWithLifecycle().flatComapMap(Holder.Reference::value, object -> this.safeCastToReference(this.wrapAsHolder((T)object)));
   }

   default Codec<Holder<T>> holderByNameCodec() {
      return this.referenceHolderWithLifecycle().flatComapMap(reference -> reference, this::safeCastToReference);
   }

   private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
      Codec<Holder.Reference<T>> codec = Identifier.CODEC
         .comapFlatMap(
            identifier -> this.get(identifier)
               .<DataResult>map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + identifier)),
            reference -> reference.key().identifier()
         );
      return ExtraCodecs.overrideLifecycle(
         codec, reference -> this.registrationInfo(reference.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
      );
   }

   private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> value) {
      return value instanceof Holder.Reference<T> reference
         ? DataResult.success(reference)
         : DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + value);
   }

   default <U> Stream<U> keys(DynamicOps<U> dynamicOps) {
      return this.keySet().stream().map(identifier -> (U)dynamicOps.createString(identifier.toString()));
   }

   @Nullable
   Identifier getKey(T value);

   Optional<ResourceKey<T>> getResourceKey(T value);

   @Override
   int getId(@Nullable T value);

   @Nullable
   T getValue(@Nullable ResourceKey<T> key);

   @Nullable
   T getValue(@Nullable Identifier key);

   Optional<RegistrationInfo> registrationInfo(ResourceKey<T> key);

   default Optional<T> getOptional(@Nullable Identifier name) {
      return Optional.ofNullable(this.getValue(name));
   }

   default Optional<T> getOptional(@Nullable ResourceKey<T> registryKey) {
      return Optional.ofNullable(this.getValue(registryKey));
   }

   Optional<Holder.Reference<T>> getAny();

   default T getValueOrThrow(ResourceKey<T> key) {
      T object = this.getValue(key);
      if (object == null) {
         throw new IllegalStateException("Missing key in " + this.key() + ": " + key);
      } else {
         return object;
      }
   }

   Set<Identifier> keySet();

   Set<Entry<ResourceKey<T>, T>> entrySet();

   Set<ResourceKey<T>> registryKeySet();

   Optional<Holder.Reference<T>> getRandom(RandomSource random);

   default Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   boolean containsKey(Identifier name);

   boolean containsKey(ResourceKey<T> key);

   static <T> T register(Registry<? super T> registry, String name, T value) {
      return register(registry, Identifier.parse(name), value);
   }

   static <V, T extends V> T register(Registry<V> registry, Identifier name, T value) {
      return register(registry, ResourceKey.create(registry.key(), name), value);
   }

   static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T value) {
      ((WritableRegistry)registry).register(key, (V)value, RegistrationInfo.BUILT_IN);
      return value;
   }

   static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, ResourceKey<R> key, T value) {
      return ((WritableRegistry)registry).register(key, (R)value, RegistrationInfo.BUILT_IN);
   }

   static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, Identifier name, T value) {
      return registerForHolder(registry, ResourceKey.create(registry.key(), name), value);
   }

   Registry<T> freeze();

   Holder.Reference<T> createIntrusiveHolder(T value);

   Optional<Holder.Reference<T>> get(int index);

   Optional<Holder.Reference<T>> get(Identifier key);

   Holder<T> wrapAsHolder(T value);

   default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> key) {
      return (Iterable<Holder<T>>)DataFixUtils.orElse(this.get(key), List.of());
   }

   Stream<HolderSet.Named<T>> getTags();

   default IdMap<Holder<T>> asHolderIdMap() {
      return new IdMap<Holder<T>>() {
         public int getId(Holder<T> holder) {
            return Registry.this.getId(holder.value());
         }

         @Nullable
         public Holder<T> byId(int i) {
            return (Holder<T>)Registry.this.get(i).orElse(null);
         }

         @Override
         public int size() {
            return Registry.this.size();
         }

         @Override
         public Iterator<Holder<T>> iterator() {
            return Registry.this.listElements().map(reference -> (Holder<T>)reference).iterator();
         }
      };
   }

   Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> loadResult);

   public interface PendingTags<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      HolderLookup.RegistryLookup<T> lookup();

      void apply();

      int size();
   }
}
