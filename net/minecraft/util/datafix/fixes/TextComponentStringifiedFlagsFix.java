package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class TextComponentStringifiedFlagsFix extends DataFix {
   public TextComponentStringifiedFlagsFix(Schema p_393716_) {
      super(p_393716_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>> type = this.getInputSchema().getType(References.TEXT_COMPONENT);
      return this.fixTypeEverywhere(
         "TextComponentStringyFlagsFix",
         type,
         p_393745_ -> p_394168_ -> p_394168_.mapSecond(
            p_393692_ -> p_393692_.mapRight(
               p_393938_ -> p_393938_.mapSecond(
                  p_393525_ -> p_393525_.mapSecond(
                     p_394003_ -> p_394003_.mapSecond(
                        p_393936_ -> p_393936_.update("bold", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("italic", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("underlined", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("strikethrough", TextComponentStringifiedFlagsFix::stringToBool)
                           .update("obfuscated", TextComponentStringifiedFlagsFix::stringToBool)
                     )
                  )
               )
            )
         )
      );
   }

   private static <T> Dynamic<T> stringToBool(Dynamic<T> p_393888_) {
      Optional<String> optional = p_393888_.asString().result();
      return optional.isPresent() ? p_393888_.createBoolean(Boolean.parseBoolean(optional.get())) : p_393888_;
   }
}
