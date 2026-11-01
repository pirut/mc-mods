package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay extends LootItemConditionalFunction {
   public static final MapCodec<ApplyExplosionDecay> CODEC = RecordCodecBuilder.mapCodec(
      p_298525_ -> commonFields(p_298525_).apply(p_298525_, ApplyExplosionDecay::new)
   );

   private ApplyExplosionDecay(List<LootItemCondition> p_298289_) {
      super(p_298289_);
   }

   @Override
   public LootItemFunctionType<ApplyExplosionDecay> getType() {
      return LootItemFunctions.EXPLOSION_DECAY;
   }

   @Override
   public ItemStack run(ItemStack p_80034_, LootContext p_80035_) {
      Float $$2 = p_80035_.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
      if ($$2 != null) {
         RandomSource $$3 = p_80035_.getRandom();
         float $$4 = 1.0F / $$2;
         int $$5 = p_80034_.getCount();
         int $$6 = 0;

         for (int $$7 = 0; $$7 < $$5; $$7++) {
            if ($$3.nextFloat() <= $$4) {
               $$6++;
            }
         }

         p_80034_.setCount($$6);
      }

      return p_80034_;
   }

   public static LootItemConditionalFunction.Builder<?> explosionDecay() {
      return simpleBuilder(ApplyExplosionDecay::new);
   }
}
