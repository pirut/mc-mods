package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsAddTextBackgroundFix extends DataFix {
   public OptionsAddTextBackgroundFix(Schema p_16607_, boolean p_16608_) {
      super(p_16607_, p_16608_);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsAddTextBackgroundFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_16610_ -> p_16610_.update(
            DSL.remainderFinder(), p_337652_ -> (Dynamic)DataFixUtils.orElse(p_337652_.get("chatOpacity").asString().map(p_404252_ -> {
               double d0 = this.calculateBackground(p_404252_);
               return p_337652_.set("textBackgroundOpacity", p_337652_.createString(String.valueOf(d0)));
            }).result(), p_337652_)
         )
      );
   }

   private double calculateBackground(String p_16617_) {
      try {
         double d0 = 0.9 * Double.parseDouble(p_16617_) + 0.1;
         return d0 / 2.0;
      } catch (NumberFormatException var4) {
         return 0.5;
      }
   }
}
