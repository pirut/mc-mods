package net.minecraft.util;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.function.Function;

public interface BoundedFloatFunction<C> {
   BoundedFloatFunction<Float> IDENTITY = createUnlimited(p_447155_ -> p_447155_);

   float apply(C var1);

   float minValue();

   float maxValue();

   static BoundedFloatFunction<Float> createUnlimited(final Float2FloatFunction p_446453_) {
      return new BoundedFloatFunction<Float>() {
         public float apply(Float p_446486_) {
            return (Float)p_446453_.apply(p_446486_);
         }

         @Override
         public float minValue() {
            return Float.NEGATIVE_INFINITY;
         }

         @Override
         public float maxValue() {
            return Float.POSITIVE_INFINITY;
         }
      };
   }

   default <C2> BoundedFloatFunction<C2> comap(final Function<C2, C> p_445870_) {
      final BoundedFloatFunction<C> boundedfloatfunction = this;
      return new BoundedFloatFunction<C2>() {
         @Override
         public float apply(C2 p_446357_) {
            return boundedfloatfunction.apply(p_445870_.apply(p_446357_));
         }

         @Override
         public float minValue() {
            return boundedfloatfunction.minValue();
         }

         @Override
         public float maxValue() {
            return boundedfloatfunction.maxValue();
         }
      };
   }
}
