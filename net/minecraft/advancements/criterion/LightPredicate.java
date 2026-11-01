package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record LightPredicate(MinMaxBounds.Ints composite) {
   public static final Codec<LightPredicate> CODEC = RecordCodecBuilder.create(
      p_469958_ -> p_469958_.group(MinMaxBounds.Ints.CODEC.optionalFieldOf("light", MinMaxBounds.Ints.ANY).forGetter(LightPredicate::composite))
         .apply(p_469958_, LightPredicate::new)
   );

   public boolean matches(ServerLevel p_467690_, BlockPos p_468756_) {
      return !p_467690_.isLoaded(p_468756_) ? false : this.composite.matches(p_467690_.getMaxLocalRawBrightness(p_468756_));
   }

   public static class Builder {
      private MinMaxBounds.Ints composite = MinMaxBounds.Ints.ANY;

      public static LightPredicate.Builder light() {
         return new LightPredicate.Builder();
      }

      public LightPredicate.Builder setComposite(MinMaxBounds.Ints p_468496_) {
         this.composite = p_468496_;
         return this;
      }

      public LightPredicate build() {
         return new LightPredicate(this.composite);
      }
   }
}
