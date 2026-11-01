package net.minecraft.world.entity.variant;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public interface PriorityProvider<Context, Condition extends PriorityProvider.SelectorCondition<Context>> {
   List<PriorityProvider.Selector<Context, Condition>> selectors();

   static <C, T> Stream<T> select(Stream<T> p_399981_, Function<T, PriorityProvider<C, ?>> p_399700_, C p_400086_) {
      List<PriorityProvider.UnpackedEntry<C, T>> list = new ArrayList<>();
      p_399981_.forEach(
         p_399951_ -> {
            PriorityProvider<C, ?> priorityprovider = p_399700_.apply((T)p_399951_);

            for (PriorityProvider.Selector<C, ?> selector : priorityprovider.selectors()) {
               list.add(
                  new PriorityProvider.UnpackedEntry<>(
                     (T)p_399951_,
                     selector.priority(),
                     (PriorityProvider.SelectorCondition<C>)DataFixUtils.orElseGet(selector.condition(), PriorityProvider.SelectorCondition::alwaysTrue)
                  )
               );
            }
         }
      );
      list.sort(PriorityProvider.UnpackedEntry.HIGHEST_PRIORITY_FIRST);
      Iterator<PriorityProvider.UnpackedEntry<C, T>> iterator = list.iterator();
      int i = Integer.MIN_VALUE;

      while (iterator.hasNext()) {
         PriorityProvider.UnpackedEntry<C, T> unpackedentry = iterator.next();
         if (unpackedentry.priority < i) {
            iterator.remove();
         } else if (unpackedentry.condition.test(p_400086_)) {
            i = unpackedentry.priority;
         } else {
            iterator.remove();
         }
      }

      return list.stream().map(PriorityProvider.UnpackedEntry::entry);
   }

   static <C, T> Optional<T> pick(Stream<T> p_399512_, Function<T, PriorityProvider<C, ?>> p_399827_, RandomSource p_399605_, C p_400048_) {
      List<T> list = select(p_399512_, p_399827_, p_400048_).toList();
      return Util.getRandomSafe(list, p_399605_);
   }

   static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> single(
      Condition p_400158_, int p_399749_
   ) {
      return List.of(new PriorityProvider.Selector<>(p_400158_, p_399749_));
   }

   static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> alwaysTrue(int p_400309_) {
      return List.of(new PriorityProvider.Selector<>(Optional.empty(), p_400309_));
   }

   public record Selector<Context, Condition extends PriorityProvider.SelectorCondition<Context>>(Optional<Condition> condition, int priority) {
      public Selector(Condition p_400069_, int p_400304_) {
         this(Optional.of(p_400069_), p_400304_);
      }

      public Selector(int p_399666_) {
         this(Optional.empty(), p_399666_);
      }

      public static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> Codec<PriorityProvider.Selector<Context, Condition>> codec(
         Codec<Condition> p_399682_
      ) {
         return RecordCodecBuilder.create(
            p_399853_ -> p_399853_.group(
                  p_399682_.optionalFieldOf("condition").forGetter(PriorityProvider.Selector::condition),
                  Codec.INT.fieldOf("priority").forGetter(PriorityProvider.Selector::priority)
               )
               .apply(p_399853_, PriorityProvider.Selector::new)
         );
      }
   }

   @FunctionalInterface
   public interface SelectorCondition<C> extends Predicate<C> {
      static <C> PriorityProvider.SelectorCondition<C> alwaysTrue() {
         return p_400277_ -> true;
      }
   }

   public record UnpackedEntry<C, T>(T entry, int priority, PriorityProvider.SelectorCondition<C> condition) {
      public static final Comparator<PriorityProvider.UnpackedEntry<?, ?>> HIGHEST_PRIORITY_FIRST = Comparator.comparingInt(
            PriorityProvider.UnpackedEntry::priority
         )
         .reversed();
   }
}
