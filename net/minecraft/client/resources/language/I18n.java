package net.minecraft.client.resources.language;

import java.util.IllegalFormatException;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.locale.Language;

@Environment(EnvType.CLIENT)
public class I18n {
   private static volatile Language language = Language.getInstance();

   private I18n() {
   }

   static void setLanguage(Language language) {
      I18n.language = language;
   }

   public static String get(String translateKey, Object... parameters) {
      String string = language.getOrDefault(translateKey);

      try {
         return String.format(Locale.ROOT, string, parameters);
      } catch (IllegalFormatException var4) {
         return "Format error: " + string;
      }
   }

   public static boolean exists(String key) {
      return language.has(key);
   }
}
