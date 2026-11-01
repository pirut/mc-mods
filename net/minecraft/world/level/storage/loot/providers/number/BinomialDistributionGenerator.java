package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider {
   public static final MapCodec<BinomialDistributionGenerator> CODEC = RecordCodecBuilder.mapCodec(
      p_299136_ -> p_299136_.group(
            NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n),
            NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)
         )
         .apply(p_299136_, BinomialDistributionGenerator::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.BINOMIAL;
   }

   @Override
   public int getInt(LootContext p_165663_) {
      int $$1 = this.n.getInt(p_165663_);
      float $$2 = this.p.getFloat(p_165663_);
      RandomSource $$3 = p_165663_.getRandom();
      int $$4 = 0;

      for (int $$5 = 0; $$5 < $$1; $$5++) {
         if ($$3.nextFloat() < $$2) {
            $$4++;
         }
      }

      return $$4;
   }

   @Override
   public float getFloat(LootContext p_165666_) {
      return this.getInt(p_165666_);
   }

   public static BinomialDistributionGenerator binomial(int p_165660_, float p_165661_) {
      return new BinomialDistributionGenerator(ConstantValue.exactly(p_165660_), ConstantValue.exactly(p_165661_));
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
   }
}
