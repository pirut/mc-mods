package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Function;

public interface ConditionUserBuilder<T extends ConditionUserBuilder<T>> {
   T when(LootItemCondition.Builder var1);

   default <E> T when(Iterable<E> p_231041_, Function<E, LootItemCondition.Builder> p_231042_) {
      T $$2 = this.unwrap();

      for (E $$3 : p_231041_) {
         $$2 = $$2.when(p_231042_.apply($$3));
      }

      return $$2;
   }

   T unwrap();
}
