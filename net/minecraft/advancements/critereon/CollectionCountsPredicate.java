package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionCountsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
   List<CollectionCountsPredicate.Entry<T, P>> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate<T, P>> codec(Codec<P> p_341206_) {
      return CollectionCountsPredicate.Entry.codec(p_341206_).listOf().xmap(CollectionCountsPredicate::of, CollectionCountsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(CollectionCountsPredicate.Entry<T, P>... p_341225_) {
      return of(List.of(p_341225_));
   }

   static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(List<CollectionCountsPredicate.Entry<T, P>> p_341102_) {
      return (CollectionCountsPredicate<T, P>)(switch (p_341102_.size()) {
         case 0 -> new CollectionCountsPredicate.Zero();
         case 1 -> new CollectionCountsPredicate.Single(p_341102_.getFirst());
         default -> new CollectionCountsPredicate.Multiple(p_341102_);
      });
   }

   public record Entry<T, P extends Predicate<T>>(P test, MinMaxBounds.Ints count) {
      public static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate.Entry<T, P>> codec(Codec<P> p_341286_) {
         return RecordCodecBuilder.create(
            p_340986_ -> p_340986_.group(
                  p_341286_.fieldOf("test").forGetter(CollectionCountsPredicate.Entry::test),
                  MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CollectionCountsPredicate.Entry::count)
               )
               .apply(p_340986_, CollectionCountsPredicate.Entry::new)
         );
      }

      public boolean test(Iterable<T> p_340995_) {
         int $$1 = 0;

         for (T $$2 : p_340995_) {
            if (this.test.test($$2)) {
               $$1++;
            }
         }

         return this.count.matches($$1);
      }
   }

   public record Multiple<T, P extends Predicate<T>>(List<CollectionCountsPredicate.Entry<T, P>> entries) implements CollectionCountsPredicate<T, P> {
      public boolean test(Iterable<T> p_340928_) {
         for (CollectionCountsPredicate.Entry<T, P> $$1 : this.entries) {
            if (!$$1.test(p_340928_)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return this.entries;
      }
   }

   public record Single<T, P extends Predicate<T>>(CollectionCountsPredicate.Entry<T, P> entry) implements CollectionCountsPredicate<T, P> {
      public boolean test(Iterable<T> p_341391_) {
         return this.entry.test(p_341391_);
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return List.of(this.entry);
      }
   }

   public static class Zero<T, P extends Predicate<T>> implements CollectionCountsPredicate<T, P> {
      public boolean test(Iterable<T> p_341222_) {
         return true;
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return List.of();
      }
   }
}
