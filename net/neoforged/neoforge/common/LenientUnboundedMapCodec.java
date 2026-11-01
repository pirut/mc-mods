package net.neoforged.neoforge.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.BaseMapCodec;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;

public class LenientUnboundedMapCodec<K, V> implements BaseMapCodec<K, V>, Codec<Map<K, V>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Codec<K> keyCodec;
   private final Codec<V> elementCodec;

   public LenientUnboundedMapCodec(Codec<K> keyCodec, Codec<V> elementCodec) {
      this.keyCodec = keyCodec;
      this.elementCodec = elementCodec;
   }

   public Codec<K> keyCodec() {
      return this.keyCodec;
   }

   public Codec<V> elementCodec() {
      return this.elementCodec;
   }

   public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
      Builder<K, V> read = ImmutableMap.builder();
      input.entries().forEach(pair -> {
         DataResult<K> k = this.keyCodec().parse(ops, pair.getFirst());
         DataResult<V> v = this.elementCodec().parse(ops, pair.getSecond());
         DataResult<Pair<K, V>> entry = k.apply2stable(Pair::of, v);
         entry.error().ifPresent(e -> LOGGER.error("Failed to decode key {} for value {}: {}", new Object[]{k, v, e}));
         entry.result().ifPresent(e -> read.put(e.getFirst(), e.getSecond()));
      });
      Map<K, V> elements = read.build();
      return DataResult.success(elements);
   }

   public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
      return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(map -> this.decode(ops, map)).map(r -> Pair.of(r, input));
   }

   public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
      return this.encode(input, ops, ops.mapBuilder()).build(prefix);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         LenientUnboundedMapCodec<?, ?> that = (LenientUnboundedMapCodec<?, ?>)o;
         return Objects.equals(this.keyCodec, that.keyCodec) && Objects.equals(this.elementCodec, that.elementCodec);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.keyCodec, this.elementCodec);
   }

   @Override
   public String toString() {
      return "LenientUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
   }
}
