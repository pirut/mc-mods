package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import org.jspecify.annotations.Nullable;

public class TextComponentHoverAndClickEventFix extends DataFix {
   public TextComponentHoverAndClickEventFix(Schema p_393653_) {
      super(p_393653_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<? extends Pair<String, ?>> type = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
      return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), type);
   }

   private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C1> p_394638_, Type<C2> p_393676_, Type<H> p_393897_) {
      Type<Pair<String, Either<Either<String, List<C1>>, Pair<Either<List<C1>, Unit>, Pair<Either<C1, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
         References.TEXT_COMPONENT.typeName(),
         DSL.or(
            DSL.or(DSL.string(), DSL.list(p_394638_)),
            DSL.and(
               DSL.optional(DSL.field("extra", DSL.list(p_394638_))),
               DSL.optional(DSL.field("separator", p_394638_)),
               DSL.optional(DSL.field("hoverEvent", p_393897_)),
               DSL.remainderType()
            )
         )
      );
      if (!type.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
         throw new IllegalStateException(
            "Text component type did not match, expected " + type + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
         );
      } else {
         Type<?> type1 = ExtraDataFixUtils.patchSubType(type, type, p_393676_);
         return this.fixTypeEverywhere(
            "TextComponentHoverAndClickEventFix",
            type,
            p_393676_,
            p_394396_ -> p_466483_ -> {
               boolean flag = (Boolean)((Either)p_466483_.getSecond()).map(p_394057_ -> false, p_394065_ -> {
                  Pair<Either<H, Unit>, Dynamic<?>> pair = (Pair<Either<H, Unit>, Dynamic<?>>)((Pair)p_394065_.getSecond()).getSecond();
                  boolean flag1 = ((Either)pair.getFirst()).left().isPresent();
                  boolean flag2 = ((Dynamic)pair.getSecond()).get("clickEvent").result().isPresent();
                  return flag1 || flag2;
               });
               return !flag
                  ? p_466483_
                  : Util.writeAndReadTypedOrThrow(
                        ExtraDataFixUtils.cast(type1, p_466483_, p_394396_), p_393676_, TextComponentHoverAndClickEventFix::fixTextComponent
                     )
                     .getValue();
            }
         );
      }
   }

   private static Dynamic<?> fixTextComponent(Dynamic<?> p_394354_) {
      return p_394354_.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent)
         .renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
   }

   private static Dynamic<?> copyFields(Dynamic<?> p_393943_, Dynamic<?> p_394371_, String... p_394668_) {
      for (String s : p_394668_) {
         p_393943_ = Dynamic.copyField(p_394371_, s, p_393943_, s);
      }

      return p_393943_;
   }

   private static Dynamic<?> fixHoverEvent(Dynamic<?> p_394485_) {
      String s = p_394485_.get("action").asString("");

      return switch (s) {
         case "show_text" -> p_394485_.renameField("contents", "value");
         case "show_item" -> {
            Dynamic<?> dynamic1 = p_394485_.get("contents").orElseEmptyMap();
            Optional<String> optional = dynamic1.asString().result();
            yield optional.isPresent()
               ? p_394485_.renameField("contents", "id")
               : copyFields(p_394485_.remove("contents"), dynamic1, "id", "count", "components");
         }
         case "show_entity" -> {
            Dynamic<?> dynamic = p_394485_.get("contents").orElseEmptyMap();
            yield copyFields(p_394485_.remove("contents"), dynamic, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
         }
         default -> p_394485_;
      };
   }

   @Nullable
   private static <T> Dynamic<T> fixClickEvent(Dynamic<T> p_394405_) {
      String s = p_394405_.get("action").asString("");
      String s1 = p_394405_.get("value").asString("");

      return switch (s) {
         case "open_url" -> !validateUri(s1) ? null : p_394405_.renameField("value", "url");
         case "open_file" -> p_394405_.renameField("value", "path");
         case "run_command", "suggest_command" -> !validateChat(s1) ? null : p_394405_.renameField("value", "command");
         case "change_page" -> {
            Integer integer = p_394405_.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse(null);
            if (integer == null) {
               yield null;
            } else {
               int i = Math.max(integer, 1);
               yield p_394405_.remove("value").set("page", p_394405_.createInt(i));
            }
         }
         default -> p_394405_;
      };
   }

   @Nullable
   private static Integer parseOldPage(Dynamic<?> p_394445_) {
      Optional<Number> optional = p_394445_.asNumber().result();
      if (optional.isPresent()) {
         return optional.get().intValue();
      } else {
         try {
            return Integer.parseInt(p_394445_.asString(""));
         } catch (Exception var3) {
            return null;
         }
      }
   }

   private static boolean validateUri(String p_393641_) {
      try {
         URI uri = new URI(p_393641_);
         String s = uri.getScheme();
         if (s == null) {
            return false;
         } else {
            String s1 = s.toLowerCase(Locale.ROOT);
            return "http".equals(s1) || "https".equals(s1);
         }
      } catch (URISyntaxException var4) {
         return false;
      }
   }

   private static boolean validateChat(String p_394051_) {
      for (int i = 0; i < p_394051_.length(); i++) {
         char c0 = p_394051_.charAt(i);
         if (c0 == 167 || c0 < ' ' || c0 == 127) {
            return false;
         }
      }

      return true;
   }
}
