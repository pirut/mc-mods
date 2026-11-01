package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;

public class EntityFallDistanceFloatToDoubleFix extends DataFix {
   private final TypeReference type;

   public EntityFallDistanceFloatToDoubleFix(Schema p_397426_, TypeReference p_397958_) {
      super(p_397426_, false);
      this.type = p_397958_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "EntityFallDistanceFloatToDoubleFixFor" + this.type.typeName(),
         this.getOutputSchema().getType(this.type),
         EntityFallDistanceFloatToDoubleFix::fixEntity
      );
   }

   private static Typed<?> fixEntity(Typed<?> p_397231_) {
      return p_397231_.update(
         DSL.remainderFinder(),
         p_397901_ -> p_397901_.renameAndFixField("FallDistance", "fall_distance", p_398000_ -> p_398000_.createDouble(p_398000_.asFloat(0.0F)))
      );
   }
}
