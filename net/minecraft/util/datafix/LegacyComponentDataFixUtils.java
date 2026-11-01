package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StrictJsonParser;

public class LegacyComponentDataFixUtils {
   private static final String EMPTY_CONTENTS = createTextComponentJson("");

   public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> p_394303_, String p_393613_) {
      String s = createTextComponentJson(p_393613_);
      return new Dynamic(p_394303_, p_394303_.createString(s));
   }

   public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> p_393609_) {
      return new Dynamic(p_393609_, p_393609_.createString(EMPTY_CONTENTS));
   }

   public static String createTextComponentJson(String p_394314_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("text", p_394314_);
      return GsonHelper.toStableString(jsonobject);
   }

   public static String createTranslatableComponentJson(String p_393777_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("translate", p_393777_);
      return GsonHelper.toStableString(jsonobject);
   }

   public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> p_393546_, String p_394262_) {
      String s = createTranslatableComponentJson(p_394262_);
      return new Dynamic(p_393546_, p_393546_.createString(s));
   }

   public static String rewriteFromLenient(String p_393742_) {
      if (!p_393742_.isEmpty() && !p_393742_.equals("null")) {
         char c0 = p_393742_.charAt(0);
         char c1 = p_393742_.charAt(p_393742_.length() - 1);
         if (c0 == '"' && c1 == '"' || c0 == '{' && c1 == '}' || c0 == '[' && c1 == ']') {
            try {
               JsonElement jsonelement = LenientJsonParser.parse(p_393742_);
               if (jsonelement.isJsonPrimitive()) {
                  return createTextComponentJson(jsonelement.getAsString());
               }

               return GsonHelper.toStableString(jsonelement);
            } catch (JsonParseException var4) {
            }
         }

         return createTextComponentJson(p_393742_);
      } else {
         return EMPTY_CONTENTS;
      }
   }

   public static boolean isStrictlyValidJson(Dynamic<?> p_446061_) {
      return p_446061_.asString().result().filter(p_446806_ -> {
         try {
            StrictJsonParser.parse(p_446806_);
            return true;
         } catch (JsonParseException var2) {
            return false;
         }
      }).isPresent();
   }

   public static Optional<String> extractTranslationString(String p_393970_) {
      try {
         JsonElement jsonelement = LenientJsonParser.parse(p_393970_);
         if (jsonelement.isJsonObject()) {
            JsonObject jsonobject = jsonelement.getAsJsonObject();
            JsonElement jsonelement1 = jsonobject.get("translate");
            if (jsonelement1 != null && jsonelement1.isJsonPrimitive()) {
               return Optional.of(jsonelement1.getAsString());
            }
         }
      } catch (JsonParseException var4) {
      }

      return Optional.empty();
   }
}
