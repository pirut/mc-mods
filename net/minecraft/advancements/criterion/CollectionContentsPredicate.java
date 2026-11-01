package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionContentsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
   List<P> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionContentsPredicate<T, P>> codec(Codec<P> p_467300_) {
      return p_467300_.listOf().xmap(CollectionContentsPredicate::of, CollectionContentsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(P... p_467726_) {
      return of(List.of(p_467726_));
   }

   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(List<P> p_467054_) {
      return (CollectionContentsPredicate<T, P>)(switch (p_467054_.size()) {
         case 0 -> new CollectionContentsPredicate.Zero();
         case 1 -> new CollectionContentsPredicate.Single(p_467054_.getFirst());
         default -> new CollectionContentsPredicate.Multiple(p_467054_);
      });
   }

   public record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> p_467817_) {
         List<Predicate<T>> list = new ArrayList<>(this.tests);

         for (T t : p_467817_) {
            list.removeIf(p_467138_ -> p_467138_.test(t));
            if (list.isEmpty()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public List<P> unpack() {
         return this.tests;
      }
   }

   public record Single<T, P extends Predicate<T>>(P test) implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> p_469755_) {
         for (T t : p_469755_) {
            if (this.test.test(t)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public List<P> unpack() {
         return List.of(this.test);
      }
   }

   public static class Zero<T, P extends Predicate<T>> implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> p_469779_) {
         return true;
      }

      @Override
      public List<P> unpack() {
         return List.of();
      }
   }
}
