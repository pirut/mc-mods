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
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
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
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import net.neoforged.neoforge.registries.holdersets.ICustomHolderSet;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public interface ByteBufCodecs {
   int MAX_INITIAL_COLLECTION_SIZE = 65536;
   StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<ByteBuf, Boolean>() {
      public Boolean decode(ByteBuf p_320813_) {
         return p_320813_.readBoolean();
      }

      public void encode(ByteBuf p_319896_, Boolean p_320251_) {
         p_319896_.writeBoolean(p_320251_);
      }
   };
   StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<ByteBuf, Byte>() {
      public Byte decode(ByteBuf p_320628_) {
         return p_320628_.readByte();
      }

      public void encode(ByteBuf p_320364_, Byte p_320618_) {
         p_320364_.writeByte(p_320618_);
      }
   };
   StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(Mth::unpackDegrees, Mth::packDegrees);
   StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<ByteBuf, Short>() {
      public Short decode(ByteBuf p_320513_) {
         return p_320513_.readShort();
      }

      public void encode(ByteBuf p_320028_, Short p_320388_) {
         p_320028_.writeShort(p_320388_);
      }
   };
   StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_320319_) {
         return p_320319_.readUnsignedShort();
      }

      public void encode(ByteBuf p_320669_, Integer p_320205_) {
         p_320669_.writeShort(p_320205_);
      }
   };
   StreamCodec<ByteBuf, Integer> INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_320253_) {
         return p_320253_.readInt();
      }

      public void encode(ByteBuf p_320753_, Integer p_330380_) {
         p_320753_.writeInt(p_330380_);
      }
   };
   StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_320759_) {
         return VarInt.read(p_320759_);
      }

      public void encode(ByteBuf p_320314_, Integer p_341414_) {
         VarInt.write(p_320314_, p_341414_);
      }
   };
   StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(
      p_378955_ -> p_378955_ == 0 ? OptionalInt.empty() : OptionalInt.of(p_378955_ - 1), p_378954_ -> p_378954_.isPresent() ? p_378954_.getAsInt() + 1 : 0
   );
   StreamCodec<ByteBuf, Long> LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(ByteBuf p_320635_) {
         return p_320635_.readLong();
      }

      public void encode(ByteBuf p_320545_, Long p_341419_) {
         p_320545_.writeLong(p_341419_);
      }
   };
   StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(ByteBuf p_320259_) {
         return VarLong.read(p_320259_);
      }

      public void encode(ByteBuf p_320199_, Long p_376100_) {
         VarLong.write(p_320199_, p_376100_);
      }
   };
   StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<ByteBuf, Float>() {
      public Float decode(ByteBuf p_320599_) {
         return p_320599_.readFloat();
      }

      public void encode(ByteBuf p_320880_, Float p_376495_) {
         p_320880_.writeFloat(p_376495_);
      }
   };
   StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<ByteBuf, Double>() {
      public Double decode(ByteBuf p_319947_) {
         return p_319947_.readDouble();
      }

      public void encode(ByteBuf p_320370_, Double p_376267_) {
         p_320370_.writeDouble(p_376267_);
      }
   };
   StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<ByteBuf, byte[]>() {
      public byte[] decode(ByteBuf p_332176_) {
         return FriendlyByteBuf.readByteArray(p_332176_);
      }

      public void encode(ByteBuf p_331068_, byte[] p_376625_) {
         FriendlyByteBuf.writeByteArray(p_331068_, p_376625_);
      }
   };
   StreamCodec<ByteBuf, long[]> LONG_ARRAY = new StreamCodec<ByteBuf, long[]>() {
      public long[] decode(ByteBuf p_341393_) {
         return FriendlyByteBuf.readLongArray(p_341393_);
      }

      public void encode(ByteBuf p_340857_, long[] p_404761_) {
         FriendlyByteBuf.writeLongArray(p_340857_, p_404761_);
      }
   };
   StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
   StreamCodec<ByteBuf, Tag> TAG = tagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, Tag> TRUSTED_TAG = tagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<ByteBuf, Optional<CompoundTag>>() {
      public Optional<CompoundTag> decode(ByteBuf p_324220_) {
         return Optional.ofNullable(FriendlyByteBuf.readNbt(p_324220_));
      }

      public void encode(ByteBuf p_323874_, Optional<CompoundTag> p_428393_) {
         FriendlyByteBuf.writeNbt(p_323874_, p_428393_.orElse(null));
      }
   };
   StreamCodec<ByteBuf, Vector3fc> VECTOR3F = new StreamCodec<ByteBuf, Vector3fc>() {
      public Vector3fc decode(ByteBuf p_331901_) {
         return FriendlyByteBuf.readVector3f(p_331901_);
      }

      public void encode(ByteBuf p_331539_, Vector3fc p_455271_) {
         FriendlyByteBuf.writeVector3f(p_331539_, p_455271_);
      }
   };
   StreamCodec<ByteBuf, Quaternionfc> QUATERNIONF = new StreamCodec<ByteBuf, Quaternionfc>() {
      public Quaternionfc decode(ByteBuf p_332082_) {
         return FriendlyByteBuf.readQuaternion(p_332082_);
      }

      public void encode(ByteBuf p_331172_, Quaternionfc p_455381_) {
         FriendlyByteBuf.writeQuaternion(p_331172_, p_455381_);
      }
   };
   StreamCodec<ByteBuf, Integer> CONTAINER_ID = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_340809_) {
         return FriendlyByteBuf.readContainerId(p_340809_);
      }

      public void encode(ByteBuf p_341417_, Integer p_428195_) {
         FriendlyByteBuf.writeContainerId(p_341417_, p_428195_);
      }
   };
   StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<ByteBuf, PropertyMap>() {
      public PropertyMap decode(ByteBuf p_415628_) {
         int i = ByteBufCodecs.readCount(p_415628_, 16);
         Builder<String, Property> builder = ImmutableMultimap.builder();

         for (int j = 0; j < i; j++) {
            String s = Utf8String.read(p_415628_, 64);
            String s1 = Utf8String.read(p_415628_, 32767);
            String s2 = FriendlyByteBuf.readNullable(p_415628_, p_428194_ -> Utf8String.read(p_428194_, 1024));
            Property property = new Property(s, s1, s2);
            builder.put(property.name(), property);
         }

         return new PropertyMap(builder.build());
      }

      public void encode(ByteBuf p_415915_, PropertyMap p_428306_) {
         ByteBufCodecs.writeCount(p_415915_, p_428306_.size(), 16);

         for (Property property : p_428306_.values()) {
            Utf8String.write(p_415915_, property.name(), 64);
            Utf8String.write(p_415915_, property.value(), 32767);
            FriendlyByteBuf.writeNullable(p_415915_, property.signature(), (p_428520_, p_428307_) -> Utf8String.write(p_428520_, p_428307_, 1024));
         }
      }
   };
   StreamCodec<ByteBuf, String> PLAYER_NAME = stringUtf8(16);
   StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, GameProfile::id, PLAYER_NAME, GameProfile::name, GAME_PROFILE_PROPERTIES, GameProfile::properties, GameProfile::new
   );
   StreamCodec<ByteBuf, Integer> RGB_COLOR = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_422404_) {
         return ARGB.color(p_422404_.readByte() & 0xFF, p_422404_.readByte() & 0xFF, p_422404_.readByte() & 0xFF);
      }

      public void encode(ByteBuf p_422062_, Integer p_440662_) {
         p_422062_.writeByte(ARGB.red(p_440662_));
         p_422062_.writeByte(ARGB.green(p_440662_));
         p_422062_.writeByte(ARGB.blue(p_440662_));
      }
   };

   static StreamCodec<ByteBuf, byte[]> byteArray(final int p_324182_) {
      return new StreamCodec<ByteBuf, byte[]>() {
         public byte[] decode(ByteBuf p_320167_) {
            return FriendlyByteBuf.readByteArray(p_320167_, p_324182_);
         }

         public void encode(ByteBuf p_320240_, byte[] p_341316_) {
            if (p_341316_.length > p_324182_) {
               throw new EncoderException("ByteArray with size " + p_341316_.length + " is bigger than allowed " + p_324182_);
            } else {
               FriendlyByteBuf.writeByteArray(p_320240_, p_341316_);
            }
         }
      };
   }

   static StreamCodec<ByteBuf, String> stringUtf8(final int p_320693_) {
      return new StreamCodec<ByteBuf, String>() {
         public String decode(ByteBuf p_376272_) {
            return Utf8String.read(p_376272_, p_320693_);
         }

         public void encode(ByteBuf p_376553_, String p_404954_) {
            Utf8String.write(p_376553_, p_404954_, p_320693_);
         }
      };
   }

   static StreamCodec<ByteBuf, Optional<Tag>> optionalTagCodec(final Supplier<NbtAccounter> p_428461_) {
      return new StreamCodec<ByteBuf, Optional<Tag>>() {
         public Optional<Tag> decode(ByteBuf p_404669_) {
            return Optional.ofNullable(FriendlyByteBuf.readNbt(p_404669_, p_428461_.get()));
         }

         public void encode(ByteBuf p_405814_, Optional<Tag> p_428357_) {
            FriendlyByteBuf.writeNbt(p_405814_, p_428357_.orElse(null));
         }
      };
   }

   static StreamCodec<ByteBuf, Tag> tagCodec(final Supplier<NbtAccounter> p_320506_) {
      return new StreamCodec<ByteBuf, Tag>() {
         public Tag decode(ByteBuf p_421862_) {
            Tag tag = FriendlyByteBuf.readNbt(p_421862_, p_320506_.get());
            if (tag == null) {
               throw new DecoderException("Expected non-null compound tag");
            } else {
               return tag;
            }
         }

         public void encode(ByteBuf p_422136_, Tag p_428274_) {
            if (p_428274_ == EndTag.INSTANCE) {
               throw new EncoderException("Expected non-null compound tag");
            } else {
               FriendlyByteBuf.writeNbt(p_422136_, p_428274_);
            }
         }
      };
   }

   static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> p_331128_) {
      return tagCodec(p_331128_).map(p_339405_ -> {
         if (p_339405_ instanceof CompoundTag compoundtag) {
            return compoundtag;
         } else {
            throw new DecoderException("Not a compound tag: " + p_339405_);
         }
      }, p_330975_ -> (Tag)p_330975_);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> p_331105_) {
      return fromCodec(p_331105_, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> p_320615_) {
      return fromCodec(p_320615_, NbtAccounter::defaultQuota);
   }

   static <T, B extends ByteBuf, V> StreamCodec.CodecOperation<B, T, V> fromCodec(DynamicOps<T> p_422160_, Codec<V> p_421684_) {
      return p_428110_ -> new StreamCodec<B, V>() {
         public V decode(B p_428561_) {
            T t = (T)p_428110_.decode(p_428561_);
            return (V)p_421684_.parse(p_422160_, t).getOrThrow(p_421833_ -> new DecoderException("Failed to decode: " + p_421833_ + " " + t));
         }

         public void encode(B p_428515_, V p_422051_) {
            T t = (T)p_421684_.encodeStart(p_422160_, p_422051_)
               .getOrThrow(p_421600_ -> new EncoderException("Failed to encode: " + p_421600_ + " " + p_422051_));
            p_428110_.encode(p_428515_, t);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> p_330943_, Supplier<NbtAccounter> p_330382_) {
      return tagCodec(p_330382_).apply(fromCodec(NbtOps.INSTANCE, p_330943_));
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> p_331713_) {
      return fromCodecWithRegistries(p_331713_, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> p_323797_) {
      return fromCodecWithRegistries(p_323797_, NbtAccounter::defaultQuota);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(final Codec<T> p_331571_, Supplier<NbtAccounter> p_331922_) {
      final StreamCodec<ByteBuf, Tag> streamcodec = tagCodec(p_331922_);
      return new StreamCodec<RegistryFriendlyByteBuf, T>() {
         public T decode(RegistryFriendlyByteBuf p_428534_) {
            Tag tag = streamcodec.decode(p_428534_);
            RegistryOps<Tag> registryops = p_428534_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return (T)p_331571_.parse(registryops, tag).getOrThrow(p_428554_ -> new DecoderException("Failed to decode: " + p_428554_ + " " + tag));
         }

         public void encode(RegistryFriendlyByteBuf p_428528_, T p_428469_) {
            RegistryOps<Tag> registryops = p_428528_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = (Tag)p_331571_.encodeStart(registryops, p_428469_)
               .getOrThrow(p_428492_ -> new EncoderException("Failed to encode: " + p_428492_ + " " + p_428469_));
            streamcodec.encode(p_428528_, tag);
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<? super B, V> p_320522_) {
      return new StreamCodec<B, Optional<V>>() {
         public Optional<V> decode(B p_363037_) {
            return p_363037_.readBoolean() ? Optional.of(p_320522_.decode(p_363037_)) : Optional.empty();
         }

         public void encode(B p_364013_, Optional<V> p_428441_) {
            if (p_428441_.isPresent()) {
               p_364013_.writeBoolean(true);
               p_320522_.encode(p_364013_, p_428441_.get());
            } else {
               p_364013_.writeBoolean(false);
            }
         }
      };
   }

   static int readCount(ByteBuf p_331813_, int p_331668_) {
      int i = VarInt.read(p_331813_);
      if (i > p_331668_) {
         throw new DecoderException(i + " elements exceeded max size of: " + p_331668_);
      } else {
         return i;
      }
   }

   static void writeCount(ByteBuf p_330907_, int p_330535_, int p_331447_) {
      if (p_330535_ > p_331447_) {
         throw new EncoderException(p_330535_ + " elements exceeded max size of: " + p_331447_);
      } else {
         VarInt.write(p_330907_, p_330535_);
      }
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> p_320579_, StreamCodec<? super B, V> p_319970_) {
      return collection(p_320579_, p_319970_, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
      final IntFunction<C> p_332198_, final StreamCodec<? super B, V> p_332183_, final int p_332173_
   ) {
      return new StreamCodec<B, C>() {
         public C decode(B p_376474_) {
            int i = ByteBufCodecs.readCount(p_376474_, p_332173_);
            C c = p_332198_.apply(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               c.add(p_332183_.decode(p_376474_));
            }

            return c;
         }

         public void encode(B p_376188_, C p_428244_) {
            ByteBufCodecs.writeCount(p_376188_, p_428244_.size(), p_332173_);

            for (V v : p_428244_) {
               p_332183_.encode(p_376188_, v);
            }
         }
      };
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> p_319808_) {
      return p_319785_ -> collection(p_319808_, p_319785_);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
      return p_320272_ -> collection(ArrayList::new, p_320272_);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int p_330434_) {
      return p_329871_ -> collection(ArrayList::new, p_329871_, p_330434_);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      IntFunction<? extends M> p_320265_, StreamCodec<? super B, K> p_320113_, StreamCodec<? super B, V> p_320275_
   ) {
      return map(p_320265_, p_320113_, p_320275_, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      final IntFunction<? extends M> p_331325_, final StreamCodec<? super B, K> p_331975_, final StreamCodec<? super B, V> p_331254_, final int p_330938_
   ) {
      return new StreamCodec<B, M>() {
         public void encode(B p_405748_, M p_428450_) {
            ByteBufCodecs.writeCount(p_405748_, p_428450_.size(), p_330938_);
            p_428450_.forEach((p_428117_, p_428118_) -> {
               p_331975_.encode(p_405748_, (K)p_428117_);
               p_331254_.encode(p_405748_, (V)p_428118_);
            });
         }

         public M decode(B p_405291_) {
            int i = ByteBufCodecs.readCount(p_405291_, p_330938_);
            M m = (M)p_331325_.apply(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               K k = p_331975_.decode(p_405291_);
               V v = p_331254_.decode(p_405291_);
               m.put(k, v);
            }

            return m;
         }
      };
   }

   static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(final StreamCodec<? super B, L> p_331983_, final StreamCodec<? super B, R> p_332156_) {
      return new StreamCodec<B, Either<L, R>>() {
         public Either<L, R> decode(B p_412599_) {
            return p_412599_.readBoolean() ? Either.left(p_331983_.decode(p_412599_)) : Either.right(p_332156_.decode(p_412599_));
         }

         public void encode(B p_412292_, Either<L, R> p_428253_) {
            p_428253_.ifLeft(p_428205_ -> {
               p_412292_.writeBoolean(true);
               p_331983_.encode(p_412292_, (L)p_428205_);
            }).ifRight(p_428354_ -> {
               p_412292_.writeBoolean(false);
               p_332156_.encode(p_412292_, (R)p_428354_);
            });
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(int p_412184_, BiFunction<B, ByteBuf, B> p_412396_) {
      return p_428113_ -> new StreamCodec<B, V>() {
         public V decode(B p_422567_) {
            int i = VarInt.read(p_422567_);
            if (i > p_412184_) {
               throw new DecoderException("Buffer size " + i + " is larger than allowed limit of " + p_412184_);
            } else {
               int j = p_422567_.readerIndex();
               B b = (B)((ByteBuf)p_412396_.apply(p_422567_, p_422567_.slice(j, i)));
               p_422567_.readerIndex(j + i);
               return (V)p_428113_.decode(b);
            }
         }

         public void encode(B p_422636_, V p_412065_) {
            B b = (B)((ByteBuf)p_412396_.apply(p_422636_, p_422636_.alloc().buffer()));

            try {
               p_428113_.encode(b, p_412065_);
               int i = b.readableBytes();
               if (i > p_412184_) {
                  throw new EncoderException("Buffer size " + i + " is  larger than allowed limit of " + p_412184_);
               }

               VarInt.write(p_422636_, i);
               p_422636_.writeBytes(b);
            } finally {
               b.release();
            }
         }
      };
   }

   static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(int p_412757_) {
      return lengthPrefixed(p_412757_, (p_428322_, p_428403_) -> p_428403_);
   }

   static <V> StreamCodec.CodecOperation<RegistryFriendlyByteBuf, V, V> registryFriendlyLengthPrefixed(int p_428297_) {
      return lengthPrefixed(p_428297_, (p_412025_, p_412026_) -> new RegistryFriendlyByteBuf(p_412026_, p_412025_.registryAccess()));
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(final IntFunction<T> p_320877_, final ToIntFunction<T> p_319985_) {
      return new StreamCodec<ByteBuf, T>() {
         public T decode(ByteBuf p_428190_) {
            int i = VarInt.read(p_428190_);
            return p_320877_.apply(i);
         }

         public void encode(ByteBuf p_428271_, T p_421951_) {
            int i = p_319985_.applyAsInt(p_421951_);
            VarInt.write(p_428271_, i);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> p_319822_) {
      return idMapper(p_319822_::byIdOrThrow, p_319822_::getIdOrThrow);
   }

   private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(
      final ResourceKey<? extends Registry<T>> p_319942_, final Function<Registry<T>, IdMap<R>> p_320353_
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, R>() {
         private IdMap<R> getRegistryOrThrow(RegistryFriendlyByteBuf p_421864_) {
            return p_320353_.apply(ByteBufCodecs.getSyncableRegistryOrThrow(p_421864_, p_319942_));
         }

         public R decode(RegistryFriendlyByteBuf p_412249_) {
            int i = VarInt.read(p_412249_);
            return (R)this.getRegistryOrThrow(p_412249_).byIdOrThrow(i);
         }

         public void encode(RegistryFriendlyByteBuf p_412573_, R p_428521_) {
            int i = this.getRegistryOrThrow(p_412573_).getIdOrThrow(p_428521_);
            VarInt.write(p_412573_, i);
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> p_320404_) {
      return registry(p_320404_, p_428107_ -> p_428107_);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> p_320387_) {
      return registry(p_320387_, Registry::asHolderIdMap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(
      final ResourceKey<? extends Registry<T>> p_320391_, final StreamCodec<? super RegistryFriendlyByteBuf, T> p_320595_
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, Holder<T>>() {
         private static final int DIRECT_HOLDER_ID = 0;

         private IdMap<Holder<T>> getRegistryOrThrow(RegistryFriendlyByteBuf p_428365_) {
            return ByteBufCodecs.getSyncableRegistryOrThrow(p_428365_, p_320391_).asHolderIdMap();
         }

         public Holder<T> decode(RegistryFriendlyByteBuf p_421902_) {
            int i = VarInt.read(p_421902_);
            return i == 0 ? Holder.direct(p_320595_.decode(p_421902_)) : (Holder)this.getRegistryOrThrow(p_421902_).byIdOrThrow(i - 1);
         }

         public void encode(RegistryFriendlyByteBuf p_422033_, Holder<T> p_428456_) {
            switch (p_428456_.kind()) {
               case REFERENCE:
                  int i = this.getRegistryOrThrow(p_422033_).getIdOrThrow(p_428456_);
                  VarInt.write(p_422033_, i + 1);
                  break;
               case DIRECT:
                  VarInt.write(p_422033_, 0);
                  p_320595_.encode(p_422033_, p_428456_.value());
            }
         }
      };
   }

   private static <T> Registry<T> getSyncableRegistryOrThrow(RegistryFriendlyByteBuf buffer, ResourceKey<? extends Registry<T>> registryKey) {
      Registry<T> registry = buffer.registryAccess().lookupOrThrow(registryKey);
      if (RegistryManager.isNonSyncedBuiltInRegistry(registry)) {
         throw new CodecException("Cannot use ID syncing for non-synced built-in registry: " + registry.key());
      } else {
         return registry;
      }
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(final ResourceKey<? extends Registry<T>> p_332137_) {
      return new StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>>() {
         private static final int NAMED_SET = -1;
         private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec = ByteBufCodecs.holderRegistry(p_332137_);
         private final Map<HolderSetType, StreamCodec<RegistryFriendlyByteBuf, ? extends ICustomHolderSet<T>>> holderSetCodecs = new ConcurrentHashMap<>();

         private StreamCodec<RegistryFriendlyByteBuf, ? extends ICustomHolderSet<T>> holderSetCodec(HolderSetType type) {
            return this.holderSetCodecs.computeIfAbsent(type, key -> key.makeStreamCodec(p_332137_));
         }

         private <H extends ICustomHolderSet<T>> H cast(ICustomHolderSet<T> holderSet) {
            return (H)holderSet;
         }

         public HolderSet<T> decode(RegistryFriendlyByteBuf p_428283_) {
            int i = VarInt.read(p_428283_) - 1;
            if (i < -1) {
               return (HolderSet<T>)this.holderSetCodec(NeoForgeRegistries.HOLDER_SET_TYPES.byIdOrThrow(-2 - i)).decode(p_428283_);
            } else if (i == -1) {
               Registry<T> registry = p_428283_.registryAccess().lookupOrThrow(p_332137_);
               return registry.get(TagKey.create(p_332137_, Identifier.STREAM_CODEC.decode(p_428283_))).orElseThrow();
            } else {
               List<Holder<T>> list = new ArrayList<>(Math.min(i, 65536));

               for (int j = 0; j < i; j++) {
                  list.add(this.holderCodec.decode(p_428283_));
               }

               return HolderSet.direct(list);
            }
         }

         public void encode(RegistryFriendlyByteBuf p_428187_, HolderSet<T> p_428305_) {
            if (p_428187_.getConnectionType().isNeoForge() && p_428305_ instanceof ICustomHolderSet<T> customHolderSet) {
               VarInt.write(p_428187_, -1 - NeoForgeRegistries.HOLDER_SET_TYPES.getId(customHolderSet.type()));
               this.holderSetCodec(customHolderSet.type()).encode(p_428187_, this.cast(customHolderSet));
            } else {
               Optional<TagKey<T>> optional = p_428305_.unwrapKey();
               if (optional.isPresent()) {
                  VarInt.write(p_428187_, 0);
                  Identifier.STREAM_CODEC.encode(p_428187_, optional.get().location());
               } else {
                  VarInt.write(p_428187_, p_428305_.size() + 1);

                  for (Holder<T> holder : p_428305_) {
                     this.holderCodec.encode(p_428187_, holder);
                  }
               }
            }
         }
      };
   }

   static StreamCodec<ByteBuf, JsonElement> lenientJson(final int p_422673_) {
      return new StreamCodec<ByteBuf, JsonElement>() {
         private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

         public JsonElement decode(ByteBuf p_422595_) {
            String s = Utf8String.read(p_422595_, p_422673_);

            try {
               return LenientJsonParser.parse(s);
            } catch (JsonSyntaxException var4) {
               throw new DecoderException("Failed to parse JSON", var4);
            }
         }

         public void encode(ByteBuf p_422247_, JsonElement p_439988_) {
            String s = GSON.toJson(p_439988_);
            Utf8String.write(p_422247_, s, p_422673_);
         }
      };
   }
}
