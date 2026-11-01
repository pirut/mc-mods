package net.neoforged.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;

public final class AlwaysCondition implements ICondition {
   public static final AlwaysCondition INSTANCE = new AlwaysCondition();
   public static MapCodec<AlwaysCondition> CODEC = MapCodec.unit(INSTANCE).stable();

   private AlwaysCondition() {
   }

   @Override
   public boolean test(ICondition.IContext context) {
      return true;
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return "always";
   }
}
