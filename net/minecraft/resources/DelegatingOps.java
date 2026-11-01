package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.ListBuilder.Builder;
import com.mojang.serialization.RecordBuilder.MapBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
      return (U)this.delegate.convertTo(p_135470_, p_135471_);
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
      return new Builder(this);
   }

   public RecordBuilder<T> mapBuilder() {
      return new MapBuilder(this);
   }
}
