package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class NamedEntityConvertUncheckedFix extends NamedEntityFix {
   public NamedEntityConvertUncheckedFix(Schema p_399680_, String p_399528_, TypeReference p_399814_, String p_399840_) {
      super(p_399680_, true, p_399528_, p_399814_, p_399840_);
   }

   @Override
   protected Typed<?> fix(Typed<?> p_399900_) {
      Type<?> type = this.getOutputSchema().getChoiceType(this.type, this.entityName);
      return ExtraDataFixUtils.cast(type, p_399900_);
   }
}
