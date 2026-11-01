package net.neoforged.neoforge.network.codec;

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
            return ResourceKey.createRegistryKey(buf.readIdentifier());
         }

         public void encode(B buf, ResourceKey<? extends Registry<?>> value) {
            buf.writeIdentifier(value.identifier());
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
