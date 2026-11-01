package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.i18n.I18nManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLanguage extends Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, String> storage;
   private final Map<String, Component> componentStorage;
   private final boolean defaultRightToLeft;

   @Deprecated
   private ClientLanguage(Map<String, String> p_118914_, boolean p_118915_) {
      this(p_118914_, p_118915_, Map.of());
   }

   private ClientLanguage(Map<String, String> p_118914_, boolean p_118915_, Map<String, Component> componentStorage) {
      this.storage = p_118914_;
      this.defaultRightToLeft = p_118915_;
      this.componentStorage = componentStorage;
   }

   public static ClientLanguage loadFrom(ResourceManager p_265765_, List<String> p_265743_, boolean p_265470_) {
      Map<String, String> map = Maps.newHashMap();
      Map<String, Component> componentMap = Maps.newHashMap();

      for (String s : p_265743_) {
         String s1 = String.format(Locale.ROOT, "lang/%s.json", s);
         map.putAll(I18nManager.loadTranslations(s));

         for (String s2 : p_265765_.getNamespaces()) {
            try {
               ResourceLocation resourcelocation = ResourceLocation.fromNamespaceAndPath(s2, s1);
               appendFrom(s, p_265765_.getResourceStack(resourcelocation), map, componentMap);
            } catch (Exception var11) {
               LOGGER.warn("Skipped language file: {}:{} ({})", new Object[]{s2, s1, var11.toString()});
            }
         }
      }

      return new ClientLanguage(ImmutableMap.copyOf(map), p_265470_, ImmutableMap.copyOf(componentMap));
   }

   @Deprecated
   private static void appendFrom(String p_235036_, List<Resource> p_235037_, Map<String, String> p_235038_) {
      appendFrom(p_235036_, p_235037_, p_235038_, new HashMap<>());
   }

   private static void appendFrom(String p_235036_, List<Resource> p_235037_, Map<String, String> p_235038_, Map<String, Component> componentMap) {
      for (Resource resource : p_235037_) {
         try (InputStream inputstream = resource.open()) {
            Language.loadFromJson(inputstream, p_235038_::put, componentMap::put);
         } catch (IOException var11) {
            LOGGER.warn("Failed to load translations for {} from pack {}", new Object[]{p_235036_, resource.sourcePackId(), var11});
         }
      }
   }

   @Override
   public String getOrDefault(String p_118920_, String p_265273_) {
      return this.storage.getOrDefault(p_118920_, p_265273_);
   }

   @Override
   public boolean has(String p_118928_) {
      return this.storage.containsKey(p_118928_);
   }

   @Override
   public boolean isDefaultRightToLeft() {
      return this.defaultRightToLeft;
   }

   @Override
   public FormattedCharSequence getVisualOrder(FormattedText p_118925_) {
      return FormattedBidiReorder.reorder(p_118925_, this.defaultRightToLeft);
   }

   @Override
   public Map<String, String> getLanguageData() {
      return this.storage;
   }

   @Nullable
   @Override
   public Component getComponent(String key) {
      return this.componentStorage.get(key);
   }
}
