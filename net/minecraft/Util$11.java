package net.minecraft;

import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

class Util$11 implements BiFunction<T, U, R> {
   private final Map<Pair<T, U>, R> cache;

   Util$11(BiFunction var1) {
      this.val$function = var1;
      this.cache = new ConcurrentHashMap<>();
   }

   @Override
   public R apply(T p_214700_, U p_214701_) {
      return (R)this.cache.computeIfAbsent(Pair.of(p_214700_, p_214701_), p_214698_ -> p_214697_.apply(p_214698_.getFirst(), p_214698_.getSecond()));
   }

   @Override
   public String toString() {
      return "memoize/2[function=" + this.val$function + ", size=" + this.cache.size() + "]";
   }
}
