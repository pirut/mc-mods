package net.minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class Util$10 implements Function<T, R> {
   private final Map<T, R> cache;

   Util$10(Function var1) {
      this.val$function = var1;
      this.cache = new ConcurrentHashMap();
   }

   @Override
   public R apply(T p_214691_) {
      return (R)this.cache.computeIfAbsent(p_214691_, this.val$function);
   }

   @Override
   public String toString() {
      return "memoize/1[function=" + this.val$function + ", size=" + this.cache.size() + "]";
   }
}
