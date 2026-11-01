package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class WeightedRandom {
   private WeightedRandom() {
   }

   public static <T> int getTotalWeight(List<T> p_146313_, ToIntFunction<T> p_393530_) {
      long i = 0L;

      for (T t : p_146313_) {
         i += p_393530_.applyAsInt(t);
      }

      if (i > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)i;
      }
   }

   public static <T> Optional<T> getRandomItem(RandomSource p_216826_, List<T> p_216827_, int p_216828_, ToIntFunction<T> p_394361_) {
      if (p_216828_ < 0) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
      } else if (p_216828_ == 0) {
         return Optional.empty();
      } else {
         int i = p_216826_.nextInt(p_216828_);
         return getWeightedItem(p_216827_, i, p_394361_);
      }
   }

   public static <T> Optional<T> getWeightedItem(List<T> p_146315_, int p_146316_, ToIntFunction<T> p_393515_) {
      for (T t : p_146315_) {
         p_146316_ -= p_393515_.applyAsInt(t);
         if (p_146316_ < 0) {
            return Optional.of(t);
         }
      }

      return Optional.empty();
   }

   public static <T> Optional<T> getRandomItem(RandomSource p_216823_, List<T> p_216824_, ToIntFunction<T> p_393718_) {
      return getRandomItem(p_216823_, p_216824_, getTotalWeight(p_216824_, p_393718_), p_393718_);
   }
}
