package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtOps implements DynamicOps<Tag> {
   public static final NbtOps INSTANCE = new NbtOps();

   private NbtOps() {
   }

   public Tag empty() {
      return EndTag.INSTANCE;
   }

   public Tag emptyList() {
      return new ListTag();
   }

   public Tag emptyMap() {
      return new CompoundTag();
   }

   public <U> U convertTo(DynamicOps<U> ops, Tag tag) {
      return (U)(switch (tag) {
         case EndTag endTag -> (Object)ops.empty();
         case ByteTag(byte var34) -> (Object)ops.createByte(var34);
         case ShortTag(short var35) -> (Object)ops.createShort(var35);
         case IntTag(int var36) -> (Object)ops.createInt(var36);
         case LongTag(long var37) -> (Object)ops.createLong(var37);
         case FloatTag(float var38) -> (Object)ops.createFloat(var38);
         case DoubleTag(double var39) -> (Object)ops.createDouble(var39);
         case ByteArrayTag byteArrayTag -> (Object)ops.createByteList(ByteBuffer.wrap(byteArrayTag.getAsByteArray()));
         case StringTag(String var40) -> (Object)ops.createString(var40);
         case ListTag listTag -> (Object)this.convertList(ops, listTag);
         case CompoundTag compoundTag -> (Object)this.convertMap(ops, compoundTag);
         case IntArrayTag intArrayTag -> (Object)ops.createIntList(Arrays.stream(intArrayTag.getAsIntArray()));
         case LongArrayTag longArrayTag -> (Object)ops.createLongList(Arrays.stream(longArrayTag.getAsLongArray()));
         default -> throw new MatchException(null, null);
      });
   }

   public DataResult<Number> getNumberValue(Tag tag) {
      return tag.asNumber().<DataResult<Number>>map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Not a number"));
   }

   public Tag createNumeric(Number data) {
      return DoubleTag.valueOf(data.doubleValue());
   }

   public Tag createByte(byte data) {
      return ByteTag.valueOf(data);
   }

   public Tag createShort(short data) {
      return ShortTag.valueOf(data);
   }

   public Tag createInt(int data) {
      return IntTag.valueOf(data);
   }

   public Tag createLong(long data) {
      return LongTag.valueOf(data);
   }

   public Tag createFloat(float data) {
      return FloatTag.valueOf(data);
   }

   public Tag createDouble(double data) {
      return DoubleTag.valueOf(data);
   }

   public Tag createBoolean(boolean data) {
      return ByteTag.valueOf(data);
   }

   public DataResult<String> getStringValue(Tag tag) {
      return tag instanceof StringTag(String var4) ? DataResult.success(var4) : DataResult.error(() -> "Not a string");
   }

   public Tag createString(String data) {
      return StringTag.valueOf(data);
   }

   public DataResult<Tag> mergeToList(Tag list, Tag tag) {
      return createCollector(list)
         .map(listCollector -> DataResult.success(listCollector.accept(tag).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + list, list));
   }

   public DataResult<Tag> mergeToList(Tag list, List<Tag> tags) {
      return createCollector(list)
         .map(listCollector -> DataResult.success(listCollector.acceptAll(tags).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + list, list));
   }

   public DataResult<Tag> mergeToMap(Tag map, Tag key, Tag value) {
      if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
      } else if (key instanceof StringTag(String var10)) {
         String compoundTag = var10;
         CompoundTag compoundTag2 = map instanceof CompoundTag compoundTagx ? compoundTagx.shallowCopy() : new CompoundTag();
         compoundTag2.put(compoundTag, value);
         return DataResult.success(compoundTag2);
      } else {
         return DataResult.error(() -> "key is not a string: " + key, map);
      }
   }

   public DataResult<Tag> mergeToMap(Tag map, MapLike<Tag> otherMap) {
      if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
      } else {
         Iterator<Pair<Tag, Tag>> iterator = otherMap.entries().iterator();
         if (!iterator.hasNext()) {
            return map == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(map);
         } else {
            CompoundTag compoundTag2 = map instanceof CompoundTag compoundTag ? compoundTag.shallowCopy() : new CompoundTag();
            List<Tag> list = new ArrayList<>();
            iterator.forEachRemaining(pair -> {
               Tag tag = (Tag)pair.getFirst();
               if (tag instanceof StringTag(String string)) {
                  compoundTag2.put(string, (Tag)pair.getSecond());
               } else {
                  list.add(tag);
               }
            });
            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundTag2) : DataResult.success(compoundTag2);
         }
      }
   }

   public DataResult<Tag> mergeToMap(Tag tag, Map<Tag, Tag> map) {
      if (!(tag instanceof CompoundTag) && !(tag instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + tag, tag);
      } else if (map.isEmpty()) {
         return tag == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(tag);
      } else {
         CompoundTag compoundTag2 = tag instanceof CompoundTag compoundTag ? compoundTag.shallowCopy() : new CompoundTag();
         List<Tag> list = new ArrayList<>();

         for (Entry<Tag, Tag> entry : map.entrySet()) {
            Tag tag2 = entry.getKey();
            if (tag2 instanceof StringTag(String var10)) {
               compoundTag2.put(var10, entry.getValue());
            } else {
               list.add(tag2);
            }
         }

         return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundTag2) : DataResult.success(compoundTag2);
      }
   }

   public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag map) {
      return map instanceof CompoundTag compoundTag
         ? DataResult.success(compoundTag.entrySet().stream().map(entry -> Pair.of(this.createString(entry.getKey()), entry.getValue())))
         : DataResult.error(() -> "Not a map: " + map);
   }

   public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag map) {
      return map instanceof CompoundTag compoundTag ? DataResult.success((Consumer<BiConsumer>)biConsumer -> {
         for (Entry<String, Tag> entry : compoundTag.entrySet()) {
            biConsumer.accept(this.createString(entry.getKey()), entry.getValue());
         }
      }) : DataResult.error(() -> "Not a map: " + map);
   }

   public DataResult<MapLike<Tag>> getMap(Tag map) {
      return map instanceof CompoundTag compoundTag ? DataResult.success(new MapLike<Tag>() {
         @Nullable
         public Tag get(Tag tag) {
            if (tag instanceof StringTag(String var4)) {
               return compoundTag.get(var4);
            } else {
               throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + tag);
            }
         }

         @Nullable
         public Tag get(String string) {
            return compoundTag.get(string);
         }

         public Stream<Pair<Tag, Tag>> entries() {
            return compoundTag.entrySet().stream().map(entry -> Pair.of(NbtOps.this.createString(entry.getKey()), entry.getValue()));
         }

         @Override
         public String toString() {
            return "MapLike[" + compoundTag + "]";
         }
      }) : DataResult.error(() -> "Not a map: " + map);
   }

   public Tag createMap(Stream<Pair<Tag, Tag>> data) {
      CompoundTag compoundTag = new CompoundTag();
      data.forEach(pair -> {
         Tag tag = (Tag)pair.getFirst();
         Tag tag2 = (Tag)pair.getSecond();
         if (tag instanceof StringTag(String string)) {
            compoundTag.put(string, tag2);
         } else {
            throw new UnsupportedOperationException("Cannot create map with non-string key: " + tag);
         }
      });
      return compoundTag;
   }

   public DataResult<Stream<Tag>> getStream(Tag tag) {
      return tag instanceof CollectionTag collectionTag ? DataResult.success(collectionTag.stream()) : DataResult.error(() -> "Not a list");
   }

   public DataResult<Consumer<Consumer<Tag>>> getList(Tag tag) {
      return tag instanceof CollectionTag collectionTag ? DataResult.success(collectionTag::forEach) : DataResult.error(() -> "Not a list: " + tag);
   }

   public DataResult<ByteBuffer> getByteBuffer(Tag tag) {
      return tag instanceof ByteArrayTag byteArrayTag ? DataResult.success(ByteBuffer.wrap(byteArrayTag.getAsByteArray())) : super.getByteBuffer(tag);
   }

   public Tag createByteList(ByteBuffer data) {
      ByteBuffer byteBuffer = data.duplicate().clear();
      byte[] bs = new byte[data.capacity()];
      byteBuffer.get(0, bs, 0, bs.length);
      return new ByteArrayTag(bs);
   }

   public DataResult<IntStream> getIntStream(Tag tag) {
      return tag instanceof IntArrayTag intArrayTag ? DataResult.success(Arrays.stream(intArrayTag.getAsIntArray())) : super.getIntStream(tag);
   }

   public Tag createIntList(IntStream data) {
      return new IntArrayTag(data.toArray());
   }

   public DataResult<LongStream> getLongStream(Tag tag) {
      return tag instanceof LongArrayTag longArrayTag ? DataResult.success(Arrays.stream(longArrayTag.getAsLongArray())) : super.getLongStream(tag);
   }

   public Tag createLongList(LongStream data) {
      return new LongArrayTag(data.toArray());
   }

   public Tag createList(Stream<Tag> data) {
      return new ListTag(data.collect(Util.toMutableList()));
   }

   public Tag remove(Tag map, String removeKey) {
      if (map instanceof CompoundTag compoundTag) {
         CompoundTag compoundTag2 = compoundTag.shallowCopy();
         compoundTag2.remove(removeKey);
         return compoundTag2;
      } else {
         return map;
      }
   }

   @Override
   public String toString() {
      return "NBT";
   }

   public RecordBuilder<Tag> mapBuilder() {
      return new NbtOps.NbtRecordBuilder();
   }

   private static Optional<NbtOps.ListCollector> createCollector(Tag tag) {
      if (tag instanceof EndTag) {
         return Optional.of(new NbtOps.GenericListCollector());
      } else if (tag instanceof CollectionTag collectionTag) {
         if (collectionTag.isEmpty()) {
            return Optional.of(new NbtOps.GenericListCollector());
         } else {
            return switch (collectionTag) {
               case ListTag listTag -> Optional.of(new NbtOps.GenericListCollector(listTag));
               case ByteArrayTag byteArrayTag -> Optional.of(new NbtOps.ByteListCollector(byteArrayTag.getAsByteArray()));
               case IntArrayTag intArrayTag -> Optional.of(new NbtOps.IntListCollector(intArrayTag.getAsIntArray()));
               case LongArrayTag longArrayTag -> Optional.of(new NbtOps.LongListCollector(longArrayTag.getAsLongArray()));
               default -> throw new MatchException(null, null);
            };
         }
      } else {
         return Optional.empty();
      }
   }

   static class ByteListCollector implements NbtOps.ListCollector {
      private final ByteArrayList values = new ByteArrayList();

      public ByteListCollector(byte[] values) {
         this.values.addElements(0, values);
      }

      @Override
      public NbtOps.ListCollector accept(Tag tag) {
         if (tag instanceof ByteTag byteTag) {
            this.values.add(byteTag.byteValue());
            return this;
         } else {
            return new NbtOps.GenericListCollector(this.values).accept(tag);
         }
      }

      @Override
      public Tag result() {
         return new ByteArrayTag(this.values.toByteArray());
      }
   }

   static class GenericListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      GenericListCollector() {
      }

      GenericListCollector(ListTag list) {
         this.result.addAll(list);
      }

      public GenericListCollector(IntArrayList list) {
         list.forEach(i -> this.result.add(IntTag.valueOf(i)));
      }

      public GenericListCollector(ByteArrayList list) {
         list.forEach(b -> this.result.add(ByteTag.valueOf(b)));
      }

      public GenericListCollector(LongArrayList list) {
         list.forEach(l -> this.result.add(LongTag.valueOf(l)));
      }

      @Override
      public NbtOps.ListCollector accept(Tag tag) {
         this.result.add(tag);
         return this;
      }

      @Override
      public Tag result() {
         return this.result;
      }
   }

   static class IntListCollector implements NbtOps.ListCollector {
      private final IntArrayList values = new IntArrayList();

      public IntListCollector(int[] values) {
         this.values.addElements(0, values);
      }

      @Override
      public NbtOps.ListCollector accept(Tag tag) {
         if (tag instanceof IntTag intTag) {
            this.values.add(intTag.intValue());
            return this;
         } else {
            return new NbtOps.GenericListCollector(this.values).accept(tag);
         }
      }

      @Override
      public Tag result() {
         return new IntArrayTag(this.values.toIntArray());
      }
   }

   interface ListCollector {
      NbtOps.ListCollector accept(Tag tag);

      default NbtOps.ListCollector acceptAll(Iterable<Tag> tags) {
         NbtOps.ListCollector listCollector = this;

         for (Tag tag : tags) {
            listCollector = listCollector.accept(tag);
         }

         return listCollector;
      }

      default NbtOps.ListCollector acceptAll(Stream<Tag> tags) {
         return this.acceptAll(tags::iterator);
      }

      Tag result();
   }

   static class LongListCollector implements NbtOps.ListCollector {
      private final LongArrayList values = new LongArrayList();

      public LongListCollector(long[] values) {
         this.values.addElements(0, values);
      }

      @Override
      public NbtOps.ListCollector accept(Tag tag) {
         if (tag instanceof LongTag longTag) {
            this.values.add(longTag.longValue());
            return this;
         } else {
            return new NbtOps.GenericListCollector(this.values).accept(tag);
         }
      }

      @Override
      public Tag result() {
         return new LongArrayTag(this.values.toLongArray());
      }
   }

   class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
      protected NbtRecordBuilder() {
         super(NbtOps.this);
      }

      protected CompoundTag initBuilder() {
         return new CompoundTag();
      }

      protected CompoundTag append(String key, Tag value, CompoundTag tag) {
         tag.put(key, value);
         return tag;
      }

      protected DataResult<Tag> build(CompoundTag compoundTag, Tag tag) {
         if (tag == null || tag == EndTag.INSTANCE) {
            return DataResult.success(compoundTag);
         } else if (!(tag instanceof CompoundTag compoundTag2)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + tag, tag);
         } else {
            CompoundTag compoundTag3 = compoundTag2.shallowCopy();

            for (Entry<String, Tag> entry : compoundTag.entrySet()) {
               compoundTag3.put(entry.getKey(), entry.getValue());
            }

            return DataResult.success(compoundTag3);
         }
      }
   }
}
