package net.neoforged.neoforge.network.codec;

import com.mojang.datafixers.util.Function7;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.Lazy;

public final class NeoForgeStreamCodecs {
   public static final StreamCodec<FriendlyByteBuf, byte[]> UNBOUNDED_BYTE_ARRAY = new StreamCodec<FriendlyByteBuf, byte[]>() {
      public byte[] decode(FriendlyByteBuf buf) {
         return buf.readByteArray();
      }

      public void encode(FriendlyByteBuf buf, byte[] data) {
         buf.writeByteArray(data);
      }
   };
   public static final StreamCodec<FriendlyByteBuf, ChunkPos> CHUNK_POS = new StreamCodec<FriendlyByteBuf, ChunkPos>() {
      public ChunkPos decode(FriendlyByteBuf buf) {
         return buf.readChunkPos();
      }

      public void encode(FriendlyByteBuf buf, ChunkPos pos) {
         buf.writeChunkPos(pos);
      }
   };

   public static <B, V> StreamCodec<B, V> lazy(Supplier<StreamCodec<B, V>> streamCodecSupplier) {
      return new NeoForgeStreamCodecs.LazyStreamCodec<>(streamCodecSupplier);
   }

   public static <B extends FriendlyByteBuf, V extends Enum<V>> StreamCodec<B, V> enumCodec(final Class<V> enumClass) {
      return new StreamCodec<B, V>() {
         public V decode(B buf) {
            return buf.readEnum(enumClass);
         }

         public void encode(B buf, V value) {
            buf.writeEnum(value);
         }
      };
   }

   public static <B extends FriendlyByteBuf> StreamCodec<B, ResourceKey<? extends Registry<?>>> registryKey() {
      return new StreamCodec<B, ResourceKey<? extends Registry<?>>>() {
         public ResourceKey<? extends Registry<?>> decode(B buf) {
            return ResourceKey.createRegistryKey(buf.readResourceLocation());
         }

         public void encode(B buf, ResourceKey<? extends Registry<?>> value) {
            buf.writeResourceLocation(value.location());
         }
      };
   }

   public static <V> StreamCodec<RegistryFriendlyByteBuf, V> connectionAware(
      final StreamCodec<? super RegistryFriendlyByteBuf, V> neoForgeCodec, final StreamCodec<? super RegistryFriendlyByteBuf, V> otherCodec
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, V>() {
         public V decode(RegistryFriendlyByteBuf buf) {
            return (V)(switch (buf.getConnectionType()) {
               case NEOFORGE -> (Object)neoForgeCodec.decode(buf);
               case OTHER -> (Object)otherCodec.decode(buf);
            });
         }

         public void encode(RegistryFriendlyByteBuf buf, V value) {
            switch (buf.getConnectionType()) {
               case NEOFORGE:
                  neoForgeCodec.encode(buf, value);
                  break;
               case OTHER:
                  otherCodec.encode(buf, value);
            }
         }
      };
   }

   public static <B, V> StreamCodec<B, V> uncheckedUnit(final V defaultValue) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B buf) {
            return defaultValue;
         }

         @Override
         public void encode(B buf, V value) {
         }
      };
   }

   public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> codec1,
      final Function<C, T1> getter1,
      final StreamCodec<? super B, T2> codec2,
      final Function<C, T2> getter2,
      final StreamCodec<? super B, T3> codec3,
      final Function<C, T3> getter3,
      final StreamCodec<? super B, T4> codec4,
      final Function<C, T4> getter4,
      final StreamCodec<? super B, T5> codec5,
      final Function<C, T5> getter5,
      final StreamCodec<? super B, T6> codec6,
      final Function<C, T6> getter6,
      final StreamCodec<? super B, T7> codec7,
      final Function<C, T7> getter7,
      final Function7<T1, T2, T3, T4, T5, T6, T7, C> p_331335_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_330310_) {
            T1 t1 = codec1.decode(p_330310_);
            T2 t2 = codec2.decode(p_330310_);
            T3 t3 = codec3.decode(p_330310_);
            T4 t4 = codec4.decode(p_330310_);
            T5 t5 = codec5.decode(p_330310_);
            T6 t6 = codec6.decode(p_330310_);
            T7 t7 = codec7.decode(p_330310_);
            return (C)p_331335_.apply(t1, t2, t3, t4, t5, t6, t7);
         }

         @Override
         public void encode(B p_332052_, C p_331912_) {
            codec1.encode(p_332052_, getter1.apply(p_331912_));
            codec2.encode(p_332052_, getter2.apply(p_331912_));
            codec3.encode(p_332052_, getter3.apply(p_331912_));
            codec4.encode(p_332052_, getter4.apply(p_331912_));
            codec5.encode(p_332052_, getter5.apply(p_331912_));
            codec6.encode(p_332052_, getter6.apply(p_331912_));
            codec7.encode(p_332052_, getter7.apply(p_331912_));
         }
      };
   }

   private static class LazyStreamCodec<B, V> implements StreamCodec<B, V> {
      private final Lazy<StreamCodec<B, V>> delegate;

      public LazyStreamCodec(Supplier<StreamCodec<B, V>> streamCodecSupplier) {
         this.delegate = Lazy.of(streamCodecSupplier);
      }

      @Override
      public void encode(B buf, V value) {
         this.delegate.get().encode(buf, value);
      }

      @Override
      public V decode(B buf) {
         return this.delegate.get().decode(buf);
      }
   }
}
