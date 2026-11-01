package net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class ByIdMap {
   private static <T> IntFunction<T> createMap(ToIntFunction<T> p_263047_, T[] p_263043_) {
      if (p_263043_.length == 0) {
         throw new IllegalArgumentException("Empty value list");
      } else {
         Int2ObjectMap<T> $$2 = new Int2ObjectOpenHashMap();

         for (T $$3 : p_263043_) {
            int $$4 = p_263047_.applyAsInt($$3);
            T $$5 = (T)$$2.put($$4, $$3);
            if ($$5 != null) {
               throw new IllegalArgumentException("Duplicate entry on id " + $$4 + ": current=" + $$3 + ", previous=" + $$5);
            }
         }

         return $$2;
      }
   }

   public static <T> IntFunction<T> sparse(ToIntFunction<T> p_262952_, T[] p_263085_, T p_262981_) {
      IntFunction<T> $$3 = createMap(p_262952_, p_263085_);
      return p_262932_ -> Objects.requireNonNullElse($$3.apply(p_262932_), p_262981_);
   }

   private static <T> T[] createSortedArray(ToIntFunction<T> p_262976_, T[] p_263053_) {
      int $$2 = p_263053_.length;
      if ($$2 == 0) {
         throw new IllegalArgumentException("Empty value list");
      } else {
         T[] $$3 = (T[])p_263053_.clone();
         Arrays.fill($$3, null);

         for (T $$4 : p_263053_) {
            int $$5 = p_262976_.applyAsInt($$4);
            if ($$5 < 0 || $$5 >= $$2) {
               throw new IllegalArgumentException("Values are not continous, found index " + $$5 + " for value " + $$4);
            }

            T $$6 = $$3[$$5];
            if ($$6 != null) {
               throw new IllegalArgumentException("Duplicate entry on id " + $$5 + ": current=" + $$4 + ", previous=" + $$6);
            }

            $$3[$$5] = $$4;
         }

         for (int $$7 = 0; $$7 < $$2; $$7++) {
            if ($$3[$$7] == null) {
               throw new IllegalArgumentException("Missing value at index: " + $$7);
            }
         }

         return $$3;
      }
   }

   public static <T> IntFunction<T> continuous(ToIntFunction<T> p_263112_, T[] p_262975_, ByIdMap.OutOfBoundsStrategy p_263075_) {
      T[] $$3 = createSortedArray(p_263112_, p_262975_);
      int $$4 = $$3.length;

      return switch (p_263075_) {
         case ZERO -> {
            T $$5 = $$3[0];
            yield p_262927_ -> p_262927_ >= 0 && p_262927_ < $$4 ? $$3[p_262927_] : $$5;
         }
         case WRAP -> p_262977_ -> $$3[Mth.positiveModulo(p_262977_, $$4)];
         case CLAMP -> p_263013_ -> $$3[Mth.clamp(p_263013_, 0, $$4 - 1)];
      };
   }

   public static enum OutOfBoundsStrategy {
      ZERO,
      WRAP,
      CLAMP;
   }
}
