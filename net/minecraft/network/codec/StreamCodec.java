package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
   static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B object) {
            return decoder.decode(object);
         }

         @Override
         public void encode(B object, V object2) {
            encoder.encode(object, object2);
         }
      };
   }

   static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B object) {
            return decoder.decode(object);
         }

         @Override
         public void encode(B object, V object2) {
            encoder.encode(object2, object);
         }
      };
   }

   static <B, V> StreamCodec<B, V> unit(V expectedValue) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B object) {
            return expectedValue;
         }

         @Override
         public void encode(B object, V object2) {
            if (!object2.equals(expectedValue)) {
               throw new IllegalStateException("Can't encode '" + object2 + "', expected '" + expectedValue + "'");
            }
         }
      };
   }

   default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> operation) {
      return operation.apply(this);
   }

   default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> factory, Function<? super O, ? extends V> getter) {
      return new StreamCodec<B, O>() {
         @Override
         public O decode(B object) {
            return (O)factory.apply(StreamCodec.this.decode(object));
         }

         @Override
         public void encode(B object, O object2) {
            StreamCodec.this.encode(object, (V)getter.apply(object2));
         }
      };
   }

   default <O extends ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> bufferFactory) {
      return new StreamCodec<O, V>() {
         public V decode(O byteBuf) {
            B object = (B)bufferFactory.apply(byteBuf);
            return StreamCodec.this.decode(object);
         }

         public void encode(O byteBuf, V object) {
            B object2 = (B)bufferFactory.apply(byteBuf);
            StreamCodec.this.encode(object2, object);
         }
      };
   }

   default <U> StreamCodec<B, U> dispatch(
      Function<? super U, ? extends V> keyGetter, Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecGetter
   ) {
      return new StreamCodec<B, U>() {
         @Override
         public U decode(B object) {
            V object2 = StreamCodec.this.decode(object);
            StreamCodec<? super B, ? extends U> streamCodec = (StreamCodec<? super B, ? extends U>)codecGetter.apply(object2);
            return (U)streamCodec.decode(object);
         }

         @Override
         public void encode(B object, U object2) {
            V object3 = (V)keyGetter.apply(object2);
            StreamCodec<B, U> streamCodec = (StreamCodec<B, U>)codecGetter.apply(object3);
            StreamCodec.this.encode(object, object3);
            streamCodec.encode(object, object2);
         }
      };
   }

   static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec, Function<C, T1> getter, Function<T1, C> factory) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec.decode(object);
            return factory.apply(object2);
         }

         @Override
         public void encode(B object, C object2) {
            codec.encode(object, getter.apply(object2));
         }
      };
   }

   static <B, C, T1, T2> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1, Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2, BiFunction<T1, T2, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            return factory.apply(object2, object3);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      Function3<T1, T2, T3, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            return (C)factory.apply(object2, object3, object4);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      Function4<T1, T2, T3, T4, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            return (C)factory.apply(object2, object3, object4, object5);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      Function5<T1, T2, T3, T4, T5, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      Function6<T1, T2, T3, T4, T5, T6, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      StreamCodec<? super B, T7> codec7,
      Function<C, T7> getter7,
      Function7<T1, T2, T3, T4, T5, T6, T7, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7, object8);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
            codec7.encode(object, getter7.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      StreamCodec<? super B, T7> codec7,
      Function<C, T7> getter7,
      StreamCodec<? super B, T8> codec8,
      Function<C, T8> getter8,
      Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7, object8, object9);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
            codec7.encode(object, getter7.apply(object2));
            codec8.encode(object, getter8.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      StreamCodec<? super B, T7> codec7,
      Function<C, T7> getter7,
      StreamCodec<? super B, T8> codec8,
      Function<C, T8> getter8,
      StreamCodec<? super B, T9> codec9,
      Function<C, T9> getter9,
      Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
            codec7.encode(object, getter7.apply(object2));
            codec8.encode(object, getter8.apply(object2));
            codec9.encode(object, getter9.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      StreamCodec<? super B, T7> codec7,
      Function<C, T7> getter7,
      StreamCodec<? super B, T8> codec8,
      Function<C, T8> getter8,
      StreamCodec<? super B, T9> codec9,
      Function<C, T9> getter9,
      StreamCodec<? super B, T10> codec10,
      Function<C, T10> getter10,
      Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            T10 object11 = codec10.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10, object11);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
            codec7.encode(object, getter7.apply(object2));
            codec8.encode(object, getter8.apply(object2));
            codec9.encode(object, getter9.apply(object2));
            codec10.encode(object, getter10.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      StreamCodec<? super B, T7> codec7,
      Function<C, T7> getter7,
      StreamCodec<? super B, T8> codec8,
      Function<C, T8> getter8,
      StreamCodec<? super B, T9> codec9,
      Function<C, T9> getter9,
      StreamCodec<? super B, T10> codec10,
      Function<C, T10> getter10,
      StreamCodec<? super B, T11> codec11,
      Function<C, T11> getter11,
      Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            T10 object11 = codec10.decode(object);
            T11 object12 = codec11.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10, object11, object12);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
            codec7.encode(object, getter7.apply(object2));
            codec8.encode(object, getter8.apply(object2));
            codec9.encode(object, getter9.apply(object2));
            codec10.encode(object, getter10.apply(object2));
            codec11.encode(object, getter11.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(
      StreamCodec<? super B, T1> codec1,
      Function<C, T1> getter1,
      StreamCodec<? super B, T2> codec2,
      Function<C, T2> getter2,
      StreamCodec<? super B, T3> codec3,
      Function<C, T3> getter3,
      StreamCodec<? super B, T4> codec4,
      Function<C, T4> getter4,
      StreamCodec<? super B, T5> codec5,
      Function<C, T5> getter5,
      StreamCodec<? super B, T6> codec6,
      Function<C, T6> getter6,
      StreamCodec<? super B, T7> codec7,
      Function<C, T7> getter7,
      StreamCodec<? super B, T8> codec8,
      Function<C, T8> getter8,
      StreamCodec<? super B, T9> codec9,
      Function<C, T9> getter9,
      StreamCodec<? super B, T10> codec10,
      Function<C, T10> getter10,
      StreamCodec<? super B, T11> codec11,
      Function<C, T11> getter11,
      StreamCodec<? super B, T12> codec12,
      Function<C, T12> getter12,
      Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> factory
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            T10 object11 = codec10.decode(object);
            T11 object12 = codec11.decode(object);
            T12 object13 = codec12.decode(object);
            return (C)factory.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10, object11, object12, object13);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, getter1.apply(object2));
            codec2.encode(object, getter2.apply(object2));
            codec3.encode(object, getter3.apply(object2));
            codec4.encode(object, getter4.apply(object2));
            codec5.encode(object, getter5.apply(object2));
            codec6.encode(object, getter6.apply(object2));
            codec7.encode(object, getter7.apply(object2));
            codec8.encode(object, getter8.apply(object2));
            codec9.encode(object, getter9.apply(object2));
            codec10.encode(object, getter10.apply(object2));
            codec11.encode(object, getter11.apply(object2));
            codec12.encode(object, getter12.apply(object2));
         }
      };
   }

   static <B, T> StreamCodec<B, T> recursive(UnaryOperator<StreamCodec<B, T>> modifier) {
      return new StreamCodec<B, T>() {
         private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> modifier.apply(this));

         @Override
         public T decode(B object) {
            return this.inner.get().decode(object);
         }

         @Override
         public void encode(B object, T object2) {
            this.inner.get().encode(object, object2);
         }
      };
   }

   default <S extends B> StreamCodec<S, V> cast() {
      return this;
   }

   @FunctionalInterface
   public interface CodecOperation<B, S, T> {
      StreamCodec<B, T> apply(StreamCodec<B, S> streamCodec);
   }
}
