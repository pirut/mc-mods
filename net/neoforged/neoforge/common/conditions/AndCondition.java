package net.neoforged.neoforge.common.conditions;

import com.google.common.base.Joiner;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record AndCondition(List<ICondition> children) implements ICondition {
   public static final MapCodec<AndCondition> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(LIST_CODEC.fieldOf("values").forGetter(AndCondition::children)).apply(builder, AndCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context) {
      for (ICondition child : this.children) {
         if (!child.test(context)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return Joiner.on(" && ").join(this.children);
   }
}
