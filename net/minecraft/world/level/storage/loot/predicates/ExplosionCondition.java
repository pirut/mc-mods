package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ExplosionCondition implements LootItemCondition {
   private static final ExplosionCondition INSTANCE = new ExplosionCondition();
   public static final MapCodec<ExplosionCondition> CODEC = MapCodec.unit(INSTANCE);

   private ExplosionCondition() {
   }

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.SURVIVES_EXPLOSION;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.EXPLOSION_RADIUS);
   }

   public boolean test(LootContext p_81659_) {
      Float $$1 = p_81659_.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
      if ($$1 != null) {
         RandomSource $$2 = p_81659_.getRandom();
         float $$3 = 1.0F / $$1;
         return $$2.nextFloat() <= $$3;
      } else {
         return true;
      }
   }

   public static LootItemCondition.Builder survivesExplosion() {
      return () -> INSTANCE;
   }
}
