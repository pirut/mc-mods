package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record DistancePredicate(
   MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute
) {
   public static final Codec<DistancePredicate> CODEC = RecordCodecBuilder.create(
      p_469359_ -> p_469359_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::x),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::y),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::z),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::horizontal),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("absolute", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::absolute)
         )
         .apply(p_469359_, DistancePredicate::new)
   );

   public static DistancePredicate horizontal(MinMaxBounds.Doubles p_467004_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_467004_, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate vertical(MinMaxBounds.Doubles p_468630_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, p_468630_, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate absolute(MinMaxBounds.Doubles p_469040_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_469040_);
   }

   public boolean matches(double p_468052_, double p_466956_, double p_469904_, double p_467259_, double p_468182_, double p_468995_) {
      float f = (float)(p_468052_ - p_467259_);
      float f1 = (float)(p_466956_ - p_468182_);
      float f2 = (float)(p_469904_ - p_468995_);
      if (this.x.matches(Mth.abs(f)) && this.y.matches(Mth.abs(f1)) && this.z.matches(Mth.abs(f2))) {
         return !this.horizontal.matchesSqr(f * f + f2 * f2) ? false : this.absolute.matchesSqr(f * f + f1 * f1 + f2 * f2);
      } else {
         return false;
      }
   }
}
