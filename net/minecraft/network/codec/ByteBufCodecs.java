package net.minecraft.network.codec;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public interface ByteBufCodecs {
   int MAX_INITIAL_COLLECTION_SIZE = 65536;
   StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<ByteBuf, Boolean>() {
      public Boolean decode(ByteBuf byteBuf) {
         return byteBuf.readBoolean();
      }

      public void encode(ByteBuf byteBuf, Boolean boolean_) {
         byteBuf.writeBoolean(boolean_);
      }
   };
   StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<ByteBuf, Byte>() {
      public Byte decode(ByteBuf byteBuf) {
         return byteBuf.readByte();
      }

      public void encode(ByteBuf byteBuf, Byte byte_) {
         byteBuf.writeByte(byte_);
      }
   };
   StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(Mth::unpackDegrees, Mth::packDegrees);
   StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<ByteBuf, Short>() {
      public Short decode(ByteBuf byteBuf) {
         return byteBuf.readShort();
      }

      public void encode(ByteBuf byteBuf, Short short_) {
         byteBuf.writeShort(short_);
      }
   };
   StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf byteBuf) {
         return byteBuf.readUnsignedShort();
      }

      public void encode(ByteBuf byteBuf, Integer integer) {
         byteBuf.writeShort(integer);
      }
   };
   StreamCodec<ByteBuf, Integer> INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf byteBuf) {
         return byteBuf.readInt();
      }

      public void encode(ByteBuf byteBuf, Integer integer) {
         byteBuf.writeInt(integer);
      }
   };
   StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf byteBuf) {
         return VarInt.read(byteBuf);
      }

      public void encode(ByteBuf byteBuf, Integer integer) {
         VarInt.write(byteBuf, integer);
      }
   };
   StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(
      integer -> integer == 0 ? OptionalInt.empty() : OptionalInt.of(integer - 1), optionalInt -> optionalInt.isPresent() ? optionalInt.getAsInt() + 1 : 0
   );
   StreamCodec<ByteBuf, Long> LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(ByteBuf byteBuf) {
         return byteBuf.readLong();
      }

      public void encode(ByteBuf byteBuf, Long long_) {
         byteBuf.writeLong(long_);
      }
   };
   StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(ByteBuf byteBuf) {
         return VarLong.read(byteBuf);
      }

      public void encode(ByteBuf byteBuf, Long long_) {
         VarLong.write(byteBuf, long_);
      }
   };
   StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<ByteBuf, Float>() {
      public Float decode(ByteBuf byteBuf) {
         return byteBuf.readFloat();
      }

      public void encode(ByteBuf byteBuf, Float float_) {
         byteBuf.writeFloat(float_);
      }
   };
   StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<ByteBuf, Double>() {
      public Double decode(ByteBuf byteBuf) {
         return byteBuf.readDouble();
      }

      public void encode(ByteBuf byteBuf, Double double_) {
         byteBuf.writeDouble(double_);
      }
   };
   StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<ByteBuf, byte[]>() {
      public byte[] decode(ByteBuf buffer) {
         return FriendlyByteBuf.readByteArray(buffer);
      }

      public void encode(ByteBuf buffer, byte[] data) {
         FriendlyByteBuf.writeByteArray(buffer, data);
      }
   };
   StreamCodec<ByteBuf, long[]> LONG_ARRAY = new StreamCodec<ByteBuf, long[]>() {
      public long[] decode(ByteBuf buffer) {
         return FriendlyByteBuf.readLongArray(buffer);
      }

      public void encode(ByteBuf buffer, long[] data) {
         FriendlyByteBuf.writeLongArray(buffer, data);
      }
   };
   StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
   StreamCodec<ByteBuf, Tag> TAG = tagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, Tag> TRUSTED_TAG = tagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<ByteBuf, Optional<CompoundTag>>() {
      public Optional<CompoundTag> decode(ByteBuf byteBuf) {
         return Optional.ofNullable(FriendlyByteBuf.readNbt(byteBuf));
      }

      public void encode(ByteBuf byteBuf, Optional<CompoundTag> optional) {
         FriendlyByteBuf.writeNbt(byteBuf, optional.orElse(null));
      }
   };
   StreamCodec<ByteBuf, Vector3fc> VECTOR3F = new StreamCodec<ByteBuf, Vector3fc>() {
      public Vector3fc decode(ByteBuf byteBuf) {
         return FriendlyByteBuf.readVector3f(byteBuf);
      }

      public void encode(ByteBuf byteBuf, Vector3fc vector3fc) {
         FriendlyByteBuf.writeVector3f(byteBuf, vector3fc);
      }
   };
   StreamCodec<ByteBuf, Quaternionfc> QUATERNIONF = new StreamCodec<ByteBuf, Quaternionfc>() {
      public Quaternionfc decode(ByteBuf byteBuf) {
         return FriendlyByteBuf.readQuaternion(byteBuf);
      }

      public void encode(ByteBuf byteBuf, Quaternionfc quaternionfc) {
         FriendlyByteBuf.writeQuaternion(byteBuf, quaternionfc);
      }
   };
   StreamCodec<ByteBuf, Integer> CONTAINER_ID = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf byteBuf) {
         return FriendlyByteBuf.readContainerId(byteBuf);
      }

      public void encode(ByteBuf byteBuf, Integer integer) {
         FriendlyByteBuf.writeContainerId(byteBuf, integer);
      }
   };
   StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<ByteBuf, PropertyMap>() {
      public PropertyMap decode(ByteBuf byteBuf) {
         int i = ByteBufCodecs.readCount(byteBuf, 16);
         Builder<String, Property> builder = ImmutableMultimap.builder();

         for (int j = 0; j < i; j++) {
            String string = Utf8String.read(byteBuf, 64);
            String string2 = Utf8String.read(byteBuf, 32767);
            String string3 = FriendlyByteBuf.readNullable(byteBuf, byteBufx -> Utf8String.read(byteBufx, 1024));
            Property property = new Property(string, string2, string3);
            builder.put(property.name(), property);
         }

         return new PropertyMap(builder.build());
      }

      public void encode(ByteBuf byteBuf, PropertyMap propertyMap) {
         ByteBufCodecs.writeCount(byteBuf, propertyMap.size(), 16);

         for (Property property : propertyMap.values()) {
            Utf8String.write(byteBuf, property.name(), 64);
            Utf8String.write(byteBuf, property.value(), 32767);
            FriendlyByteBuf.writeNullable(byteBuf, property.signature(), (byteBufx, string) -> Utf8String.write(byteBufx, string, 1024));
         }
      }
   };
   StreamCodec<ByteBuf, String> PLAYER_NAME = stringUtf8(16);
   StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, GameProfile::id, PLAYER_NAME, GameProfile::name, GAME_PROFILE_PROPERTIES, GameProfile::properties, GameProfile::new
   );
   StreamCodec<ByteBuf, Integer> RGB_COLOR = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf byteBuf) {
         return ARGB.color(byteBuf.readByte() & 0xFF, byteBuf.readByte() & 0xFF, byteBuf.readByte() & 0xFF);
      }

      public void encode(ByteBuf byteBuf, Integer integer) {
         byteBuf.writeByte(ARGB.red(integer));
         byteBuf.writeByte(ARGB.green(integer));
         byteBuf.writeByte(ARGB.blue(integer));
      }
   };

   static StreamCodec<ByteBuf, byte[]> byteArray(int maxSize) {
      return new StreamCodec<ByteBuf, byte[]>() {
         public byte[] decode(ByteBuf buffer) {
            return FriendlyByteBuf.readByteArray(buffer, maxSize);
         }

         public void encode(ByteBuf buffer, byte[] value) {
            if (value.length > maxSize) {
               throw new EncoderException("ByteArray with size " + value.length + " is bigger than allowed " + maxSize);
            } else {
               FriendlyByteBuf.writeByteArray(buffer, value);
            }
         }
      };
   }

   static StreamCodec<ByteBuf, String> stringUtf8(int maxLength) {
      return new StreamCodec<ByteBuf, String>() {
         public String decode(ByteBuf byteBuf) {
            return Utf8String.read(byteBuf, maxLength);
         }

         public void encode(ByteBuf byteBuf, String string) {
            Utf8String.write(byteBuf, string, maxLength);
         }
      };
   }

   static StreamCodec<ByteBuf, Optional<Tag>> optionalTagCodec(Supplier<NbtAccounter> accounter) {
      return new StreamCodec<ByteBuf, Optional<Tag>>() {
         public Optional<Tag> decode(ByteBuf byteBuf) {
            return Optional.ofNullable(FriendlyByteBuf.readNbt(byteBuf, accounter.get()));
         }

         public void encode(ByteBuf byteBuf, Optional<Tag> optional) {
            FriendlyByteBuf.writeNbt(byteBuf, optional.orElse(null));
         }
      };
   }

   static StreamCodec<ByteBuf, Tag> tagCodec(Supplier<NbtAccounter> accounter) {
      return new StreamCodec<ByteBuf, Tag>() {
         public Tag decode(ByteBuf byteBuf) {
            Tag tag = FriendlyByteBuf.readNbt(byteBuf, accounter.get());
            if (tag == null) {
               throw new DecoderException("Expected non-null compound tag");
            } else {
               return tag;
            }
         }

         public void encode(ByteBuf byteBuf, Tag tag) {
            if (tag == EndTag.INSTANCE) {
               throw new EncoderException("Expected non-null compound tag");
            } else {
               FriendlyByteBuf.writeNbt(byteBuf, tag);
            }
         }
      };
   }

   static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> accounterSupplier) {
      return tagCodec(accounterSupplier).map(tag -> {
         if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
         } else {
            throw new DecoderException("Not a compound tag: " + tag);
         }
      }, compoundTag -> (Tag)compoundTag);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> codec) {
      return fromCodec(codec, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
      return fromCodec(codec, NbtAccounter::defaultQuota);
   }

   static <T, B extends ByteBuf, V> StreamCodec.CodecOperation<B, T, V> fromCodec(DynamicOps<T> ops, Codec<V> codec) {
      return streamCodec -> new StreamCodec<B, V>() {
         public V decode(B byteBuf) {
            T object = (T)streamCodec.decode(byteBuf);
            return (V)codec.parse(ops, object).getOrThrow(string -> new DecoderException("Failed to decode: " + string + " " + object));
         }

         public void encode(B byteBuf, V object) {
            T object2 = (T)codec.encodeStart(ops, object).getOrThrow(string -> new EncoderException("Failed to encode: " + string + " " + object));
            streamCodec.encode(byteBuf, object2);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> accounterSupplier) {
      return tagCodec(accounterSupplier).apply(fromCodec(NbtOps.INSTANCE, codec));
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> codec) {
      return fromCodecWithRegistries(codec, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
      return fromCodecWithRegistries(codec, NbtAccounter::defaultQuota);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec, Supplier<NbtAccounter> accounterSupplier) {
      final StreamCodec<ByteBuf, Tag> streamCodec = tagCodec(accounterSupplier);
      return new StreamCodec<RegistryFriendlyByteBuf, T>() {
         public T decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            Tag tag = streamCodec.decode(registryFriendlyByteBuf);
            RegistryOps<Tag> registryOps = registryFriendlyByteBuf.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return (T)codec.parse(registryOps, tag).getOrThrow(string -> new DecoderException("Failed to decode: " + string + " " + tag));
         }

         public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, T object) {
            RegistryOps<Tag> registryOps = registryFriendlyByteBuf.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = (Tag)codec.encodeStart(registryOps, object).getOrThrow(string -> new EncoderException("Failed to encode: " + string + " " + object));
            streamCodec.encode(registryFriendlyByteBuf, tag);
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<? super B, V> codec) {
      return new StreamCodec<B, Optional<V>>() {
         public Optional<V> decode(B byteBuf) {
            return byteBuf.readBoolean() ? Optional.of(codec.decode(byteBuf)) : Optional.empty();
         }

         public void encode(B byteBuf, Optional<V> optional) {
            if (optional.isPresent()) {
               byteBuf.writeBoolean(true);
               codec.encode(byteBuf, optional.get());
            } else {
               byteBuf.writeBoolean(false);
            }
         }
      };
   }

   static int readCount(ByteBuf buffer, int maxSize) {
      int i = VarInt.read(buffer);
      if (i > maxSize) {
         throw new DecoderException(i + " elements exceeded max size of: " + maxSize);
      } else {
         return i;
      }
   }

   static void writeCount(ByteBuf buffer, int count, int maxSize) {
      if (count > maxSize) {
         throw new EncoderException(count + " elements exceeded max size of: " + maxSize);
      } else {
         VarInt.write(buffer, count);
      }
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec) {
      return collection(factory, codec, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec, int maxSize) {
      return new StreamCodec<B, C>() {
         public C decode(B byteBuf) {
            int i = ByteBufCodecs.readCount(byteBuf, maxSize);
            C collection = factory.apply(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               collection.add(codec.decode(byteBuf));
            }

            return collection;
         }

         public void encode(B byteBuf, C collection) {
            ByteBufCodecs.writeCount(byteBuf, collection.size(), maxSize);

            for (V object : collection) {
               codec.encode(byteBuf, object);
            }
         }
      };
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> factory) {
      return streamCodec -> collection(factory, streamCodec);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
      return streamCodec -> collection(ArrayList::new, streamCodec);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
      return streamCodec -> collection(ArrayList::new, streamCodec, maxSize);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      IntFunction<? extends M> factory, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec
   ) {
      return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      IntFunction<? extends M> factory, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec, int maxSize
   ) {
      return new StreamCodec<B, M>() {
         public void encode(B byteBuf, M map) {
            ByteBufCodecs.writeCount(byteBuf, map.size(), maxSize);
            map.forEach((object, object2) -> {
               keyCodec.encode(byteBuf, (K)object);
               valueCodec.encode(byteBuf, (V)object2);
            });
         }

         public M decode(B byteBuf) {
            int i = ByteBufCodecs.readCount(byteBuf, maxSize);
            M map = (M)factory.apply(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               K object = keyCodec.decode(byteBuf);
               V object2 = valueCodec.decode(byteBuf);
               map.put(object, object2);
            }

            return map;
         }
      };
   }

   static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(StreamCodec<? super B, L> leftCodec, StreamCodec<? super B, R> rightCodec) {
      return new StreamCodec<B, Either<L, R>>() {
         public Either<L, R> decode(B byteBuf) {
            return byteBuf.readBoolean() ? Either.left(leftCodec.decode(byteBuf)) : Either.right(rightCodec.decode(byteBuf));
         }

         public void encode(B byteBuf, Either<L, R> either) {
            either.ifLeft(object -> {
               byteBuf.writeBoolean(true);
               leftCodec.encode(byteBuf, (L)object);
            }).ifRight(object -> {
               byteBuf.writeBoolean(false);
               rightCodec.encode(byteBuf, (R)object);
            });
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(int maxLength, BiFunction<B, ByteBuf, B> function) {
      return streamCodec -> new StreamCodec<B, V>() {
         public V decode(B byteBuf) {
            int i = VarInt.read(byteBuf);
            if (i > maxLength) {
               throw new DecoderException("Buffer size " + i + " is larger than allowed limit of " + maxLength);
            } else {
               int j = byteBuf.readerIndex();
               B byteBuf2 = (B)((ByteBuf)function.apply(byteBuf, byteBuf.slice(j, i)));
               byteBuf.readerIndex(j + i);
               return (V)streamCodec.decode(byteBuf2);
            }
         }

         public void encode(B byteBuf, V object) {
            B byteBuf2 = (B)((ByteBuf)function.apply(byteBuf, byteBuf.alloc().buffer()));

            try {
               streamCodec.encode(byteBuf2, object);
               int i = byteBuf2.readableBytes();
               if (i > maxLength) {
                  throw new EncoderException("Buffer size " + i + " is  larger than allowed limit of " + maxLength);
               }

               VarInt.write(byteBuf, i);
               byteBuf.writeBytes(byteBuf2);
            } finally {
               byteBuf2.release();
            }
         }
      };
   }

   static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(int length) {
      return lengthPrefixed(length, (byteBuf, byteBuf2) -> byteBuf2);
   }

   static <V> StreamCodec.CodecOperation<RegistryFriendlyByteBuf, V, V> registryFriendlyLengthPrefixed(int length) {
      return lengthPrefixed(length, (registryFriendlyByteBuf, byteBuf) -> new RegistryFriendlyByteBuf(byteBuf, registryFriendlyByteBuf.registryAccess()));
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> idLookup, ToIntFunction<T> idGetter) {
      return new StreamCodec<ByteBuf, T>() {
         public T decode(ByteBuf byteBuf) {
            int i = VarInt.read(byteBuf);
            return idLookup.apply(i);
         }

         public void encode(ByteBuf byteBuf, T object) {
            int i = idGetter.applyAsInt(object);
            VarInt.write(byteBuf, i);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> idMap) {
      return idMapper(idMap::byIdOrThrow, idMap::getIdOrThrow);
   }

   private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(
      ResourceKey<? extends Registry<T>> registryKey, Function<Registry<T>, IdMap<R>> idGetter
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, R>() {
         private IdMap<R> getRegistryOrThrow(RegistryFriendlyByteBuf buffer) {
            return idGetter.apply(buffer.registryAccess().lookupOrThrow(registryKey));
         }

         public R decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            int i = VarInt.read(registryFriendlyByteBuf);
            return (R)this.getRegistryOrThrow(registryFriendlyByteBuf).byIdOrThrow(i);
         }

         public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, R object) {
            int i = this.getRegistryOrThrow(registryFriendlyByteBuf).getIdOrThrow(object);
            VarInt.write(registryFriendlyByteBuf, i);
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> registryKey) {
      return registry(registryKey, registry -> registry);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> registryKey) {
      return registry(registryKey, Registry::asHolderIdMap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(
      ResourceKey<? extends Registry<T>> registryKey, StreamCodec<? super RegistryFriendlyByteBuf, T> codec
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, Holder<T>>() {
         private static final int DIRECT_HOLDER_ID = 0;

         private IdMap<Holder<T>> getRegistryOrThrow(RegistryFriendlyByteBuf buffer) {
            return buffer.registryAccess().lookupOrThrow(registryKey).asHolderIdMap();
         }

         public Holder<T> decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            int i = VarInt.read(registryFriendlyByteBuf);
            return i == 0 ? Holder.direct(codec.decode(registryFriendlyByteBuf)) : (Holder)this.getRegistryOrThrow(registryFriendlyByteBuf).byIdOrThrow(i - 1);
         }

         public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, Holder<T> holder) {
            switch (holder.kind()) {
               case REFERENCE:
                  int i = this.getRegistryOrThrow(registryFriendlyByteBuf).getIdOrThrow(holder);
                  VarInt.write(registryFriendlyByteBuf, i + 1);
                  break;
               case DIRECT:
                  VarInt.write(registryFriendlyByteBuf, 0);
                  codec.encode(registryFriendlyByteBuf, holder.value());
            }
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(ResourceKey<? extends Registry<T>> registryKey) {
      return new StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>>() {
         private static final int NAMED_SET = -1;
         private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec = ByteBufCodecs.holderRegistry(registryKey);

         public HolderSet<T> decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            int i = VarInt.read(registryFriendlyByteBuf) - 1;
            if (i == -1) {
               Registry<T> registry = registryFriendlyByteBuf.registryAccess().lookupOrThrow(registryKey);
               return registry.get(TagKey.create(registryKey, Identifier.STREAM_CODEC.decode(registryFriendlyByteBuf))).orElseThrow();
            } else {
               List<Holder<T>> list = new ArrayList<>(Math.min(i, 65536));

               for (int j = 0; j < i; j++) {
                  list.add(this.holderCodec.decode(registryFriendlyByteBuf));
               }

               return HolderSet.direct(list);
            }
         }

         public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, HolderSet<T> holderSet) {
            Optional<TagKey<T>> optional = holderSet.unwrapKey();
            if (optional.isPresent()) {
               VarInt.write(registryFriendlyByteBuf, 0);
               Identifier.STREAM_CODEC.encode(registryFriendlyByteBuf, optional.get().location());
            } else {
               VarInt.write(registryFriendlyByteBuf, holderSet.size() + 1);

               for (Holder<T> holder : holderSet) {
                  this.holderCodec.encode(registryFriendlyByteBuf, holder);
               }
            }
         }
      };
   }

   static StreamCodec<ByteBuf, JsonElement> lenientJson(int maxLength) {
      return new StreamCodec<ByteBuf, JsonElement>() {
         private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

         public JsonElement decode(ByteBuf byteBuf) {
            String string = Utf8String.read(byteBuf, maxLength);

            try {
               return LenientJsonParser.parse(string);
            } catch (JsonSyntaxException var4) {
               throw new DecoderException("Failed to parse JSON", var4);
            }
         }

         public void encode(ByteBuf byteBuf, JsonElement jsonElement) {
            String string = GSON.toJson(jsonElement);
            Utf8String.write(byteBuf, string, maxLength);
         }
      };
   }
}
