package net.neoforged.neoforge.common.conditions;

import com.google.common.base.Joiner;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record OrCondition(List<ICondition> values) implements ICondition {
   public static final MapCodec<OrCondition> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(LIST_CODEC.fieldOf("values").forGetter(OrCondition::values)).apply(builder, OrCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context) {
      for (ICondition child : this.values()) {
         if (child.test(context)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return Joiner.on(" || ").join(this.values());
   }
}
