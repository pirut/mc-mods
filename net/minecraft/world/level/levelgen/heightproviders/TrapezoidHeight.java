package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class TrapezoidHeight extends HeightProvider {
   public static final MapCodec<TrapezoidHeight> CODEC = RecordCodecBuilder.mapCodec(
      p_162005_ -> p_162005_.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(p_162021_ -> p_162021_.minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(p_162019_ -> p_162019_.maxInclusive),
            Codec.INT.optionalFieldOf("plateau", 0).forGetter(p_162014_ -> p_162014_.plateau)
         )
         .apply(p_162005_, TrapezoidHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int plateau;

   private TrapezoidHeight(VerticalAnchor p_162000_, VerticalAnchor p_162001_, int p_162002_) {
      this.minInclusive = p_162000_;
      this.maxInclusive = p_162001_;
      this.plateau = p_162002_;
   }

   public static TrapezoidHeight of(VerticalAnchor p_162010_, VerticalAnchor p_162011_, int p_162012_) {
      return new TrapezoidHeight(p_162010_, p_162011_, p_162012_);
   }

   public static TrapezoidHeight of(VerticalAnchor p_162007_, VerticalAnchor p_162008_) {
      return of(p_162007_, p_162008_, 0);
   }

   @Override
   public int sample(RandomSource p_226305_, WorldGenerationContext p_226306_) {
      int $$2 = this.minInclusive.resolveY(p_226306_);
      int $$3 = this.maxInclusive.resolveY(p_226306_);
      if ($$2 > $$3) {
         LOGGER.warn("Empty height range: {}", this);
         return $$2;
      } else {
         int $$4 = $$3 - $$2;
         if (this.plateau >= $$4) {
            return Mth.randomBetweenInclusive(p_226305_, $$2, $$3);
         } else {
            int $$5 = ($$4 - this.plateau) / 2;
            int $$6 = $$4 - $$5;
            return $$2 + Mth.randomBetweenInclusive(p_226305_, 0, $$6) + Mth.randomBetweenInclusive(p_226305_, 0, $$5);
         }
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.TRAPEZOID;
   }

   @Override
   public String toString() {
      return this.plateau == 0
         ? "triangle (" + this.minInclusive + "-" + this.maxInclusive + ")"
         : "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
