package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record DistancePredicate(
   MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute
) {
   public static final Codec<DistancePredicate> CODEC = RecordCodecBuilder.create(
      p_337353_ -> p_337353_.group(
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::x),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::y),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::z),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::horizontal),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("absolute", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::absolute)
         )
         .apply(p_337353_, DistancePredicate::new)
   );

   public static DistancePredicate horizontal(MinMaxBounds.Doubles p_148837_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_148837_, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate vertical(MinMaxBounds.Doubles p_148839_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, p_148839_, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
   }

   public static DistancePredicate absolute(MinMaxBounds.Doubles p_148841_) {
      return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, p_148841_);
   }

   public boolean matches(double p_26256_, double p_26257_, double p_26258_, double p_26259_, double p_26260_, double p_26261_) {
      float $$6 = (float)(p_26256_ - p_26259_);
      float $$7 = (float)(p_26257_ - p_26260_);
      float $$8 = (float)(p_26258_ - p_26261_);
      if (!this.x.matches(Mth.abs($$6)) || !this.y.matches(Mth.abs($$7)) || !this.z.matches(Mth.abs($$8))) {
         return false;
      } else {
         return !this.horizontal.matchesSqr($$6 * $$6 + $$8 * $$8) ? false : this.absolute.matchesSqr($$6 * $$6 + $$7 * $$7 + $$8 * $$8);
      }
   }
}
