package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsGraphicsModeSplitFix extends DataFix {
   private final String newFieldName;
   private final String valueIfFast;
   private final String valueIfFancy;
   private final String valueIfFabulous;

   public OptionsGraphicsModeSplitFix(Schema p_455195_, String p_455962_, String p_455718_, String p_455670_, String p_455536_) {
      super(p_455195_, true);
      this.newFieldName = p_455962_;
      this.valueIfFast = p_455718_;
      this.valueIfFancy = p_455670_;
      this.valueIfFabulous = p_455536_;
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "graphicsMode split to " + this.newFieldName,
         this.getInputSchema().getType(References.OPTIONS),
         p_454935_ -> p_454935_.update(
            DSL.remainderFinder(),
            p_454718_ -> (Dynamic)DataFixUtils.orElseGet(
               p_454718_.get("graphicsMode")
                  .asString()
                  .map(p_454671_ -> p_454718_.set(this.newFieldName, p_454718_.createString(this.getValue(p_454671_))))
                  .result(),
               () -> p_454718_.set(this.newFieldName, p_454718_.createString(this.valueIfFancy))
            )
         )
      );
   }

   private String getValue(String p_454969_) {
      return switch (p_454969_) {
         case "2" -> this.valueIfFabulous;
         case "0" -> this.valueIfFast;
         default -> this.valueIfFancy;
      };
   }
}
