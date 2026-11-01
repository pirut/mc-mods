package net.neoforged.neoforge.common.extensions;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import org.apache.commons.lang3.function.TriConsumer;

public interface IFriendlyByteBufExtension {
   private FriendlyByteBuf self() {
      return (FriendlyByteBuf)this;
   }

   default <T> void writeObjectCollection(Collection<T> set, BiConsumer<T, FriendlyByteBuf> writer) {
      this.self().writeCollection(set, (buf, t) -> writer.accept(t, buf));
   }

   default <T> T[] readArray(IntFunction<T[]> builder, StreamDecoder<? super FriendlyByteBuf, T> reader) {
      int size = this.self().readVarInt();
      T[] array = (T[])builder.apply(size);

      for (int i = 0; i < size; i++) {
         array[i] = reader.decode(this.self());
      }

      return array;
   }

   default <T> FriendlyByteBuf writeArray(T[] array, StreamEncoder<? super FriendlyByteBuf, T> writer) {
      this.self().writeVarInt(array.length);

      for (T t : array) {
         writer.encode(this.self(), t);
      }

      return this.self();
   }

   default FriendlyByteBuf writeByte(byte value) {
      return this.self().writeByte(value);
   }

   default <K, V> Map<K, V> readMap(StreamDecoder<? super FriendlyByteBuf, K> keyReader, BiFunction<FriendlyByteBuf, K, V> valueReader) {
      int size = this.self().readVarInt();
      Map<K, V> map = Maps.newHashMapWithExpectedSize(size);

      for (int i = 0; i < size; i++) {
         K k = keyReader.decode(this.self());
         map.put(k, valueReader.apply(this.self(), k));
      }

      return map;
   }

   default <K, V> void writeMap(Map<K, V> map, StreamEncoder<? super FriendlyByteBuf, K> keyWriter, TriConsumer<FriendlyByteBuf, K, V> valueWriter) {
      this.self().writeVarInt(map.size());
      map.forEach((key, value) -> {
         keyWriter.encode(this.self(), (K)key);
         valueWriter.accept(this.self(), key, value);
      });
   }
}
