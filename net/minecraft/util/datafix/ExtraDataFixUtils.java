package net.minecraft.util.datafix;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ExtraDataFixUtils {
   public static Dynamic<?> fixBlockPos(Dynamic<?> p_326292_) {
      Optional<Number> $$1 = p_326292_.get("X").asNumber().result();
      Optional<Number> $$2 = p_326292_.get("Y").asNumber().result();
      Optional<Number> $$3 = p_326292_.get("Z").asNumber().result();
      return !$$1.isEmpty() && !$$2.isEmpty() && !$$3.isEmpty()
         ? p_326292_.createIntList(IntStream.of($$1.get().intValue(), $$2.get().intValue(), $$3.get().intValue()))
         : p_326292_;
   }

   public static <T, R> Typed<R> cast(Type<R> p_330690_, Typed<T> p_331921_) {
      return new Typed(p_330690_, p_331921_.getOps(), p_331921_.getValue());
   }

   @SafeVarargs
   public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>>... p_344769_) {
      return p_345927_ -> {
         for (Function<Typed<?>, Typed<?>> $$2 : p_344769_) {
            p_345927_ = $$2.apply(p_345927_);
         }

         return p_345927_;
      };
   }
}
