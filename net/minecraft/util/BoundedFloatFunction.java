package net.minecraft.util;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.function.Function;

public interface BoundedFloatFunction<C> {
   BoundedFloatFunction<Float> IDENTITY = createUnlimited(f -> f);

   float apply(C input);

   float minValue();

   float maxValue();

   static BoundedFloatFunction<Float> createUnlimited(Float2FloatFunction function) {
      return new BoundedFloatFunction<Float>() {
         public float apply(Float float_) {
            return (Float)function.apply(float_);
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

   default <C2> BoundedFloatFunction<C2> comap(Function<C2, C> mapper) {
      final BoundedFloatFunction<C> boundedFloatFunction = this;
      return new BoundedFloatFunction<C2>() {
         @Override
         public float apply(C2 input) {
            return boundedFloatFunction.apply(mapper.apply(input));
         }

         @Override
         public float minValue() {
            return boundedFloatFunction.minValue();
         }

         @Override
         public float maxValue() {
            return boundedFloatFunction.maxValue();
         }
      };
   }
}
