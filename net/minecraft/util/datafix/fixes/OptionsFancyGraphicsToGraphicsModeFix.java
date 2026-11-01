package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsFancyGraphicsToGraphicsModeFix extends DataFix {
   public OptionsFancyGraphicsToGraphicsModeFix(Schema p_455204_) {
      super(p_455204_, true);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "fancyGraphics to graphicsMode",
         this.getInputSchema().getType(References.OPTIONS),
         p_454947_ -> p_454947_.update(
            DSL.remainderFinder(),
            p_455303_ -> p_455303_.renameAndFixField("fancyGraphics", "graphicsMode", OptionsFancyGraphicsToGraphicsModeFix::fixGraphicsMode)
         )
      );
   }

   private static <T> Dynamic<T> fixGraphicsMode(Dynamic<T> p_455264_) {
      return "true".equals(p_455264_.asString("true")) ? p_455264_.createString("1") : p_455264_.createString("0");
   }
}
