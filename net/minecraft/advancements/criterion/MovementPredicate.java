package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record MovementPredicate(
   MinMaxBounds.Doubles x,
   MinMaxBounds.Doubles y,
   MinMaxBounds.Doubles z,
   MinMaxBounds.Doubles speed,
   MinMaxBounds.Doubles horizontalSpeed,
   MinMaxBounds.Doubles verticalSpeed,
   MinMaxBounds.Doubles fallDistance
) {
   public static final Codec<MovementPredicate> CODEC = RecordCodecBuilder.create(
      p_469739_ -> p_469739_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::x),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::y),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::z),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::speed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal_speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::horizontalSpeed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("vertical_speed", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::verticalSpeed),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("fall_distance", MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::fallDistance)
         )
         .apply(p_469739_, MovementPredicate::new)
   );

   public static MovementPredicate speed(MinMaxBounds.Doubles p_468540_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_468540_,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate horizontalSpeed(MinMaxBounds.Doubles p_467182_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_467182_,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate verticalSpeed(MinMaxBounds.Doubles p_467202_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_467202_,
         MinMaxBounds.Doubles.ANY
      );
   }

   public static MovementPredicate fallDistance(MinMaxBounds.Doubles p_466901_) {
      return new MovementPredicate(
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         MinMaxBounds.Doubles.ANY,
         p_466901_
      );
   }

   public boolean matches(double p_469107_, double p_466996_, double p_467708_, double p_467244_) {
      if (this.x.matches(p_469107_) && this.y.matches(p_466996_) && this.z.matches(p_467708_)) {
         double d0 = Mth.lengthSquared(p_469107_, p_466996_, p_467708_);
         if (!this.speed.matchesSqr(d0)) {
            return false;
         } else {
            double d1 = Mth.lengthSquared(p_469107_, p_467708_);
            if (!this.horizontalSpeed.matchesSqr(d1)) {
               return false;
            } else {
               double d2 = Math.abs(p_466996_);
               return !this.verticalSpeed.matches(d2) ? false : this.fallDistance.matches(p_467244_);
            }
         }
      } else {
         return false;
      }
   }
}
