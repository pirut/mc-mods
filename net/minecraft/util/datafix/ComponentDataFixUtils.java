package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;

public class ComponentDataFixUtils {
   private static final String EMPTY_CONTENTS = createTextComponentJson("");

   public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> p_304546_, String p_304390_) {
      String $$2 = createTextComponentJson(p_304390_);
      return new Dynamic(p_304546_, p_304546_.createString($$2));
   }

   public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> p_304990_) {
      return new Dynamic(p_304990_, p_304990_.createString(EMPTY_CONTENTS));
   }

   private static String createTextComponentJson(String p_304837_) {
      JsonObject $$1 = new JsonObject();
      $$1.addProperty("text", p_304837_);
      return GsonHelper.toStableString($$1);
   }

   public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> p_304499_, String p_304830_) {
      JsonObject $$2 = new JsonObject();
      $$2.addProperty("translate", p_304830_);
      return new Dynamic(p_304499_, p_304499_.createString(GsonHelper.toStableString($$2)));
   }

   public static <T> Dynamic<T> wrapLiteralStringAsComponent(Dynamic<T> p_304540_) {
      return (Dynamic<T>)DataFixUtils.orElse(p_304540_.asString().map(p_304989_ -> createPlainTextComponent(p_304540_.getOps(), p_304989_)).result(), p_304540_);
   }

   public static Dynamic<?> rewriteFromLenient(Dynamic<?> p_323863_) {
      Optional<String> $$1 = p_323863_.asString().result();
      if ($$1.isEmpty()) {
         return p_323863_;
      } else {
         String $$2 = $$1.get();
         if (!$$2.isEmpty() && !$$2.equals("null")) {
            char $$3 = $$2.charAt(0);
            char $$4 = $$2.charAt($$2.length() - 1);
            if ($$3 == '"' && $$4 == '"' || $$3 == '{' && $$4 == '}' || $$3 == '[' && $$4 == ']') {
               try {
                  JsonElement $$5 = JsonParser.parseString($$2);
                  if ($$5.isJsonPrimitive()) {
                     return createPlainTextComponent(p_323863_.getOps(), $$5.getAsString());
                  }

                  return p_323863_.createString(GsonHelper.toStableString($$5));
               } catch (JsonParseException var6) {
               }
            }

            return createPlainTextComponent(p_323863_.getOps(), $$2);
         } else {
            return createEmptyComponent(p_323863_.getOps());
         }
      }
   }

   public static Optional<String> extractTranslationString(String p_338737_) {
      try {
         JsonElement $$1 = JsonParser.parseString(p_338737_);
         if ($$1.isJsonObject()) {
            JsonObject $$2 = $$1.getAsJsonObject();
            JsonElement $$3 = $$2.get("translate");
            if ($$3 != null && $$3.isJsonPrimitive()) {
               return Optional.of($$3.getAsString());
            }
         }
      } catch (JsonParseException var4) {
      }

      return Optional.empty();
   }
}
