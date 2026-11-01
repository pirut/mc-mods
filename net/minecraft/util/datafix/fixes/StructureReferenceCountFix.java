package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class StructureReferenceCountFix extends DataFix {
   public StructureReferenceCountFix(Schema p_16961_, boolean p_16962_) {
      super(p_16961_, p_16962_);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
      return this.fixTypeEverywhereTyped(
         "Structure Reference Fix", $$0, p_16964_ -> p_16964_.update(DSL.remainderFinder(), StructureReferenceCountFix::setCountToAtLeastOne)
      );
   }

   private static <T> Dynamic<T> setCountToAtLeastOne(Dynamic<T> p_16966_) {
      return p_16966_.update(
         "references", p_337667_ -> p_337667_.createInt(p_337667_.asNumber().map(Number::intValue).result().filter(p_145724_ -> p_145724_ > 0).orElse(1))
      );
   }
}
