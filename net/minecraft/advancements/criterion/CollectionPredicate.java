package net.minecraft.advancements.criterion;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;

public record CollectionPredicate<T, P extends Predicate<T>>(
   Optional<CollectionContentsPredicate<T, P>> contains, Optional<CollectionCountsPredicate<T, P>> counts, Optional<MinMaxBounds.Ints> size
) implements Predicate<Iterable<T>> {
   public static <T, P extends Predicate<T>> Codec<CollectionPredicate<T, P>> codec(Codec<P> p_468043_) {
      return RecordCodecBuilder.create(
         p_468783_ -> p_468783_.group(
               CollectionContentsPredicate.codec(p_468043_).optionalFieldOf("contains").forGetter(CollectionPredicate::contains),
               CollectionCountsPredicate.codec(p_468043_).optionalFieldOf("count").forGetter(CollectionPredicate::counts),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("size").forGetter(CollectionPredicate::size)
            )
            .apply(p_468783_, CollectionPredicate::new)
      );
   }

   public boolean test(Iterable<T> p_468266_) {
      if (this.contains.isPresent() && !this.contains.get().test(p_468266_)) {
         return false;
      } else {
         return this.counts.isPresent() && !this.counts.get().test(p_468266_)
            ? false
            : !this.size.isPresent() || this.size.get().matches(Iterables.size(p_468266_));
      }
   }
}
