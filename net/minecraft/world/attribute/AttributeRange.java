package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public interface AttributeRange<Value> {
   AttributeRange<Float> UNIT_FLOAT = ofFloat(0.0F, 1.0F);
   AttributeRange<Float> NON_NEGATIVE_FLOAT = ofFloat(0.0F, Float.POSITIVE_INFINITY);

   static <Value> AttributeRange<Value> any() {
      return new AttributeRange<Value>() {
         @Override
         public DataResult<Value> validate(Value p_457845_) {
            return DataResult.success(p_457845_);
         }

         @Override
         public Value sanitize(Value p_457594_) {
            return p_457594_;
         }
      };
   }

   static AttributeRange<Float> ofFloat(final float p_457625_, final float p_457789_) {
      return new AttributeRange<Float>() {
         public DataResult<Float> validate(Float p_458024_) {
            return p_458024_ >= p_457625_ && p_458024_ <= p_457789_
               ? DataResult.success(p_458024_)
               : DataResult.error(() -> p_458024_ + " is not in range [" + p_457625_ + "; " + p_457789_ + "]");
         }

         public Float sanitize(Float p_457929_) {
            return p_457929_ >= p_457625_ && p_457929_ <= p_457789_ ? p_457929_ : Mth.clamp(p_457929_, p_457625_, p_457789_);
         }
      };
   }

   DataResult<Value> validate(Value var1);

   Value sanitize(Value var1);
}
