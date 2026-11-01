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

   public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> ops, String data) {
      String string = createTextComponentJson(data);
      return new Dynamic(ops, ops.createString(string));
   }

   public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> ops) {
      return new Dynamic(ops, ops.createString(EMPTY_CONTENTS));
   }

   public static String createTextComponentJson(String json) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("text", json);
      return GsonHelper.toStableString(jsonObject);
   }

   public static String createTranslatableComponentJson(String json) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("translate", json);
      return GsonHelper.toStableString(jsonObject);
   }

   public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> ops, String data) {
      String string = createTranslatableComponentJson(data);
      return new Dynamic(ops, ops.createString(string));
   }

   public static String rewriteFromLenient(String data) {
      if (!data.isEmpty() && !data.equals("null")) {
         char c = data.charAt(0);
         char d = data.charAt(data.length() - 1);
         if (c == '"' && d == '"' || c == '{' && d == '}' || c == '[' && d == ']') {
            try {
               JsonElement jsonElement = LenientJsonParser.parse(data);
               if (jsonElement.isJsonPrimitive()) {
                  return createTextComponentJson(jsonElement.getAsString());
               }

               return GsonHelper.toStableString(jsonElement);
            } catch (JsonParseException var4) {
            }
         }

         return createTextComponentJson(data);
      } else {
         return EMPTY_CONTENTS;
      }
   }

   public static boolean isStrictlyValidJson(Dynamic<?> data) {
      return data.asString().result().filter(string -> {
         try {
            StrictJsonParser.parse(string);
            return true;
         } catch (JsonParseException var2) {
            return false;
         }
      }).isPresent();
   }

   public static Optional<String> extractTranslationString(String data) {
      try {
         JsonElement jsonElement = LenientJsonParser.parse(data);
         if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement jsonElement2 = jsonObject.get("translate");
            if (jsonElement2 != null && jsonElement2.isJsonPrimitive()) {
               return Optional.of(jsonElement2.getAsString());
            }
         }
      } catch (JsonParseException var4) {
      }

      return Optional.empty();
   }
}
