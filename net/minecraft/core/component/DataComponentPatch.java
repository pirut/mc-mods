package net.minecraft.core.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

public final class DataComponentPatch {
   public static final DataComponentPatch EMPTY = new DataComponentPatch(Reference2ObjectMaps.emptyMap());
   public static final Codec<DataComponentPatch> CODEC = Codec.dispatchedMap(DataComponentPatch.PatchKey.CODEC, DataComponentPatch.PatchKey::valueCodec)
      .xmap(p_330885_ -> {
         if (p_330885_.isEmpty()) {
            return EMPTY;
         } else {
            Reference2ObjectMap<DataComponentType<?>, Optional<?>> reference2objectmap = new Reference2ObjectArrayMap(p_330885_.size());

            for (Entry<DataComponentPatch.PatchKey, ?> entry : p_330885_.entrySet()) {
               DataComponentPatch.PatchKey datacomponentpatch$patchkey = entry.getKey();
               if (datacomponentpatch$patchkey.removed()) {
                  reference2objectmap.put(datacomponentpatch$patchkey.type(), Optional.empty());
               } else {
                  reference2objectmap.put(datacomponentpatch$patchkey.type(), Optional.of(entry.getValue()));
               }
            }

            return new DataComponentPatch(reference2objectmap);
         }
      }, p_332132_ -> {
         Reference2ObjectMap<DataComponentPatch.PatchKey, Object> reference2objectmap = new Reference2ObjectArrayMap(p_332132_.map.size());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(p_332132_.map).iterator();

         while (var2.hasNext()) {
            Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
            DataComponentType<?> datacomponenttype = entry.getKey();
            if (!datacomponenttype.isTransient()) {
               Optional<?> optional = entry.getValue();
               if (optional.isPresent()) {
                  reference2objectmap.put(new DataComponentPatch.PatchKey(datacomponenttype, false), optional.get());
               } else {
                  reference2objectmap.put(new DataComponentPatch.PatchKey(datacomponenttype, true), Unit.INSTANCE);
               }
            }
         }

         return reference2objectmap;
      });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> STREAM_CODEC = createStreamCodec(new DataComponentPatch.CodecGetter() {
      @Override
      public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(DataComponentType<T> p_412084_) {
         return p_412084_.streamCodec().cast();
      }
   });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> DELIMITED_STREAM_CODEC = createStreamCodec(
      new DataComponentPatch.CodecGetter() {
         @Override
         public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(DataComponentType<T> p_412362_) {
            StreamCodec<RegistryFriendlyByteBuf, T> streamcodec = p_412362_.streamCodec().cast();
            return streamcodec.apply(ByteBufCodecs.registryFriendlyLengthPrefixed(Integer.MAX_VALUE));
         }
      }
   );
   private static final String REMOVED_PREFIX = "!";
   final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

   private static StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> createStreamCodec(final DataComponentPatch.CodecGetter p_412279_) {
      return new StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch>() {
         public DataComponentPatch decode(RegistryFriendlyByteBuf p_412537_) {
            int i = p_412537_.readVarInt();
            int j = p_412537_.readVarInt();
            if (i == 0 && j == 0) {
               return DataComponentPatch.EMPTY;
            } else {
               int k = i + j;
               Reference2ObjectMap<DataComponentType<?>, Optional<?>> reference2objectmap = new Reference2ObjectArrayMap(Math.min(k, 65536));

               for (int l = 0; l < i; l++) {
                  DataComponentType<?> datacomponenttype = DataComponentType.STREAM_CODEC.decode(p_412537_);
                  Object object = p_412279_.apply(datacomponenttype).decode(p_412537_);
                  reference2objectmap.put(datacomponenttype, Optional.of(object));
               }

               for (int i1 = 0; i1 < j; i1++) {
                  DataComponentType<?> datacomponenttype1 = DataComponentType.STREAM_CODEC.decode(p_412537_);
                  reference2objectmap.put(datacomponenttype1, Optional.empty());
               }

               return new DataComponentPatch(reference2objectmap);
            }
         }

         public void encode(RegistryFriendlyByteBuf p_412402_, DataComponentPatch p_412784_) {
            if (p_412784_.isEmpty()) {
               p_412402_.writeVarInt(0);
               p_412402_.writeVarInt(0);
            } else {
               int i = 0;
               int j = 0;
               ObjectIterator var5 = Reference2ObjectMaps.fastIterable(p_412784_.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  if (((Optional)entry.getValue()).isPresent()) {
                     i++;
                  } else {
                     j++;
                  }
               }

               p_412402_.writeVarInt(i);
               p_412402_.writeVarInt(j);
               var5 = Reference2ObjectMaps.fastIterable(p_412784_.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry1 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  Optional<?> optional = (Optional<?>)entry1.getValue();
                  if (optional.isPresent()) {
                     DataComponentType<?> datacomponenttype = (DataComponentType<?>)entry1.getKey();
                     DataComponentType.STREAM_CODEC.encode(p_412402_, datacomponenttype);
                     this.encodeComponent(p_412402_, datacomponenttype, optional.get());
                  }
               }

               var5 = Reference2ObjectMaps.fastIterable(p_412784_.map).iterator();

               while (var5.hasNext()) {
                  it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry2 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
                  if (((Optional)entry2.getValue()).isEmpty()) {
                     DataComponentType<?> datacomponenttype1 = (DataComponentType<?>)entry2.getKey();
                     DataComponentType.STREAM_CODEC.encode(p_412402_, datacomponenttype1);
                  }
               }
            }
         }

         private <T> void encodeComponent(RegistryFriendlyByteBuf p_412610_, DataComponentType<T> p_412314_, Object p_412095_) {
            p_412279_.apply(p_412314_).encode(p_412610_, (T)p_412095_);
         }
      };
   }

   DataComponentPatch(Reference2ObjectMap<DataComponentType<?>, Optional<?>> p_331816_) {
      this.map = p_331816_;
   }

   public static DataComponentPatch.Builder builder() {
      return new DataComponentPatch.Builder();
   }

   @Nullable
   public <T> Optional<? extends T> get(DataComponentType<? extends T> p_331532_) {
      return (Optional<? extends T>)this.map.get(p_331532_);
   }

   public Set<Entry<DataComponentType<?>, Optional<?>>> entrySet() {
      return this.map.entrySet();
   }

   public int size() {
      return this.map.size();
   }

   public DataComponentPatch forget(Predicate<DataComponentType<?>> p_338629_) {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         Reference2ObjectMap<DataComponentType<?>, Optional<?>> reference2objectmap = new Reference2ObjectArrayMap(this.map);
         reference2objectmap.keySet().removeIf(p_338629_);
         return reference2objectmap.isEmpty() ? EMPTY : new DataComponentPatch(reference2objectmap);
      }
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public DataComponentPatch.SplitResult split() {
      if (this.isEmpty()) {
         return DataComponentPatch.SplitResult.EMPTY;
      } else {
         DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
         Set<DataComponentType<?>> set = Sets.newIdentityHashSet();
         this.map.forEach((p_337451_, p_337452_) -> {
            if (p_337452_.isPresent()) {
               datacomponentmap$builder.setUnchecked(p_337451_, p_337452_.get());
            } else {
               set.add(p_337451_);
            }
         });
         return new DataComponentPatch.SplitResult(datacomponentmap$builder.build(), set);
      }
   }

   @Override
   public boolean equals(Object p_331686_) {
      return this == p_331686_ ? true : p_331686_ instanceof DataComponentPatch datacomponentpatch && this.map.equals(datacomponentpatch.map);
   }

   @Override
   public int hashCode() {
      return this.map.hashCode();
   }

   @Override
   public String toString() {
      return toString(this.map);
   }

   static String toString(Reference2ObjectMap<DataComponentType<?>, Optional<?>> p_330935_) {
      StringBuilder stringbuilder = new StringBuilder();
      stringbuilder.append('{');
      boolean flag = true;
      ObjectIterator var3 = Reference2ObjectMaps.fastIterable(p_330935_).iterator();

      while (var3.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> entry = (Entry<DataComponentType<?>, Optional<?>>)var3.next();
         if (flag) {
            flag = false;
         } else {
            stringbuilder.append(", ");
         }

         Optional<?> optional = entry.getValue();
         if (optional.isPresent()) {
            stringbuilder.append(entry.getKey());
            stringbuilder.append("=>");
            stringbuilder.append(optional.get());
         } else {
            stringbuilder.append("!");
            stringbuilder.append(entry.getKey());
         }
      }

      stringbuilder.append('}');
      return stringbuilder.toString();
   }

   public static class Builder {
      private final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap();

      Builder() {
      }

      public <T> DataComponentPatch.Builder set(DataComponentType<T> p_332004_, T p_331566_) {
         CommonHooks.validateComponent(p_331566_);
         this.map.put(p_332004_, Optional.of(p_331566_));
         return this;
      }

      public <T> DataComponentPatch.Builder remove(DataComponentType<T> p_330845_) {
         this.map.put(p_330845_, Optional.empty());
         return this;
      }

      public <T> DataComponentPatch.Builder set(TypedDataComponent<T> p_331321_) {
         return this.set(p_331321_.type(), p_331321_.value());
      }

      public DataComponentPatch build() {
         return this.map.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(this.map);
      }
   }

   @FunctionalInterface
   interface CodecGetter {
      <T> StreamCodec<? super RegistryFriendlyByteBuf, T> apply(DataComponentType<T> var1);
   }

   record PatchKey(DataComponentType<?> type, boolean removed) {
      public static final Codec<DataComponentPatch.PatchKey> CODEC = Codec.STRING
         .flatXmap(
            p_465937_ -> {
               boolean flag = p_465937_.startsWith("!");
               if (flag) {
                  p_465937_ = p_465937_.substring("!".length());
               }

               Identifier identifier = Identifier.tryParse(p_465937_);
               DataComponentType<?> datacomponenttype = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(identifier);
               if (datacomponenttype == null) {
                  return DataResult.error(() -> "No component with type: '" + identifier + "'");
               } else {
                  return datacomponenttype.isTransient()
                     ? DataResult.error(() -> "'" + identifier + "' is not a persistent component")
                     : DataResult.success(new DataComponentPatch.PatchKey(datacomponenttype, flag));
               }
            },
            p_465936_ -> {
               DataComponentType<?> datacomponenttype = p_465936_.type();
               Identifier identifier = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(datacomponenttype);
               return identifier == null
                  ? DataResult.error(() -> "Unregistered component: " + datacomponenttype)
                  : DataResult.success(p_465936_.removed() ? "!" + identifier : identifier.toString());
            }
         );

      public Codec<?> valueCodec() {
         return this.removed ? Codec.EMPTY.codec() : this.type.codecOrThrow();
      }
   }

   public record SplitResult(DataComponentMap added, Set<DataComponentType<?>> removed) {
      public static final DataComponentPatch.SplitResult EMPTY = new DataComponentPatch.SplitResult(DataComponentMap.EMPTY, Set.of());
   }
}
