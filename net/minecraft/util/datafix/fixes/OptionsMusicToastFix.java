package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class OptionsMusicToastFix extends DataFix {
   public OptionsMusicToastFix(Schema p_470700_, boolean p_470577_) {
      super(p_470700_, p_470577_);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsMusicToastFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_470734_ -> p_470734_.update(
            DSL.remainderFinder(),
            p_470597_ -> p_470597_.renameAndFixField(
               "showNowPlayingToast",
               "musicToast",
               p_470616_ -> p_470597_.createString(p_470616_.asString("false").equals("false") ? "never" : "pause_and_toast")
            )
         )
      );
   }
}
