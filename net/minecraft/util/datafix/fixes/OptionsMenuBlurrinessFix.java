package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsMenuBlurrinessFix extends DataFix {
   public OptionsMenuBlurrinessFix(Schema p_348673_) {
      super(p_348673_, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsMenuBlurrinessFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_348619_ -> p_348619_.update(DSL.remainderFinder(), p_348652_ -> p_348652_.update("menuBackgroundBlurriness", p_404253_ -> {
            int i = this.convertToIntRange(p_404253_.asString("0.5"));
            return p_404253_.createString(String.valueOf(i));
         }))
      );
   }

   private int convertToIntRange(String p_348595_) {
      try {
         return Math.round(Float.parseFloat(p_348595_) * 10.0F);
      } catch (NumberFormatException var3) {
         return 5;
      }
   }
}
