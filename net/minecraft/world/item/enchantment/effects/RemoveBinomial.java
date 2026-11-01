package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record RemoveBinomial(LevelBasedValue chance) implements EnchantmentValueEffect {
   public static final MapCodec<RemoveBinomial> CODEC = RecordCodecBuilder.mapCodec(
      p_344917_ -> p_344917_.group(LevelBasedValue.CODEC.fieldOf("chance").forGetter(RemoveBinomial::chance)).apply(p_344917_, RemoveBinomial::new)
   );

   @Override
   public float process(int p_345642_, RandomSource p_345903_, float p_345714_) {
      float $$3 = this.chance.calculate(p_345642_);
      int $$4 = 0;

      for (int $$5 = 0; $$5 < p_345714_; $$5++) {
         if (p_345903_.nextFloat() < $$3) {
            $$4++;
         }
      }

      return p_345714_ - $$4;
   }

   @Override
   public MapCodec<RemoveBinomial> codec() {
      return CODEC;
   }
}
