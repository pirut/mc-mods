package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class DelegatingOps<T> implements DynamicOps<T> {
   protected final DynamicOps<T> delegate;

   protected DelegatingOps(DynamicOps<T> p_135467_) {
      this.delegate = p_135467_;
   }

   public T empty() {
      return (T)this.delegate.empty();
   }

   public T emptyMap() {
      return (T)this.delegate.emptyMap();
   }

   public T emptyList() {
      return (T)this.delegate.emptyList();
   }

   public <U> U convertTo(DynamicOps<U> p_135470_, T p_135471_) {
      return (U)(Objects.equals(p_135470_, this.delegate) ? p_135471_ : this.delegate.convertTo(p_135470_, p_135471_));
   }

   public DataResult<Number> getNumberValue(T p_135518_) {
      return this.delegate.getNumberValue(p_135518_);
   }

   public T createNumeric(Number p_135495_) {
      return (T)this.delegate.createNumeric(p_135495_);
   }

   public T createByte(byte p_135475_) {
      return (T)this.delegate.createByte(p_135475_);
   }

   public T createShort(short p_135497_) {
      return (T)this.delegate.createShort(p_135497_);
   }

   public T createInt(int p_135483_) {
      return (T)this.delegate.createInt(p_135483_);
   }

   public T createLong(long p_135489_) {
      return (T)this.delegate.createLong(p_135489_);
   }

   public T createFloat(float p_135481_) {
      return (T)this.delegate.createFloat(p_135481_);
   }

   public T createDouble(double p_135479_) {
      return (T)this.delegate.createDouble(p_135479_);
   }

   public DataResult<Boolean> getBooleanValue(T p_135502_) {
      return this.delegate.getBooleanValue(p_135502_);
   }

   public T createBoolean(boolean p_135473_) {
      return (T)this.delegate.createBoolean(p_135473_);
   }

   public DataResult<String> getStringValue(T p_135522_) {
      return this.delegate.getStringValue(p_135522_);
   }

   public T createString(String p_135499_) {
      return (T)this.delegate.createString(p_135499_);
   }

   public DataResult<T> mergeToList(T p_135526_, T p_135527_) {
      return this.delegate.mergeToList(p_135526_, p_135527_);
   }

   public DataResult<T> mergeToList(T p_135529_, List<T> p_135530_) {
      return this.delegate.mergeToList(p_135529_, p_135530_);
   }

   public DataResult<T> mergeToMap(T p_135535_, T p_135536_, T p_135537_) {
      return this.delegate.mergeToMap(p_135535_, p_135536_, p_135537_);
   }

   public DataResult<T> mergeToMap(T p_135532_, MapLike<T> p_135533_) {
      return this.delegate.mergeToMap(p_135532_, p_135533_);
   }

   public DataResult<T> mergeToMap(T p_341903_, Map<T, T> p_341909_) {
      return this.delegate.mergeToMap(p_341903_, p_341909_);
   }

   public DataResult<T> mergeToPrimitive(T p_341890_, T p_341887_) {
      return this.delegate.mergeToPrimitive(p_341890_, p_341887_);
   }

   public DataResult<Stream<Pair<T, T>>> getMapValues(T p_135516_) {
      return this.delegate.getMapValues(p_135516_);
   }

   public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T p_135514_) {
      return this.delegate.getMapEntries(p_135514_);
   }

   public T createMap(Map<T, T> p_341927_) {
      return (T)this.delegate.createMap(p_341927_);
   }

   public T createMap(Stream<Pair<T, T>> p_135493_) {
      return (T)this.delegate.createMap(p_135493_);
   }

   public DataResult<MapLike<T>> getMap(T p_135512_) {
      return this.delegate.getMap(p_135512_);
   }

   public DataResult<Stream<T>> getStream(T p_135520_) {
      return this.delegate.getStream(p_135520_);
   }

   public DataResult<Consumer<Consumer<T>>> getList(T p_135508_) {
      return this.delegate.getList(p_135508_);
   }

   public T createList(Stream<T> p_135487_) {
      return (T)this.delegate.createList(p_135487_);
   }

   public DataResult<ByteBuffer> getByteBuffer(T p_135504_) {
      return this.delegate.getByteBuffer(p_135504_);
   }

   public T createByteList(ByteBuffer p_135477_) {
      return (T)this.delegate.createByteList(p_135477_);
   }

   public DataResult<IntStream> getIntStream(T p_135506_) {
      return this.delegate.getIntStream(p_135506_);
   }

   public T createIntList(IntStream p_135485_) {
      return (T)this.delegate.createIntList(p_135485_);
   }

   public DataResult<LongStream> getLongStream(T p_135510_) {
      return this.delegate.getLongStream(p_135510_);
   }

   public T createLongList(LongStream p_135491_) {
      return (T)this.delegate.createLongList(p_135491_);
   }

   public T remove(T p_135539_, String p_135540_) {
      return (T)this.delegate.remove(p_135539_, p_135540_);
   }

   public boolean compressMaps() {
      return this.delegate.compressMaps();
   }

   public ListBuilder<T> listBuilder() {
      return new DelegatingOps.DelegateListBuilder(this.delegate.listBuilder());
   }

   public RecordBuilder<T> mapBuilder() {
      return new DelegatingOps.DelegateRecordBuilder(this.delegate.mapBuilder());
   }

   protected class DelegateListBuilder implements ListBuilder<T> {
      private final ListBuilder<T> original;

      protected DelegateListBuilder(ListBuilder<T> p_412307_) {
         this.original = p_412307_;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public DataResult<T> build(T p_412237_) {
         return this.original.build(p_412237_);
      }

      public ListBuilder<T> add(T p_412532_) {
         this.original.add(p_412532_);
         return this;
      }

      public ListBuilder<T> add(DataResult<T> p_412378_) {
         this.original.add(p_412378_);
         return this;
      }

      public <E> ListBuilder<T> add(E p_412579_, Encoder<E> p_412566_) {
         this.original.add(p_412566_.encodeStart(this.ops(), p_412579_));
         return this;
      }

      public <E> ListBuilder<T> addAll(Iterable<E> p_412167_, Encoder<E> p_412458_) {
         p_412167_.forEach(p_412442_ -> this.original.add(p_412458_.encode(p_412442_, this.ops(), this.ops().empty())));
         return this;
      }

      public ListBuilder<T> withErrorsFrom(DataResult<?> p_412058_) {
         this.original.withErrorsFrom(p_412058_);
         return this;
      }

      public ListBuilder<T> mapError(UnaryOperator<String> p_412382_) {
         this.original.mapError(p_412382_);
         return this;
      }

      public DataResult<T> build(DataResult<T> p_412048_) {
         return this.original.build(p_412048_);
      }
   }

   protected class DelegateRecordBuilder implements RecordBuilder<T> {
      private final RecordBuilder<T> original;

      protected DelegateRecordBuilder(RecordBuilder<T> p_412697_) {
         this.original = p_412697_;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public RecordBuilder<T> add(T p_412088_, T p_412147_) {
         this.original.add(p_412088_, p_412147_);
         return this;
      }

      public RecordBuilder<T> add(T p_412590_, DataResult<T> p_412110_) {
         this.original.add(p_412590_, p_412110_);
         return this;
      }

      public RecordBuilder<T> add(DataResult<T> p_412341_, DataResult<T> p_412398_) {
         this.original.add(p_412341_, p_412398_);
         return this;
      }

      public RecordBuilder<T> add(String p_412210_, T p_412450_) {
         this.original.add(p_412210_, p_412450_);
         return this;
      }

      public RecordBuilder<T> add(String p_412601_, DataResult<T> p_412079_) {
         this.original.add(p_412601_, p_412079_);
         return this;
      }

      public <E> RecordBuilder<T> add(String p_412459_, E p_412139_, Encoder<E> p_412219_) {
         return this.original.add(p_412459_, p_412219_.encodeStart(this.ops(), p_412139_));
      }

      public RecordBuilder<T> withErrorsFrom(DataResult<?> p_412634_) {
         this.original.withErrorsFrom(p_412634_);
         return this;
      }

      public RecordBuilder<T> setLifecycle(Lifecycle p_412125_) {
         this.original.setLifecycle(p_412125_);
         return this;
      }

      public RecordBuilder<T> mapError(UnaryOperator<String> p_412209_) {
         this.original.mapError(p_412209_);
         return this;
      }

      public DataResult<T> build(T p_412377_) {
         return this.original.build(p_412377_);
      }

      public DataResult<T> build(DataResult<T> p_412414_) {
         return this.original.build(p_412414_);
      }
   }
}
