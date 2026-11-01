package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public interface AttributeRange<Value> {
   AttributeRange<Float> UNIT_FLOAT = ofFloat(0.0F, 1.0F);
   AttributeRange<Float> NON_NEGATIVE_FLOAT = ofFloat(0.0F, Float.POSITIVE_INFINITY);

   static <Value> AttributeRange<Value> any() {
      return new AttributeRange<Value>() {
         @Override
         public DataResult<Value> validate(Value value) {
            return DataResult.success(value);
         }

         @Override
         public Value sanitize(Value value) {
            return value;
         }
      };
   }

   static AttributeRange<Float> ofFloat(float min, float max) {
      return new AttributeRange<Float>() {
         public DataResult<Float> validate(Float float_) {
            return float_ >= min && float_ <= max ? DataResult.success(float_) : DataResult.error(() -> float_ + " is not in range [" + min + "; " + max + "]");
         }

         public Float sanitize(Float float_) {
            return float_ >= min && float_ <= max ? float_ : Mth.clamp(float_, min, max);
         }
      };
   }

   DataResult<Value> validate(Value value);

   Value sanitize(Value value);
}
