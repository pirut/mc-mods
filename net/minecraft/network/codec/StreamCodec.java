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
   static <B, V> StreamCodec<B, V> of(final StreamEncoder<B, V> p_320243_, final StreamDecoder<B, V> p_320197_) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B p_319945_) {
            return p_320197_.decode(p_319945_);
         }

         @Override
         public void encode(B p_320538_, V p_320754_) {
            p_320243_.encode(p_320538_, p_320754_);
         }
      };
   }

   static <B, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> p_320316_, final StreamDecoder<B, V> p_319991_) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B p_320797_) {
            return p_319991_.decode(p_320797_);
         }

         @Override
         public void encode(B p_319939_, V p_320568_) {
            p_320316_.encode(p_320568_, p_319939_);
         }
      };
   }

   static <B, V> StreamCodec<B, V> unit(final V p_320438_) {
      return new StreamCodec<B, V>() {
         @Override
         public V decode(B p_320572_) {
            return p_320438_;
         }

         @Override
         public void encode(B p_320044_, V p_320328_) {
            if (!p_320328_.equals(p_320438_)) {
               throw new IllegalStateException("Can't encode '" + p_320328_ + "', expected '" + p_320438_ + "'");
            }
         }
      };
   }

   default <O> StreamCodec<B, O> apply(StreamCodec.CodecOperation<B, V, O> p_320531_) {
      return p_320531_.apply(this);
   }

   default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> p_320812_, final Function<? super O, ? extends V> p_320191_) {
      return new StreamCodec<B, O>() {
         @Override
         public O decode(B p_320534_) {
            return (O)p_320812_.apply(StreamCodec.this.decode(p_320534_));
         }

         @Override
         public void encode(B p_319798_, O p_320273_) {
            StreamCodec.this.encode(p_319798_, (V)p_320191_.apply(p_320273_));
         }
      };
   }

   default <O extends ByteBuf> StreamCodec<O, V> mapStream(final Function<O, ? extends B> p_320784_) {
      return new StreamCodec<O, V>() {
         public V decode(O p_319818_) {
            B b = (B)p_320784_.apply(p_319818_);
            return StreamCodec.this.decode(b);
         }

         public void encode(O p_319973_, V p_319843_) {
            B b = (B)p_320784_.apply(p_319973_);
            StreamCodec.this.encode(b, p_319843_);
         }
      };
   }

   default <U> StreamCodec<B, U> dispatch(
      final Function<? super U, ? extends V> p_320474_, final Function<? super V, ? extends StreamCodec<? super B, ? extends U>> p_320190_
   ) {
      return new StreamCodec<B, U>() {
         @Override
         public U decode(B p_320094_) {
            V v = StreamCodec.this.decode(p_320094_);
            StreamCodec<? super B, ? extends U> streamcodec = (StreamCodec<? super B, ? extends U>)p_320190_.apply(v);
            return (U)streamcodec.decode(p_320094_);
         }

         @Override
         public void encode(B p_320767_, U p_320010_) {
            V v = (V)p_320474_.apply(p_320010_);
            StreamCodec<B, U> streamcodec = (StreamCodec<B, U>)p_320190_.apply(v);
            StreamCodec.this.encode(p_320767_, v);
            streamcodec.encode(p_320767_, p_320010_);
         }
      };
   }

   static <B, C, T1> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> p_320179_, final Function<C, T1> p_320085_, final Function<T1, C> p_320672_) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_320924_) {
            T1 t1 = p_320179_.decode(p_320924_);
            return p_320672_.apply(t1);
         }

         @Override
         public void encode(B p_320798_, C p_320749_) {
            p_320179_.encode(p_320798_, p_320085_.apply(p_320749_));
         }
      };
   }

   static <B, C, T1, T2> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_320642_,
      final Function<C, T1> p_320284_,
      final StreamCodec<? super B, T2> p_320068_,
      final Function<C, T2> p_319959_,
      final BiFunction<T1, T2, C> p_320761_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_320168_) {
            T1 t1 = p_320642_.decode(p_320168_);
            T2 t2 = p_320068_.decode(p_320168_);
            return p_320761_.apply(t1, t2);
         }

         @Override
         public void encode(B p_320592_, C p_320163_) {
            p_320642_.encode(p_320592_, p_320284_.apply(p_320163_));
            p_320068_.encode(p_320592_, p_319959_.apply(p_320163_));
         }
      };
   }

   static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_320928_,
      final Function<C, T1> p_320123_,
      final StreamCodec<? super B, T2> p_319815_,
      final Function<C, T2> p_319965_,
      final StreamCodec<? super B, T3> p_319834_,
      final Function<C, T3> p_320645_,
      final Function3<T1, T2, T3, C> p_320386_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_320842_) {
            T1 t1 = p_320928_.decode(p_320842_);
            T2 t2 = p_319815_.decode(p_320842_);
            T3 t3 = p_319834_.decode(p_320842_);
            return (C)p_320386_.apply(t1, t2, t3);
         }

         @Override
         public void encode(B p_320737_, C p_320439_) {
            p_320928_.encode(p_320737_, p_320123_.apply(p_320439_));
            p_319815_.encode(p_320737_, p_319965_.apply(p_320439_));
            p_319834_.encode(p_320737_, p_320645_.apply(p_320439_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_323726_,
      final Function<C, T1> p_324414_,
      final StreamCodec<? super B, T2> p_323659_,
      final Function<C, T2> p_323932_,
      final StreamCodec<? super B, T3> p_323964_,
      final Function<C, T3> p_324282_,
      final StreamCodec<? super B, T4> p_323671_,
      final Function<C, T4> p_324254_,
      final Function4<T1, T2, T3, T4, C> p_324090_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_323859_) {
            T1 t1 = p_323726_.decode(p_323859_);
            T2 t2 = p_323659_.decode(p_323859_);
            T3 t3 = p_323964_.decode(p_323859_);
            T4 t4 = p_323671_.decode(p_323859_);
            return (C)p_324090_.apply(t1, t2, t3, t4);
         }

         @Override
         public void encode(B p_323667_, C p_323469_) {
            p_323726_.encode(p_323667_, p_324414_.apply(p_323469_));
            p_323659_.encode(p_323667_, p_323932_.apply(p_323469_));
            p_323964_.encode(p_323667_, p_324282_.apply(p_323469_));
            p_323671_.encode(p_323667_, p_324254_.apply(p_323469_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_324413_,
      final Function<C, T1> p_323568_,
      final StreamCodec<? super B, T2> p_323835_,
      final Function<C, T2> p_323513_,
      final StreamCodec<? super B, T3> p_324112_,
      final Function<C, T3> p_323815_,
      final StreamCodec<? super B, T4> p_323483_,
      final Function<C, T4> p_323725_,
      final StreamCodec<? super B, T5> p_324486_,
      final Function<C, T5> p_323518_,
      final Function5<T1, T2, T3, T4, T5, C> p_324480_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_324610_) {
            T1 t1 = p_324413_.decode(p_324610_);
            T2 t2 = p_323835_.decode(p_324610_);
            T3 t3 = p_324112_.decode(p_324610_);
            T4 t4 = p_323483_.decode(p_324610_);
            T5 t5 = p_324486_.decode(p_324610_);
            return (C)p_324480_.apply(t1, t2, t3, t4, t5);
         }

         @Override
         public void encode(B p_323786_, C p_323619_) {
            p_324413_.encode(p_323786_, p_323568_.apply(p_323619_));
            p_323835_.encode(p_323786_, p_323513_.apply(p_323619_));
            p_324112_.encode(p_323786_, p_323815_.apply(p_323619_));
            p_323483_.encode(p_323786_, p_323725_.apply(p_323619_));
            p_324486_.encode(p_323786_, p_323518_.apply(p_323619_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_331822_,
      final Function<C, T1> p_330864_,
      final StreamCodec<? super B, T2> p_331390_,
      final Function<C, T2> p_331203_,
      final StreamCodec<? super B, T3> p_331499_,
      final Function<C, T3> p_330294_,
      final StreamCodec<? super B, T4> p_331169_,
      final Function<C, T4> p_331830_,
      final StreamCodec<? super B, T5> p_331057_,
      final Function<C, T5> p_331593_,
      final StreamCodec<? super B, T6> p_331117_,
      final Function<C, T6> p_331904_,
      final Function6<T1, T2, T3, T4, T5, T6, C> p_331335_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_330310_) {
            T1 t1 = p_331822_.decode(p_330310_);
            T2 t2 = p_331390_.decode(p_330310_);
            T3 t3 = p_331499_.decode(p_330310_);
            T4 t4 = p_331169_.decode(p_330310_);
            T5 t5 = p_331057_.decode(p_330310_);
            T6 t6 = p_331117_.decode(p_330310_);
            return (C)p_331335_.apply(t1, t2, t3, t4, t5, t6);
         }

         @Override
         public void encode(B p_332052_, C p_331912_) {
            p_331822_.encode(p_332052_, p_330864_.apply(p_331912_));
            p_331390_.encode(p_332052_, p_331203_.apply(p_331912_));
            p_331499_.encode(p_332052_, p_330294_.apply(p_331912_));
            p_331169_.encode(p_332052_, p_331830_.apply(p_331912_));
            p_331057_.encode(p_332052_, p_331593_.apply(p_331912_));
            p_331117_.encode(p_332052_, p_331904_.apply(p_331912_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_373078_,
      final Function<C, T1> p_372935_,
      final StreamCodec<? super B, T2> p_372849_,
      final Function<C, T2> p_372886_,
      final StreamCodec<? super B, T3> p_372969_,
      final Function<C, T3> p_372958_,
      final StreamCodec<? super B, T4> p_373122_,
      final Function<C, T4> p_372916_,
      final StreamCodec<? super B, T5> p_373075_,
      final Function<C, T5> p_373047_,
      final StreamCodec<? super B, T6> p_372992_,
      final Function<C, T6> p_372826_,
      final StreamCodec<? super B, T7> p_372832_,
      final Function<C, T7> p_373016_,
      final Function7<T1, T2, T3, T4, T5, T6, T7, C> p_372975_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_330903_) {
            T1 t1 = p_373078_.decode(p_330903_);
            T2 t2 = p_372849_.decode(p_330903_);
            T3 t3 = p_372969_.decode(p_330903_);
            T4 t4 = p_373122_.decode(p_330903_);
            T5 t5 = p_373075_.decode(p_330903_);
            T6 t6 = p_372992_.decode(p_330903_);
            T7 t7 = p_372832_.decode(p_330903_);
            return (C)p_372975_.apply(t1, t2, t3, t4, t5, t6, t7);
         }

         @Override
         public void encode(B p_331641_, C p_330634_) {
            p_373078_.encode(p_331641_, p_372935_.apply(p_330634_));
            p_372849_.encode(p_331641_, p_372886_.apply(p_330634_));
            p_372969_.encode(p_331641_, p_372958_.apply(p_330634_));
            p_373122_.encode(p_331641_, p_372916_.apply(p_330634_));
            p_373075_.encode(p_331641_, p_373047_.apply(p_330634_));
            p_372992_.encode(p_331641_, p_372826_.apply(p_330634_));
            p_372832_.encode(p_331641_, p_373016_.apply(p_330634_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_380960_,
      final Function<C, T1> p_381028_,
      final StreamCodec<? super B, T2> p_381002_,
      final Function<C, T2> p_381159_,
      final StreamCodec<? super B, T3> p_381128_,
      final Function<C, T3> p_381004_,
      final StreamCodec<? super B, T4> p_380994_,
      final Function<C, T4> p_381101_,
      final StreamCodec<? super B, T5> p_381073_,
      final Function<C, T5> p_381117_,
      final StreamCodec<? super B, T6> p_380957_,
      final Function<C, T6> p_380996_,
      final StreamCodec<? super B, T7> p_381122_,
      final Function<C, T7> p_381050_,
      final StreamCodec<? super B, T8> p_381110_,
      final Function<C, T8> p_380986_,
      final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> p_381061_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_373035_) {
            T1 t1 = p_380960_.decode(p_373035_);
            T2 t2 = p_381002_.decode(p_373035_);
            T3 t3 = p_381128_.decode(p_373035_);
            T4 t4 = p_380994_.decode(p_373035_);
            T5 t5 = p_381073_.decode(p_373035_);
            T6 t6 = p_380957_.decode(p_373035_);
            T7 t7 = p_381122_.decode(p_373035_);
            T8 t8 = p_381110_.decode(p_373035_);
            return (C)p_381061_.apply(t1, t2, t3, t4, t5, t6, t7, t8);
         }

         @Override
         public void encode(B p_372928_, C p_372897_) {
            p_380960_.encode(p_372928_, p_381028_.apply(p_372897_));
            p_381002_.encode(p_372928_, p_381159_.apply(p_372897_));
            p_381128_.encode(p_372928_, p_381004_.apply(p_372897_));
            p_380994_.encode(p_372928_, p_381101_.apply(p_372897_));
            p_381073_.encode(p_372928_, p_381117_.apply(p_372897_));
            p_380957_.encode(p_372928_, p_380996_.apply(p_372897_));
            p_381122_.encode(p_372928_, p_381050_.apply(p_372897_));
            p_381110_.encode(p_372928_, p_380986_.apply(p_372897_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_397889_,
      final Function<C, T1> p_397877_,
      final StreamCodec<? super B, T2> p_397161_,
      final Function<C, T2> p_398060_,
      final StreamCodec<? super B, T3> p_397054_,
      final Function<C, T3> p_397534_,
      final StreamCodec<? super B, T4> p_397149_,
      final Function<C, T4> p_397705_,
      final StreamCodec<? super B, T5> p_397670_,
      final Function<C, T5> p_397063_,
      final StreamCodec<? super B, T6> p_397318_,
      final Function<C, T6> p_397848_,
      final StreamCodec<? super B, T7> p_397963_,
      final Function<C, T7> p_398039_,
      final StreamCodec<? super B, T8> p_397012_,
      final Function<C, T8> p_397038_,
      final StreamCodec<? super B, T9> p_397403_,
      final Function<C, T9> p_397436_,
      final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> p_398058_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_381156_) {
            T1 t1 = p_397889_.decode(p_381156_);
            T2 t2 = p_397161_.decode(p_381156_);
            T3 t3 = p_397054_.decode(p_381156_);
            T4 t4 = p_397149_.decode(p_381156_);
            T5 t5 = p_397670_.decode(p_381156_);
            T6 t6 = p_397318_.decode(p_381156_);
            T7 t7 = p_397963_.decode(p_381156_);
            T8 t8 = p_397012_.decode(p_381156_);
            T9 t9 = p_397403_.decode(p_381156_);
            return (C)p_398058_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
         }

         @Override
         public void encode(B p_380991_, C p_381087_) {
            p_397889_.encode(p_380991_, p_397877_.apply(p_381087_));
            p_397161_.encode(p_380991_, p_398060_.apply(p_381087_));
            p_397054_.encode(p_380991_, p_397534_.apply(p_381087_));
            p_397149_.encode(p_380991_, p_397705_.apply(p_381087_));
            p_397670_.encode(p_380991_, p_397063_.apply(p_381087_));
            p_397318_.encode(p_380991_, p_397848_.apply(p_381087_));
            p_397963_.encode(p_380991_, p_398039_.apply(p_381087_));
            p_397012_.encode(p_380991_, p_397038_.apply(p_381087_));
            p_397403_.encode(p_380991_, p_397436_.apply(p_381087_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_454836_,
      final Function<C, T1> p_455577_,
      final StreamCodec<? super B, T2> p_455502_,
      final Function<C, T2> p_455103_,
      final StreamCodec<? super B, T3> p_456197_,
      final Function<C, T3> p_455928_,
      final StreamCodec<? super B, T4> p_455153_,
      final Function<C, T4> p_455183_,
      final StreamCodec<? super B, T5> p_455720_,
      final Function<C, T5> p_454770_,
      final StreamCodec<? super B, T6> p_456276_,
      final Function<C, T6> p_455633_,
      final StreamCodec<? super B, T7> p_456216_,
      final Function<C, T7> p_456026_,
      final StreamCodec<? super B, T8> p_456226_,
      final Function<C, T8> p_454843_,
      final StreamCodec<? super B, T9> p_455933_,
      final Function<C, T9> p_455010_,
      final StreamCodec<? super B, T10> p_455057_,
      final Function<C, T10> p_455831_,
      final Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> p_454799_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_397196_) {
            T1 t1 = p_454836_.decode(p_397196_);
            T2 t2 = p_455502_.decode(p_397196_);
            T3 t3 = p_456197_.decode(p_397196_);
            T4 t4 = p_455153_.decode(p_397196_);
            T5 t5 = p_455720_.decode(p_397196_);
            T6 t6 = p_456276_.decode(p_397196_);
            T7 t7 = p_456216_.decode(p_397196_);
            T8 t8 = p_456226_.decode(p_397196_);
            T9 t9 = p_455933_.decode(p_397196_);
            T10 t10 = p_455057_.decode(p_397196_);
            return (C)p_454799_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
         }

         @Override
         public void encode(B p_397968_, C p_397982_) {
            p_454836_.encode(p_397968_, p_455577_.apply(p_397982_));
            p_455502_.encode(p_397968_, p_455103_.apply(p_397982_));
            p_456197_.encode(p_397968_, p_455928_.apply(p_397982_));
            p_455153_.encode(p_397968_, p_455183_.apply(p_397982_));
            p_455720_.encode(p_397968_, p_454770_.apply(p_397982_));
            p_456276_.encode(p_397968_, p_455633_.apply(p_397982_));
            p_456216_.encode(p_397968_, p_456026_.apply(p_397982_));
            p_456226_.encode(p_397968_, p_454843_.apply(p_397982_));
            p_455933_.encode(p_397968_, p_455010_.apply(p_397982_));
            p_455057_.encode(p_397968_, p_455831_.apply(p_397982_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_425768_,
      final Function<C, T1> p_426137_,
      final StreamCodec<? super B, T2> p_425680_,
      final Function<C, T2> p_425767_,
      final StreamCodec<? super B, T3> p_425700_,
      final Function<C, T3> p_426226_,
      final StreamCodec<? super B, T4> p_425792_,
      final Function<C, T4> p_426330_,
      final StreamCodec<? super B, T5> p_426249_,
      final Function<C, T5> p_425789_,
      final StreamCodec<? super B, T6> p_426248_,
      final Function<C, T6> p_425689_,
      final StreamCodec<? super B, T7> p_425685_,
      final Function<C, T7> p_425579_,
      final StreamCodec<? super B, T8> p_426135_,
      final Function<C, T8> p_425766_,
      final StreamCodec<? super B, T9> p_425708_,
      final Function<C, T9> p_425801_,
      final StreamCodec<? super B, T10> p_425715_,
      final Function<C, T10> p_425687_,
      final StreamCodec<? super B, T11> p_425601_,
      final Function<C, T11> p_426192_,
      final Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> p_426154_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_425697_) {
            T1 t1 = p_425768_.decode(p_425697_);
            T2 t2 = p_425680_.decode(p_425697_);
            T3 t3 = p_425700_.decode(p_425697_);
            T4 t4 = p_425792_.decode(p_425697_);
            T5 t5 = p_426249_.decode(p_425697_);
            T6 t6 = p_426248_.decode(p_425697_);
            T7 t7 = p_425685_.decode(p_425697_);
            T8 t8 = p_426135_.decode(p_425697_);
            T9 t9 = p_425708_.decode(p_425697_);
            T10 t10 = p_425715_.decode(p_425697_);
            T11 t11 = p_425601_.decode(p_425697_);
            return (C)p_426154_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
         }

         @Override
         public void encode(B p_425873_, C p_425600_) {
            p_425768_.encode(p_425873_, p_426137_.apply(p_425600_));
            p_425680_.encode(p_425873_, p_425767_.apply(p_425600_));
            p_425700_.encode(p_425873_, p_426226_.apply(p_425600_));
            p_425792_.encode(p_425873_, p_426330_.apply(p_425600_));
            p_426249_.encode(p_425873_, p_425789_.apply(p_425600_));
            p_426248_.encode(p_425873_, p_425689_.apply(p_425600_));
            p_425685_.encode(p_425873_, p_425579_.apply(p_425600_));
            p_426135_.encode(p_425873_, p_425766_.apply(p_425600_));
            p_425708_.encode(p_425873_, p_425801_.apply(p_425600_));
            p_425715_.encode(p_425873_, p_425687_.apply(p_425600_));
            p_425601_.encode(p_425873_, p_426192_.apply(p_425600_));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(
      final StreamCodec<? super B, T1> p_455991_,
      final Function<C, T1> p_455192_,
      final StreamCodec<? super B, T2> p_455219_,
      final Function<C, T2> p_454663_,
      final StreamCodec<? super B, T3> p_455096_,
      final Function<C, T3> p_455581_,
      final StreamCodec<? super B, T4> p_454780_,
      final Function<C, T4> p_456260_,
      final StreamCodec<? super B, T5> p_455797_,
      final Function<C, T5> p_454909_,
      final StreamCodec<? super B, T6> p_455607_,
      final Function<C, T6> p_456075_,
      final StreamCodec<? super B, T7> p_455203_,
      final Function<C, T7> p_454741_,
      final StreamCodec<? super B, T8> p_454681_,
      final Function<C, T8> p_455230_,
      final StreamCodec<? super B, T9> p_456107_,
      final Function<C, T9> p_455934_,
      final StreamCodec<? super B, T10> p_456057_,
      final Function<C, T10> p_455805_,
      final StreamCodec<? super B, T11> p_456082_,
      final Function<C, T11> p_455044_,
      final StreamCodec<? super B, T12> p_455423_,
      final Function<C, T12> p_455751_,
      final Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> p_455613_
   ) {
      return new StreamCodec<B, C>() {
         @Override
         public C decode(B p_455691_) {
            T1 t1 = p_455991_.decode(p_455691_);
            T2 t2 = p_455219_.decode(p_455691_);
            T3 t3 = p_455096_.decode(p_455691_);
            T4 t4 = p_454780_.decode(p_455691_);
            T5 t5 = p_455797_.decode(p_455691_);
            T6 t6 = p_455607_.decode(p_455691_);
            T7 t7 = p_455203_.decode(p_455691_);
            T8 t8 = p_454681_.decode(p_455691_);
            T9 t9 = p_456107_.decode(p_455691_);
            T10 t10 = p_456057_.decode(p_455691_);
            T11 t11 = p_456082_.decode(p_455691_);
            T12 t12 = p_455423_.decode(p_455691_);
            return (C)p_455613_.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
         }

         @Override
         public void encode(B p_454898_, C p_454964_) {
            p_455991_.encode(p_454898_, p_455192_.apply(p_454964_));
            p_455219_.encode(p_454898_, p_454663_.apply(p_454964_));
            p_455096_.encode(p_454898_, p_455581_.apply(p_454964_));
            p_454780_.encode(p_454898_, p_456260_.apply(p_454964_));
            p_455797_.encode(p_454898_, p_454909_.apply(p_454964_));
            p_455607_.encode(p_454898_, p_456075_.apply(p_454964_));
            p_455203_.encode(p_454898_, p_454741_.apply(p_454964_));
            p_454681_.encode(p_454898_, p_455230_.apply(p_454964_));
            p_456107_.encode(p_454898_, p_455934_.apply(p_454964_));
            p_456057_.encode(p_454898_, p_455805_.apply(p_454964_));
            p_456082_.encode(p_454898_, p_455044_.apply(p_454964_));
            p_455423_.encode(p_454898_, p_455751_.apply(p_454964_));
         }
      };
   }

   static <B, T> StreamCodec<B, T> recursive(final UnaryOperator<StreamCodec<B, T>> p_330470_) {
      return new StreamCodec<B, T>() {
         private final Supplier<StreamCodec<B, T>> inner = Suppliers.memoize(() -> p_330470_.apply(this));

         @Override
         public T decode(B p_455222_) {
            return this.inner.get().decode(p_455222_);
         }

         @Override
         public void encode(B p_455653_, T p_456153_) {
            this.inner.get().encode(p_455653_, p_456153_);
         }
      };
   }

   default <S extends B> StreamCodec<S, V> cast() {
      return this;
   }

   @FunctionalInterface
   public interface CodecOperation<B, S, T> {
      StreamCodec<B, T> apply(StreamCodec<B, S> var1);
   }
}
