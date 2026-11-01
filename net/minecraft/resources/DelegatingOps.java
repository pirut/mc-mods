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

   protected DelegatingOps(DynamicOps<T> delegate) {
      this.delegate = delegate;
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

   public <U> U convertTo(DynamicOps<U> dynamicOps, T object) {
      return (U)(Objects.equals(dynamicOps, this.delegate) ? object : this.delegate.convertTo(dynamicOps, object));
   }

   public DataResult<Number> getNumberValue(T object) {
      return this.delegate.getNumberValue(object);
   }

   public T createNumeric(Number number) {
      return (T)this.delegate.createNumeric(number);
   }

   public T createByte(byte b) {
      return (T)this.delegate.createByte(b);
   }

   public T createShort(short s) {
      return (T)this.delegate.createShort(s);
   }

   public T createInt(int i) {
      return (T)this.delegate.createInt(i);
   }

   public T createLong(long l) {
      return (T)this.delegate.createLong(l);
   }

   public T createFloat(float f) {
      return (T)this.delegate.createFloat(f);
   }

   public T createDouble(double d) {
      return (T)this.delegate.createDouble(d);
   }

   public DataResult<Boolean> getBooleanValue(T object) {
      return this.delegate.getBooleanValue(object);
   }

   public T createBoolean(boolean bl) {
      return (T)this.delegate.createBoolean(bl);
   }

   public DataResult<String> getStringValue(T object) {
      return this.delegate.getStringValue(object);
   }

   public T createString(String string) {
      return (T)this.delegate.createString(string);
   }

   public DataResult<T> mergeToList(T object, T object2) {
      return this.delegate.mergeToList(object, object2);
   }

   public DataResult<T> mergeToList(T object, List<T> list) {
      return this.delegate.mergeToList(object, list);
   }

   public DataResult<T> mergeToMap(T object, T object2, T object3) {
      return this.delegate.mergeToMap(object, object2, object3);
   }

   public DataResult<T> mergeToMap(T object, MapLike<T> mapLike) {
      return this.delegate.mergeToMap(object, mapLike);
   }

   public DataResult<T> mergeToMap(T object, Map<T, T> map) {
      return this.delegate.mergeToMap(object, map);
   }

   public DataResult<T> mergeToPrimitive(T object, T object2) {
      return this.delegate.mergeToPrimitive(object, object2);
   }

   public DataResult<Stream<Pair<T, T>>> getMapValues(T object) {
      return this.delegate.getMapValues(object);
   }

   public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T object) {
      return this.delegate.getMapEntries(object);
   }

   public T createMap(Map<T, T> map) {
      return (T)this.delegate.createMap(map);
   }

   public T createMap(Stream<Pair<T, T>> stream) {
      return (T)this.delegate.createMap(stream);
   }

   public DataResult<MapLike<T>> getMap(T object) {
      return this.delegate.getMap(object);
   }

   public DataResult<Stream<T>> getStream(T object) {
      return this.delegate.getStream(object);
   }

   public DataResult<Consumer<Consumer<T>>> getList(T object) {
      return this.delegate.getList(object);
   }

   public T createList(Stream<T> stream) {
      return (T)this.delegate.createList(stream);
   }

   public DataResult<ByteBuffer> getByteBuffer(T object) {
      return this.delegate.getByteBuffer(object);
   }

   public T createByteList(ByteBuffer byteBuffer) {
      return (T)this.delegate.createByteList(byteBuffer);
   }

   public DataResult<IntStream> getIntStream(T object) {
      return this.delegate.getIntStream(object);
   }

   public T createIntList(IntStream intStream) {
      return (T)this.delegate.createIntList(intStream);
   }

   public DataResult<LongStream> getLongStream(T object) {
      return this.delegate.getLongStream(object);
   }

   public T createLongList(LongStream longStream) {
      return (T)this.delegate.createLongList(longStream);
   }

   public T remove(T object, String string) {
      return (T)this.delegate.remove(object, string);
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

      protected DelegateListBuilder(final ListBuilder<T> original) {
         this.original = original;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public DataResult<T> build(T object) {
         return this.original.build(object);
      }

      public ListBuilder<T> add(T object) {
         this.original.add(object);
         return this;
      }

      public ListBuilder<T> add(DataResult<T> dataResult) {
         this.original.add(dataResult);
         return this;
      }

      public <E> ListBuilder<T> add(E object, Encoder<E> encoder) {
         this.original.add(encoder.encodeStart(this.ops(), object));
         return this;
      }

      public <E> ListBuilder<T> addAll(Iterable<E> iterable, Encoder<E> encoder) {
         iterable.forEach(object -> this.original.add(encoder.encode(object, this.ops(), this.ops().empty())));
         return this;
      }

      public ListBuilder<T> withErrorsFrom(DataResult<?> dataResult) {
         this.original.withErrorsFrom(dataResult);
         return this;
      }

      public ListBuilder<T> mapError(UnaryOperator<String> unaryOperator) {
         this.original.mapError(unaryOperator);
         return this;
      }

      public DataResult<T> build(DataResult<T> dataResult) {
         return this.original.build(dataResult);
      }
   }

   protected class DelegateRecordBuilder implements RecordBuilder<T> {
      private final RecordBuilder<T> original;

      protected DelegateRecordBuilder(final RecordBuilder<T> original) {
         this.original = original;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public RecordBuilder<T> add(T object, T object2) {
         this.original.add(object, object2);
         return this;
      }

      public RecordBuilder<T> add(T object, DataResult<T> dataResult) {
         this.original.add(object, dataResult);
         return this;
      }

      public RecordBuilder<T> add(DataResult<T> dataResult, DataResult<T> dataResult2) {
         this.original.add(dataResult, dataResult2);
         return this;
      }

      public RecordBuilder<T> add(String string, T object) {
         this.original.add(string, object);
         return this;
      }

      public RecordBuilder<T> add(String string, DataResult<T> dataResult) {
         this.original.add(string, dataResult);
         return this;
      }

      public <E> RecordBuilder<T> add(String string, E object, Encoder<E> encoder) {
         return this.original.add(string, encoder.encodeStart(this.ops(), object));
      }

      public RecordBuilder<T> withErrorsFrom(DataResult<?> dataResult) {
         this.original.withErrorsFrom(dataResult);
         return this;
      }

      public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
         this.original.setLifecycle(lifecycle);
         return this;
      }

      public RecordBuilder<T> mapError(UnaryOperator<String> unaryOperator) {
         this.original.mapError(unaryOperator);
         return this;
      }

      public DataResult<T> build(T object) {
         return this.original.build(object);
      }

      public DataResult<T> build(DataResult<T> dataResult) {
         return this.original.build(dataResult);
      }
   }
}
