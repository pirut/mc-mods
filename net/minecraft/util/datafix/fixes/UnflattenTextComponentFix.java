package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class UnflattenTextComponentFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public UnflattenTextComponentFix(Schema p_394642_) {
      super(p_394642_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, String>> type = this.getInputSchema().getType(References.TEXT_COMPONENT);
      Type<?> type1 = this.getOutputSchema().getType(References.TEXT_COMPONENT);
      return this.createFixer(type, type1);
   }

   private <T> TypeRewriteRule createFixer(Type<Pair<String, String>> p_393909_, Type<T> p_394508_) {
      return this.fixTypeEverywhere(
         "UnflattenTextComponentFix",
         p_393909_,
         p_394508_,
         p_393997_ -> p_466490_ -> Util.readTypedOrThrow(p_394508_, unflattenJson(p_393997_, (String)p_466490_.getSecond()), true).getValue()
      );
   }

   private static <T> Dynamic<T> unflattenJson(DynamicOps<T> p_394198_, String p_394650_) {
      try {
         JsonElement jsonelement = LenientJsonParser.parse(p_394650_);
         if (!jsonelement.isJsonNull()) {
            return new Dynamic(p_394198_, JsonOps.INSTANCE.convertTo(p_394198_, jsonelement));
         }
      } catch (Exception var3) {
         LOGGER.error("Failed to unflatten text component json: {}", p_394650_, var3);
      }

      return new Dynamic(p_394198_, p_394198_.createString(p_394650_));
   }
}
