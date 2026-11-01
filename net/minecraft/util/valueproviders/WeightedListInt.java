package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;

public class WeightedListInt extends IntProvider {
   public static final MapCodec<WeightedListInt> CODEC = RecordCodecBuilder.mapCodec(
      p_185920_ -> p_185920_.group(
            SimpleWeightedRandomList.wrappedCodec(IntProvider.CODEC).fieldOf("distribution").forGetter(p_185918_ -> p_185918_.distribution)
         )
         .apply(p_185920_, WeightedListInt::new)
   );
   private final SimpleWeightedRandomList<IntProvider> distribution;
   private final int minValue;
   private final int maxValue;

   public WeightedListInt(SimpleWeightedRandomList<IntProvider> p_185915_) {
      this.distribution = p_185915_;
      List<WeightedEntry.Wrapper<IntProvider>> $$1 = p_185915_.unwrap();
      int $$2 = Integer.MAX_VALUE;
      int $$3 = Integer.MIN_VALUE;

      for (WeightedEntry.Wrapper<IntProvider> $$4 : $$1) {
         int $$5 = $$4.data().getMinValue();
         int $$6 = $$4.data().getMaxValue();
         $$2 = Math.min($$2, $$5);
         $$3 = Math.max($$3, $$6);
      }

      this.minValue = $$2;
      this.maxValue = $$3;
   }

   @Override
   public int sample(RandomSource p_216870_) {
      return this.distribution.getRandomValue(p_216870_).orElseThrow(IllegalStateException::new).sample(p_216870_);
   }

   @Override
   public int getMinValue() {
      return this.minValue;
   }

   @Override
   public int getMaxValue() {
      return this.maxValue;
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.WEIGHTED_LIST;
   }
}
