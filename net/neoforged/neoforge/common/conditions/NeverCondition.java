package net.neoforged.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;

public final class NeverCondition implements ICondition {
   public static final NeverCondition INSTANCE = new NeverCondition();
   public static final MapCodec<NeverCondition> CODEC = MapCodec.unit(INSTANCE).stable();

   private NeverCondition() {
   }

   @Override
   public boolean test(ICondition.IContext condition) {
      return false;
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return "never";
   }
}
