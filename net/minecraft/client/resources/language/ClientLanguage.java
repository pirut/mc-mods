package net.minecraft.client.resources.language;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientLanguage extends Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, String> storage;
   private final boolean defaultRightToLeft;

   private ClientLanguage(Map<String, String> storage, boolean defaultRightToLeft) {
      this.storage = storage;
      this.defaultRightToLeft = defaultRightToLeft;
   }

   public static ClientLanguage loadFrom(ResourceManager resourceManager, List<String> filenames, boolean defaultRightToLeft) {
      Map<String, String> map = new HashMap<>();

      for (String string : filenames) {
         String string2 = String.format(Locale.ROOT, "lang/%s.json", string);

         for (String string3 : resourceManager.getNamespaces()) {
            try {
               Identifier identifier = Identifier.fromNamespaceAndPath(string3, string2);
               appendFrom(string, resourceManager.getResourceStack(identifier), map);
            } catch (Exception var10) {
               LOGGER.warn("Skipped language file: {}:{} ({})", new Object[]{string3, string2, var10.toString()});
            }
         }
      }

      DeprecatedTranslationsInfo.loadFromDefaultResource().applyToMap(map);
      return new ClientLanguage(Map.copyOf(map), defaultRightToLeft);
   }

   private static void appendFrom(String languageName, List<Resource> resources, Map<String, String> destinationMap) {
      for (Resource resource : resources) {
         try (InputStream inputStream = resource.open()) {
            Language.loadFromJson(inputStream, destinationMap::put);
         } catch (IOException var10) {
            LOGGER.warn("Failed to load translations for {} from pack {}", new Object[]{languageName, resource.sourcePackId(), var10});
         }
      }
   }

   @Override
   public String getOrDefault(String key, String defaultValue) {
      return this.storage.getOrDefault(key, defaultValue);
   }

   @Override
   public boolean has(String id) {
      return this.storage.containsKey(id);
   }

   @Override
   public boolean isDefaultRightToLeft() {
      return this.defaultRightToLeft;
   }

   @Override
   public FormattedCharSequence getVisualOrder(FormattedText text) {
      return FormattedBidiReorder.reorder(text, this.defaultRightToLeft);
   }
}
