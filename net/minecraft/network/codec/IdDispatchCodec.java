package net.minecraft.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.VarInt;

public class IdDispatchCodec<B extends ByteBuf, V, T> implements StreamCodec<B, V> {
   private static final int UNKNOWN_TYPE = -1;
   private final Function<V, ? extends T> typeGetter;
   private final List<IdDispatchCodec.Entry<B, V, T>> byId;
   private final Object2IntMap<T> toId;

   IdDispatchCodec(Function<V, ? extends T> p_320468_, List<IdDispatchCodec.Entry<B, V, T>> p_320536_, Object2IntMap<T> p_320597_) {
      this.typeGetter = p_320468_;
      this.byId = p_320536_;
      this.toId = p_320597_;
   }

   public V decode(B p_320374_) {
      int $$1 = VarInt.read(p_320374_);
      if ($$1 >= 0 && $$1 < this.byId.size()) {
         IdDispatchCodec.Entry<B, V, T> $$2 = this.byId.get($$1);

         try {
            return (V)$$2.serializer.decode(p_320374_);
         } catch (Exception var5) {
            throw new DecoderException("Failed to decode packet '" + $$2.type + "'", var5);
         }
      } else {
         throw new DecoderException("Received unknown packet id " + $$1);
      }
   }

   public void encode(B p_319955_, V p_320138_) {
      T $$2 = (T)this.typeGetter.apply(p_320138_);
      int $$3 = this.toId.getOrDefault($$2, -1);
      if ($$3 == -1) {
         throw new EncoderException("Sending unknown packet '" + $$2 + "'");
      } else {
         VarInt.write(p_319955_, $$3);
         IdDispatchCodec.Entry<B, V, T> $$4 = this.byId.get($$3);

         try {
            StreamCodec<? super B, V> $$5 = (StreamCodec<? super B, V>)$$4.serializer;
            $$5.encode(p_319955_, p_320138_);
         } catch (Exception var7) {
            throw new EncoderException("Failed to encode packet '" + $$2 + "'", var7);
         }
      }
   }

   public static <B extends ByteBuf, V, T> IdDispatchCodec.Builder<B, V, T> builder(Function<V, ? extends T> p_319781_) {
      return new IdDispatchCodec.Builder<>(p_319781_);
   }

   public static class Builder<B extends ByteBuf, V, T> {
      private final List<IdDispatchCodec.Entry<B, V, T>> entries = new ArrayList<>();
      private final Function<V, ? extends T> typeGetter;

      Builder(Function<V, ? extends T> p_320009_) {
         this.typeGetter = p_320009_;
      }

      public IdDispatchCodec.Builder<B, V, T> add(T p_320423_, StreamCodec<? super B, ? extends V> p_320781_) {
         this.entries.add(new IdDispatchCodec.Entry<>(p_320781_, p_320423_));
         return this;
      }

      public IdDispatchCodec<B, V, T> build() {
         Object2IntOpenHashMap<T> $$0 = new Object2IntOpenHashMap();
         $$0.defaultReturnValue(-2);

         for (IdDispatchCodec.Entry<B, V, T> $$1 : this.entries) {
            int $$2 = $$0.size();
            int $$3 = $$0.putIfAbsent($$1.type, $$2);
            if ($$3 != -2) {
               throw new IllegalStateException("Duplicate registration for type " + $$1.type);
            }
         }

         return new IdDispatchCodec<>(this.typeGetter, List.copyOf(this.entries), $$0);
      }
   }

   record Entry<B, V, T>(StreamCodec<? super B, ? extends V> serializer, T type) {
   }
}
