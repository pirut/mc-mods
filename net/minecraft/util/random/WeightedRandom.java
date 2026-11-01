package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public class WeightedRandom {
   private WeightedRandom() {
   }

   public static int getTotalWeight(List<? extends WeightedEntry> p_146313_) {
      long $$1 = 0L;

      for (WeightedEntry $$2 : p_146313_) {
         $$1 += $$2.getWeight().asInt();
      }

      if ($$1 > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)$$1;
      }
   }

   public static <T extends WeightedEntry> Optional<T> getRandomItem(RandomSource p_216826_, List<T> p_216827_, int p_216828_) {
      if (p_216828_ < 0) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
      } else if (p_216828_ == 0) {
         return Optional.empty();
      } else {
         int $$3 = p_216826_.nextInt(p_216828_);
         return getWeightedItem(p_216827_, $$3);
      }
   }

   public static <T extends WeightedEntry> Optional<T> getWeightedItem(List<T> p_146315_, int p_146316_) {
      for (T $$2 : p_146315_) {
         p_146316_ -= $$2.getWeight().asInt();
         if (p_146316_ < 0) {
            return Optional.of($$2);
         }
      }

      return Optional.empty();
   }

   public static <T extends WeightedEntry> Optional<T> getRandomItem(RandomSource p_216823_, List<T> p_216824_) {
      return getRandomItem(p_216823_, p_216824_, getTotalWeight(p_216824_));
   }
}
