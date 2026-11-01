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
   public LegacyHoverEventFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<? extends Pair<String, ?>> type = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), type);
   }

   private <C, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C> componentType, Type<H> hoverEventType) {
      Type<Pair<String, Either<Either<String, List<C>>, Pair<Either<List<C>, Unit>, Pair<Either<C, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
         References.TEXT_COMPONENT.typeName(),
         DSL.or(
            DSL.or(DSL.string(), DSL.list(componentType)),
            DSL.and(
               DSL.optional(DSL.field("extra", DSL.list(componentType))),
               DSL.optional(DSL.field("separator", componentType)),
               DSL.optional(DSL.field("hoverEvent", hoverEventType)),
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
            dynamicOps -> pair -> pair.mapSecond(either -> either.mapRight(pairx -> pairx.mapSecond(pairxx -> pairxx.mapSecond(pairxxx -> {
               Dynamic<?> dynamic = (Dynamic<?>)pairxxx.getSecond();
               Optional<? extends Dynamic<?>> optional = dynamic.get("hoverEvent").result();
               if (optional.isEmpty()) {
                  return pairxxx;
               } else {
                  Optional<? extends Dynamic<?>> optional2 = optional.get().get("value").result();
                  if (optional2.isEmpty()) {
                     return pairxxx;
                  } else {
                     String string = ((Either)pairxxx.getFirst()).left().<String>map(Pair::getFirst).orElse("");
                     H pair2 = this.fixHoverEvent(hoverEventType, string, (Dynamic<?>)optional.get());
                     return pairxxx.mapFirst(eitherx -> Either.left(pair2));
                  }
               }
            }))))
         );
      }
   }

   private <H> H fixHoverEvent(Type<H> type, String action, Dynamic<?> data) {
      return "show_text".equals(action) ? fixShowTextHover(type, data) : createPlaceholderHover(type, data);
   }

   private static <H> H fixShowTextHover(Type<H> type, Dynamic<?> data) {
      Dynamic<?> dynamic = data.renameField("value", "contents");
      return (H)Util.readTypedOrThrow(type, dynamic).getValue();
   }

   private static <H> H createPlaceholderHover(Type<H> type, Dynamic<?> data) {
      JsonElement jsonElement = (JsonElement)data.convert(JsonOps.INSTANCE).getValue();
      Dynamic<?> dynamic = new Dynamic(
         JavaOps.INSTANCE,
         Map.of("action", "show_text", "contents", Map.<String, String>of("text", "Legacy hoverEvent: " + GsonHelper.toStableString(jsonElement)))
      );
      return (H)Util.readTypedOrThrow(type, dynamic).getValue();
   }
}
