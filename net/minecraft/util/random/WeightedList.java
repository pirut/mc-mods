package net.minecraft.util.random;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public final class WeightedList<E> {
   private static final int FLAT_THRESHOLD = 64;
   private final int totalWeight;
   private final List<Weighted<E>> items;
   @Nullable
   private final WeightedList.Selector<E> selector;

   WeightedList(List<? extends Weighted<E>> p_393913_) {
      this.items = List.copyOf(p_393913_);
      this.totalWeight = WeightedRandom.getTotalWeight(p_393913_, Weighted::weight);
      if (this.totalWeight == 0) {
         this.selector = null;
      } else if (this.totalWeight < 64) {
         this.selector = new WeightedList.Flat<>(this.items, this.totalWeight);
      } else {
         this.selector = new WeightedList.Compact<>(this.items);
      }
   }

   public static <E> WeightedList<E> of() {
      return new WeightedList<>(List.of());
   }

   public static <E> WeightedList<E> of(E p_394135_) {
      return new WeightedList<>(List.of(new Weighted<>(p_394135_, 1)));
   }

   @SafeVarargs
   public static <E> WeightedList<E> of(Weighted<E>... p_394617_) {
      return new WeightedList<>(List.of(p_394617_));
   }

   public static <E> WeightedList<E> of(List<Weighted<E>> p_394359_) {
      return new WeightedList<>(p_394359_);
   }

   public static <E> WeightedList.Builder<E> builder() {
      return new WeightedList.Builder<>();
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public <T> WeightedList<T> map(Function<E, T> p_409869_) {
      return new WeightedList(Lists.transform(this.items, p_409789_ -> p_409789_.map(p_409869_)));
   }

   public Optional<E> getRandom(RandomSource p_393707_) {
      if (this.selector == null) {
         return Optional.empty();
      } else {
         int i = p_393707_.nextInt(this.totalWeight);
         return Optional.of(this.selector.get(i));
      }
   }

   public E getRandomOrThrow(RandomSource p_394082_) {
      if (this.selector == null) {
         throw new IllegalStateException("Weighted list has no elements");
      } else {
         int i = p_394082_.nextInt(this.totalWeight);
         return this.selector.get(i);
      }
   }

   public List<Weighted<E>> unwrap() {
      return this.items;
   }

   public static <E> Codec<WeightedList<E>> codec(Codec<E> p_394327_) {
      return Weighted.codec(p_394327_).listOf().xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> codec(MapCodec<E> p_393650_) {
      return Weighted.codec(p_393650_).listOf().xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> nonEmptyCodec(Codec<E> p_393785_) {
      return ExtraCodecs.nonEmptyList(Weighted.codec(p_393785_).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> nonEmptyCodec(MapCodec<E> p_394431_) {
      return ExtraCodecs.nonEmptyList(Weighted.codec(p_394431_).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E, B extends ByteBuf> StreamCodec<B, WeightedList<E>> streamCodec(StreamCodec<B, E> p_437214_) {
      return Weighted.streamCodec(p_437214_).apply(ByteBufCodecs.list()).map(WeightedList::of, WeightedList::unwrap);
   }

   public boolean contains(E p_397638_) {
      for (Weighted<E> weighted : this.items) {
         if (weighted.value().equals(p_397638_)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean equals(@Nullable Object p_393762_) {
      if (this == p_393762_) {
         return true;
      } else {
         return p_393762_ instanceof WeightedList<?> weightedlist
            ? this.totalWeight == weightedlist.totalWeight && Objects.equals(this.items, weightedlist.items)
            : false;
      }
   }

   @Override
   public int hashCode() {
      int i = this.totalWeight;
      return 31 * i + this.items.hashCode();
   }

   public static class Builder<E> {
      private final List<Weighted<E>> result = new ArrayList<>();

      public WeightedList.Builder<E> add(E p_394625_) {
         return this.add(p_394625_, 1);
      }

      public WeightedList.Builder<E> add(E p_394058_, int p_393949_) {
         this.result.add(new Weighted<>(p_394058_, p_393949_));
         return this;
      }

      public WeightedList.Builder<E> add(Weighted<E> value) {
         this.result.add(value);
         return this;
      }

      public WeightedList.Builder<E> addAll(WeightedList<E> values) {
         return this.addAll(values.unwrap());
      }

      public WeightedList.Builder<E> addAll(Collection<Weighted<E>> values) {
         this.result.addAll(values);
         return this;
      }

      public WeightedList.Builder<E> remove(Weighted<E> value) {
         this.result.remove(value);
         return this;
      }

      public WeightedList.Builder<E> remove(E value) {
         this.result.removeIf(weighted -> weighted.value().equals(value));
         return this;
      }

      public WeightedList.Builder<E> removeIf(Predicate<Weighted<E>> filter) {
         this.result.removeIf(filter);
         return this;
      }

      public List<Weighted<E>> getList() {
         return Collections.unmodifiableList(this.result);
      }

      public WeightedList<E> build() {
         return new WeightedList<>(this.result);
      }
   }

   static class Compact<E> implements WeightedList.Selector<E> {
      private final Weighted<?>[] entries;

      Compact(List<Weighted<E>> p_393977_) {
         this.entries = p_393977_.toArray(Weighted[]::new);
      }

      @Override
      public E get(int p_393502_) {
         for (Weighted<?> weighted : this.entries) {
            p_393502_ -= weighted.weight();
            if (p_393502_ < 0) {
               return (E)weighted.value();
            }
         }

         throw new IllegalStateException(p_393502_ + " exceeded total weight");
      }
   }

   static class Flat<E> implements WeightedList.Selector<E> {
      private final Object[] entries;

      Flat(List<Weighted<E>> p_394297_, int p_394269_) {
         this.entries = new Object[p_394269_];
         int i = 0;

         for (Weighted<E> weighted : p_394297_) {
            int j = weighted.weight();
            Arrays.fill(this.entries, i, i + j, weighted.value());
            i += j;
         }
      }

      @Override
      public E get(int p_394566_) {
         return (E)this.entries[p_394566_];
      }
   }

   interface Selector<E> {
      E get(int var1);
   }
}
