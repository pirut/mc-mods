package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;

public class LegacyHoverEventFix extends DataFix {
   public LegacyHoverEventFix(Schema p_394437_) {
      super(p_394437_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<? extends Pair<String, ?>> type = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), type);
   }

   private <C, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C> p_393921_, Type<H> p_393491_) {
      Type<Pair<String, Either<Either<String, List<C>>, Pair<Either<List<C>, Unit>, Pair<Either<C, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
         References.TEXT_COMPONENT.typeName(),
         DSL.or(
            DSL.or(DSL.string(), DSL.list(p_393921_)),
            DSL.and(
               DSL.optional(DSL.field("extra", DSL.list(p_393921_))),
               DSL.optional(DSL.field("separator", p_393921_)),
               DSL.optional(DSL.field("hoverEvent", p_393491_)),
               DSL.remainderType()
            )
         )
      );
      if (!type.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
         throw new IllegalStateException(
            "Text component type did not match, expected " + type + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
         );
      } else {
         return this.fixTypeEverywhere(
            "LegacyHoverEventFix",
            type,
            p_394584_ -> p_394164_ -> p_394164_.mapSecond(
               p_393535_ -> p_393535_.mapRight(p_394347_ -> p_394347_.mapSecond(p_393557_ -> p_393557_.mapSecond(p_394017_ -> {
                  Dynamic<?> dynamic = (Dynamic<?>)p_394017_.getSecond();
                  Optional<? extends Dynamic<?>> optional = dynamic.get("hoverEvent").result();
                  if (optional.isEmpty()) {
                     return p_394017_;
                  } else {
                     Optional<? extends Dynamic<?>> optional1 = optional.get().get("value").result();
                     if (optional1.isEmpty()) {
                        return p_394017_;
                     } else {
                        String s = ((Either)p_394017_.getFirst()).left().<String>map(Pair::getFirst).orElse("");
                        H h = this.fixHoverEvent(p_393491_, s, (Dynamic<?>)optional.get());
                        return p_394017_.mapFirst(p_393590_ -> Either.left(h));
                     }
                  }
               })))
            )
         );
      }
   }

   private <H> H fixHoverEvent(Type<H> p_394028_, String p_394084_, Dynamic<?> p_394517_) {
      return "show_text".equals(p_394084_) ? fixShowTextHover(p_394028_, p_394517_) : createPlaceholderHover(p_394028_, p_394517_);
   }

   private static <H> H fixShowTextHover(Type<H> p_393521_, Dynamic<?> p_394195_) {
      Dynamic<?> dynamic = p_394195_.renameField("value", "contents");
      return (H)Util.readTypedOrThrow(p_393521_, dynamic).getValue();
   }

   private static <H> H createPlaceholderHover(Type<H> p_393539_, Dynamic<?> p_394275_) {
      JsonElement jsonelement = (JsonElement)p_394275_.convert(JsonOps.INSTANCE).getValue();
      Dynamic<?> dynamic = new Dynamic(
         JavaOps.INSTANCE,
         Map.of("action", "show_text", "contents", Map.<String, String>of("text", "Legacy hoverEvent: " + GsonHelper.toStableString(jsonelement)))
      );
      return (H)Util.readTypedOrThrow(p_393539_, dynamic).getValue();
   }
}
