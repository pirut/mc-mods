package net.minecraft.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class HashOps implements DynamicOps<HashCode> {
   private static final byte TAG_EMPTY = 1;
   private static final byte TAG_MAP_START = 2;
   private static final byte TAG_MAP_END = 3;
   private static final byte TAG_LIST_START = 4;
   private static final byte TAG_LIST_END = 5;
   private static final byte TAG_BYTE = 6;
   private static final byte TAG_SHORT = 7;
   private static final byte TAG_INT = 8;
   private static final byte TAG_LONG = 9;
   private static final byte TAG_FLOAT = 10;
   private static final byte TAG_DOUBLE = 11;
   private static final byte TAG_STRING = 12;
   private static final byte TAG_BOOLEAN = 13;
   private static final byte TAG_BYTE_ARRAY_START = 14;
   private static final byte TAG_BYTE_ARRAY_END = 15;
   private static final byte TAG_INT_ARRAY_START = 16;
   private static final byte TAG_INT_ARRAY_END = 17;
   private static final byte TAG_LONG_ARRAY_START = 18;
   private static final byte TAG_LONG_ARRAY_END = 19;
   private static final byte[] EMPTY_PAYLOAD = new byte[]{1};
   private static final byte[] FALSE_PAYLOAD = new byte[]{13, 0};
   private static final byte[] TRUE_PAYLOAD = new byte[]{13, 1};
   public static final byte[] EMPTY_MAP_PAYLOAD = new byte[]{2, 3};
   public static final byte[] EMPTY_LIST_PAYLOAD = new byte[]{4, 5};
   private static final DataResult<Object> UNSUPPORTED_OPERATION_ERROR = DataResult.error(() -> "Unsupported operation");
   private static final Comparator<HashCode> HASH_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
   private static final Comparator<Entry<HashCode, HashCode>> MAP_ENTRY_ORDER = Entry.<HashCode, HashCode>comparingByKey(HASH_COMPARATOR)
      .thenComparing(Entry.comparingByValue(HASH_COMPARATOR));
   private static final Comparator<Pair<HashCode, HashCode>> MAPLIKE_ENTRY_ORDER = Comparator.<Pair<HashCode, HashCode>, HashCode>comparing(
         Pair::getFirst, HASH_COMPARATOR
      )
      .thenComparing(Pair::getSecond, HASH_COMPARATOR);
   public static final HashOps CRC32C_INSTANCE = new HashOps(Hashing.crc32c());
   final HashFunction hashFunction;
   final HashCode empty;
   private final HashCode emptyMap;
   private final HashCode emptyList;
   private final HashCode trueHash;
   private final HashCode falseHash;

   public HashOps(HashFunction p_412557_) {
      this.hashFunction = p_412557_;
      this.empty = p_412557_.hashBytes(EMPTY_PAYLOAD);
      this.emptyMap = p_412557_.hashBytes(EMPTY_MAP_PAYLOAD);
      this.emptyList = p_412557_.hashBytes(EMPTY_LIST_PAYLOAD);
      this.falseHash = p_412557_.hashBytes(FALSE_PAYLOAD);
      this.trueHash = p_412557_.hashBytes(TRUE_PAYLOAD);
   }

   public HashCode empty() {
      return this.empty;
   }

   public HashCode emptyMap() {
      return this.emptyMap;
   }

   public HashCode emptyList() {
      return this.emptyList;
   }

   public HashCode createNumeric(Number p_412443_) {
      return switch (p_412443_) {
         case Byte obyte -> this.createByte(obyte);
         case Short oshort -> this.createShort(oshort);
         case Integer integer -> this.createInt(integer);
         case Long olong -> this.createLong(olong);
         case Double d0 -> this.createDouble(d0);
         case Float f -> this.createFloat(f);
         default -> this.createDouble(p_412443_.doubleValue());
      };
   }

   public HashCode createByte(byte p_412636_) {
      return this.hashFunction.newHasher(2).putByte((byte)6).putByte(p_412636_).hash();
   }

   public HashCode createShort(short p_412616_) {
      return this.hashFunction.newHasher(3).putByte((byte)7).putShort(p_412616_).hash();
   }

   public HashCode createInt(int p_412683_) {
      return this.hashFunction.newHasher(5).putByte((byte)8).putInt(p_412683_).hash();
   }

   public HashCode createLong(long p_412271_) {
      return this.hashFunction.newHasher(9).putByte((byte)9).putLong(p_412271_).hash();
   }

   public HashCode createFloat(float p_412641_) {
      return this.hashFunction.newHasher(5).putByte((byte)10).putFloat(p_412641_).hash();
   }

   public HashCode createDouble(double p_412066_) {
      return this.hashFunction.newHasher(9).putByte((byte)11).putDouble(p_412066_).hash();
   }

   public HashCode createString(String p_412211_) {
      return this.hashFunction.newHasher().putByte((byte)12).putInt(p_412211_.length()).putUnencodedChars(p_412211_).hash();
   }

   public HashCode createBoolean(boolean p_412649_) {
      return p_412649_ ? this.trueHash : this.falseHash;
   }

   private static Hasher hashMap(Hasher p_412213_, Map<HashCode, HashCode> p_412494_) {
      p_412213_.putByte((byte)2);
      p_412494_.entrySet()
         .stream()
         .sorted(MAP_ENTRY_ORDER)
         .forEach(p_412485_ -> p_412213_.putBytes(p_412485_.getKey().asBytes()).putBytes(p_412485_.getValue().asBytes()));
      p_412213_.putByte((byte)3);
      return p_412213_;
   }

   static Hasher hashMap(Hasher p_412189_, Stream<Pair<HashCode, HashCode>> p_412324_) {
      p_412189_.putByte((byte)2);
      p_412324_.sorted(MAPLIKE_ENTRY_ORDER)
         .forEach(p_412404_ -> p_412189_.putBytes(((HashCode)p_412404_.getFirst()).asBytes()).putBytes(((HashCode)p_412404_.getSecond()).asBytes()));
      p_412189_.putByte((byte)3);
      return p_412189_;
   }

   public HashCode createMap(Stream<Pair<HashCode, HashCode>> p_412416_) {
      return hashMap(this.hashFunction.newHasher(), p_412416_).hash();
   }

   public HashCode createMap(Map<HashCode, HashCode> p_412512_) {
      return hashMap(this.hashFunction.newHasher(), p_412512_).hash();
   }

   public HashCode createList(Stream<HashCode> p_412225_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)4);
      p_412225_.forEach(p_412370_ -> hasher.putBytes(p_412370_.asBytes()));
      hasher.putByte((byte)5);
      return hasher.hash();
   }

   public HashCode createByteList(ByteBuffer p_412081_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)14);
      hasher.putBytes(p_412081_);
      hasher.putByte((byte)15);
      return hasher.hash();
   }

   public HashCode createIntList(IntStream p_412517_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)16);
      p_412517_.forEach(hasher::putInt);
      hasher.putByte((byte)17);
      return hasher.hash();
   }

   public HashCode createLongList(LongStream p_412654_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)18);
      p_412654_.forEach(hasher::putLong);
      hasher.putByte((byte)19);
      return hasher.hash();
   }

   public HashCode remove(HashCode p_412437_, String p_412123_) {
      return p_412437_;
   }

   public RecordBuilder<HashCode> mapBuilder() {
      return new HashOps.MapHashBuilder();
   }

   public ListBuilder<HashCode> listBuilder() {
      return new HashOps.ListHashBuilder();
   }

   @Override
   public String toString() {
      return "Hash " + this.hashFunction;
   }

   public <U> U convertTo(DynamicOps<U> p_412264_, HashCode p_412248_) {
      throw new UnsupportedOperationException("Can't convert from this type");
   }

   public Number getNumberValue(HashCode p_412545_, Number p_412129_) {
      return p_412129_;
   }

   public HashCode set(HashCode p_412708_, String p_412229_, HashCode p_412577_) {
      return p_412708_;
   }

   public HashCode update(HashCode p_412258_, String p_412582_, Function<HashCode, HashCode> p_412141_) {
      return p_412258_;
   }

   public HashCode updateGeneric(HashCode p_412276_, HashCode p_412115_, Function<HashCode, HashCode> p_412233_) {
      return p_412276_;
   }

   private static <T> DataResult<T> unsupported() {
      return (DataResult<T>)UNSUPPORTED_OPERATION_ERROR;
   }

   public DataResult<HashCode> get(HashCode p_412426_, String p_412356_) {
      return unsupported();
   }

   public DataResult<HashCode> getGeneric(HashCode p_412138_, HashCode p_412331_) {
      return unsupported();
   }

   public DataResult<Number> getNumberValue(HashCode p_412281_) {
      return unsupported();
   }

   public DataResult<Boolean> getBooleanValue(HashCode p_412639_) {
      return unsupported();
   }

   public DataResult<String> getStringValue(HashCode p_412375_) {
      return unsupported();
   }

   boolean isEmpty(HashCode p_470813_) {
      return p_470813_.equals(this.empty);
   }

   public DataResult<HashCode> mergeToList(HashCode p_412567_, HashCode p_412638_) {
      return this.isEmpty(p_412567_) ? DataResult.success(this.createList(Stream.of(p_412638_))) : unsupported();
   }

   public DataResult<HashCode> mergeToList(HashCode p_412195_, List<HashCode> p_412670_) {
      return this.isEmpty(p_412195_) ? DataResult.success(this.createList(p_412670_.stream())) : unsupported();
   }

   public DataResult<HashCode> mergeToMap(HashCode p_412061_, HashCode p_412446_, HashCode p_412770_) {
      return this.isEmpty(p_412061_) ? DataResult.success(this.createMap(Map.of(p_412446_, p_412770_))) : unsupported();
   }

   public DataResult<HashCode> mergeToMap(HashCode p_412263_, Map<HashCode, HashCode> p_412681_) {
      return this.isEmpty(p_412263_) ? DataResult.success(this.createMap(p_412681_)) : unsupported();
   }

   public DataResult<HashCode> mergeToMap(HashCode p_412371_, MapLike<HashCode> p_412753_) {
      return this.isEmpty(p_412371_) ? DataResult.success(this.createMap(p_412753_.entries())) : unsupported();
   }

   public DataResult<Stream<Pair<HashCode, HashCode>>> getMapValues(HashCode p_412515_) {
      return unsupported();
   }

   public DataResult<Consumer<BiConsumer<HashCode, HashCode>>> getMapEntries(HashCode p_412308_) {
      return unsupported();
   }

   public DataResult<Stream<HashCode>> getStream(HashCode p_412047_) {
      return unsupported();
   }

   public DataResult<Consumer<Consumer<HashCode>>> getList(HashCode p_412386_) {
      return unsupported();
   }

   public DataResult<MapLike<HashCode>> getMap(HashCode p_412339_) {
      return unsupported();
   }

   public DataResult<ByteBuffer> getByteBuffer(HashCode p_412476_) {
      return unsupported();
   }

   public DataResult<IntStream> getIntStream(HashCode p_412744_) {
      return unsupported();
   }

   public DataResult<LongStream> getLongStream(HashCode p_412105_) {
      return unsupported();
   }

   class ListHashBuilder extends AbstractListBuilder<HashCode, Hasher> {
      public ListHashBuilder() {
         super(HashOps.this);
      }

      protected Hasher initBuilder() {
         return HashOps.this.hashFunction.newHasher().putByte((byte)4);
      }

      protected Hasher append(Hasher p_412586_, HashCode p_412043_) {
         return p_412586_.putBytes(p_412043_.asBytes());
      }

      protected DataResult<HashCode> build(Hasher p_412335_, HashCode p_412267_) {
         assert p_412267_.equals(HashOps.this.empty);

         p_412335_.putByte((byte)5);
         return DataResult.success(p_412335_.hash());
      }
   }

   final class MapHashBuilder extends AbstractUniversalBuilder<HashCode, List<Pair<HashCode, HashCode>>> {
      public MapHashBuilder() {
         super(HashOps.this);
      }

      protected List<Pair<HashCode, HashCode>> initBuilder() {
         return new ArrayList<>();
      }

      protected List<Pair<HashCode, HashCode>> append(HashCode p_412594_, HashCode p_412246_, List<Pair<HashCode, HashCode>> p_412564_) {
         p_412564_.add(Pair.of(p_412594_, p_412246_));
         return p_412564_;
      }

      protected DataResult<HashCode> build(List<Pair<HashCode, HashCode>> p_412648_, HashCode p_412523_) {
         assert HashOps.this.isEmpty(p_412523_);

         return DataResult.success(HashOps.hashMap(HashOps.this.hashFunction.newHasher(), p_412648_.stream()).hash());
      }
   }
}
