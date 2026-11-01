package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public interface EasingType {
   ExtraCodecs.LateBoundIdMapper<String, EasingType> SIMPLE_REGISTRY = new ExtraCodecs.LateBoundIdMapper<>();
   Codec<EasingType> CODEC = Codec.either(SIMPLE_REGISTRY.codec(Codec.STRING), EasingType.CubicBezier.CODEC)
      .xmap(
         Either::unwrap,
         p_467588_ -> p_467588_ instanceof EasingType.CubicBezier easingtype$cubicbezier ? Either.right(easingtype$cubicbezier) : Either.left(p_467588_)
      );
   EasingType CONSTANT = registerSimple("constant", p_468850_ -> 0.0F);
   EasingType LINEAR = registerSimple("linear", p_469267_ -> p_469267_);
   EasingType IN_BACK = registerSimple("in_back", Ease::inBack);
   EasingType IN_BOUNCE = registerSimple("in_bounce", Ease::inBounce);
   EasingType IN_CIRC = registerSimple("in_circ", Ease::inCirc);
   EasingType IN_CUBIC = registerSimple("in_cubic", Ease::inCubic);
   EasingType IN_ELASTIC = registerSimple("in_elastic", Ease::inElastic);
   EasingType IN_EXPO = registerSimple("in_expo", Ease::inExpo);
   EasingType IN_QUAD = registerSimple("in_quad", Ease::inQuad);
   EasingType IN_QUART = registerSimple("in_quart", Ease::inQuart);
   EasingType IN_QUINT = registerSimple("in_quint", Ease::inQuint);
   EasingType IN_SINE = registerSimple("in_sine", Ease::inSine);
   EasingType IN_OUT_BACK = registerSimple("in_out_back", Ease::inOutBack);
   EasingType IN_OUT_BOUNCE = registerSimple("in_out_bounce", Ease::inOutBounce);
   EasingType IN_OUT_CIRC = registerSimple("in_out_circ", Ease::inOutCirc);
   EasingType IN_OUT_CUBIC = registerSimple("in_out_cubic", Ease::inOutCubic);
   EasingType IN_OUT_ELASTIC = registerSimple("in_out_elastic", Ease::inOutElastic);
   EasingType IN_OUT_EXPO = registerSimple("in_out_expo", Ease::inOutExpo);
   EasingType IN_OUT_QUAD = registerSimple("in_out_quad", Ease::inOutQuad);
   EasingType IN_OUT_QUART = registerSimple("in_out_quart", Ease::inOutQuart);
   EasingType IN_OUT_QUINT = registerSimple("in_out_quint", Ease::inOutQuint);
   EasingType IN_OUT_SINE = registerSimple("in_out_sine", Ease::inOutSine);
   EasingType OUT_BACK = registerSimple("out_back", Ease::outBack);
   EasingType OUT_BOUNCE = registerSimple("out_bounce", Ease::outBounce);
   EasingType OUT_CIRC = registerSimple("out_circ", Ease::outCirc);
   EasingType OUT_CUBIC = registerSimple("out_cubic", Ease::outCubic);
   EasingType OUT_ELASTIC = registerSimple("out_elastic", Ease::outElastic);
   EasingType OUT_EXPO = registerSimple("out_expo", Ease::outExpo);
   EasingType OUT_QUAD = registerSimple("out_quad", Ease::outQuad);
   EasingType OUT_QUART = registerSimple("out_quart", Ease::outQuart);
   EasingType OUT_QUINT = registerSimple("out_quint", Ease::outQuint);
   EasingType OUT_SINE = registerSimple("out_sine", Ease::outSine);

   static EasingType registerSimple(String p_469823_, EasingType p_469350_) {
      SIMPLE_REGISTRY.put(p_469823_, p_469350_);
      return p_469350_;
   }

   static EasingType cubicBezier(float p_469912_, float p_469058_, float p_469447_, float p_466849_) {
      return new EasingType.CubicBezier(new EasingType.CubicBezierControls(p_469912_, p_469058_, p_469447_, p_466849_));
   }

   static EasingType symmetricCubicBezier(float p_468004_, float p_467903_) {
      return cubicBezier(p_468004_, p_467903_, 1.0F - p_468004_, 1.0F - p_467903_);
   }

   float apply(float var1);

   public static final class CubicBezier implements EasingType {
      public static final Codec<EasingType.CubicBezier> CODEC = RecordCodecBuilder.create(
         p_468836_ -> p_468836_.group(EasingType.CubicBezierControls.CODEC.fieldOf("cubic_bezier").forGetter(p_468574_ -> p_468574_.controls))
            .apply(p_468836_, EasingType.CubicBezier::new)
      );
      private static final int NEWTON_RAPHSON_ITERATIONS = 4;
      private final EasingType.CubicBezierControls controls;
      private final EasingType.CubicBezier.CubicCurve xCurve;
      private final EasingType.CubicBezier.CubicCurve yCurve;

      public CubicBezier(EasingType.CubicBezierControls p_468211_) {
         this.controls = p_468211_;
         this.xCurve = curveFromControls(p_468211_.x1, p_468211_.x2);
         this.yCurve = curveFromControls(p_468211_.y1, p_468211_.y2);
      }

      private static EasingType.CubicBezier.CubicCurve curveFromControls(float p_469670_, float p_468617_) {
         return new EasingType.CubicBezier.CubicCurve(3.0F * p_469670_ - 3.0F * p_468617_ + 1.0F, -6.0F * p_469670_ + 3.0F * p_468617_, 3.0F * p_469670_);
      }

      @Override
      public float apply(float p_467584_) {
         float f = p_467584_;

         for (int i = 0; i < 4; i++) {
            float f1 = this.xCurve.sampleGradient(f);
            if (f1 < 1.0E-5F) {
               break;
            }

            float f2 = this.xCurve.sample(f) - p_467584_;
            f -= f2 / f1;
         }

         return this.yCurve.sample(f);
      }

      @Override
      public boolean equals(Object p_469511_) {
         return p_469511_ instanceof EasingType.CubicBezier easingtype$cubicbezier && this.controls.equals(easingtype$cubicbezier.controls);
      }

      @Override
      public int hashCode() {
         return this.controls.hashCode();
      }

      @Override
      public String toString() {
         return "CubicBezier(" + this.controls.x1 + ", " + this.controls.y1 + ", " + this.controls.x2 + ", " + this.controls.y2 + ")";
      }

      record CubicCurve(float a, float b, float c) {
         public float sample(float p_466890_) {
            return ((this.a * p_466890_ + this.b) * p_466890_ + this.c) * p_466890_;
         }

         public float sampleGradient(float p_468041_) {
            return (3.0F * this.a * p_468041_ + 2.0F * this.b) * p_468041_ + this.c;
         }
      }
   }

   public record CubicBezierControls(float x1, float y1, float x2, float y2) {
      public static final Codec<EasingType.CubicBezierControls> CODEC = Codec.FLOAT
         .listOf(4, 4)
         .xmap(
            p_467017_ -> new EasingType.CubicBezierControls((Float)p_467017_.get(0), (Float)p_467017_.get(1), (Float)p_467017_.get(2), (Float)p_467017_.get(3)),
            p_468664_ -> List.of(p_468664_.x1, p_468664_.y1, p_468664_.x2, p_468664_.y2)
         )
         .validate(EasingType.CubicBezierControls::validate);

      private DataResult<EasingType.CubicBezierControls> validate() {
         if (!(this.x1 < 0.0F) && !(this.x1 > 1.0F)) {
            return !(this.x2 < 0.0F) && !(this.x2 > 1.0F) ? DataResult.success(this) : DataResult.error(() -> "x2 must be in range [0; 1]");
         } else {
            return DataResult.error(() -> "x1 must be in range [0; 1]");
         }
      }
   }
}
