package net.minecraft.world.level.storage.loot.functions;

import java.util.Arrays;
import java.util.function.Function;

public interface FunctionUserBuilder<T extends FunctionUserBuilder<T>> {
   T apply(LootItemFunction.Builder var1);

   default <E> T apply(Iterable<E> p_230985_, Function<E, LootItemFunction.Builder> p_230986_) {
      T $$2 = this.unwrap();

      for (E $$3 : p_230985_) {
         $$2 = $$2.apply(p_230986_.apply($$3));
      }

      return $$2;
   }

   default <E> T apply(E[] p_230988_, Function<E, LootItemFunction.Builder> p_230989_) {
      return this.apply(Arrays.asList(p_230988_), p_230989_);
   }

   T unwrap();
}
