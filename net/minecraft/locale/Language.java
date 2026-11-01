package net.minecraft.locale;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringDecomposer;
import net.neoforged.neoforge.server.LanguageHook;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();
   private static final Pattern UNSUPPORTED_FORMAT_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
   public static final String DEFAULT = "en_us";
   private static volatile Language instance = loadDefault();

   private static Language loadDefault() {
      DeprecatedTranslationsInfo deprecatedtranslationsinfo = DeprecatedTranslationsInfo.loadFromDefaultResource();
      final Map<String, String> map = new HashMap<>();
      BiConsumer<String, String> biconsumer = map::put;
      final Map<String, Component> componentMap = new HashMap<>();
      parseTranslations(biconsumer, componentMap::put, "/assets/minecraft/lang/en_us.json");
      deprecatedtranslationsinfo.applyToMap(map);
      LanguageHook.captureLanguageMap(map, componentMap);
      return new Language() {
         @Override
         public String getOrDefault(String p_128127_, String p_265421_) {
            return map.getOrDefault(p_128127_, p_265421_);
         }

         @Override
         public boolean has(String p_128135_) {
            return map.containsKey(p_128135_);
         }

         @Override
         public boolean isDefaultRightToLeft() {
            return false;
         }

         @Override
         public FormattedCharSequence getVisualOrder(FormattedText p_128129_) {
            return p_128132_ -> p_128129_.visit(
                  (p_177835_, p_177836_) -> StringDecomposer.iterateFormatted(p_177836_, p_177835_, p_128132_)
                     ? Optional.empty()
                     : FormattedText.STOP_ITERATION,
                  Style.EMPTY
               )
               .isPresent();
         }

         @Override
         public Map<String, String> getLanguageData() {
            return map;
         }

         @Nullable
         @Override
         public Component getComponent(String key) {
            return componentMap.get(key);
         }
      };
   }

   @Deprecated
   private static void parseTranslations(BiConsumer<String, String> p_282031_, String p_283638_) {
      parseTranslations(p_282031_, (key, value) -> {}, p_283638_);
   }

   private static void parseTranslations(BiConsumer<String, String> p_282031_, BiConsumer<String, Component> componentConsumer, String p_283638_) {
      try (InputStream inputstream = Language.class.getResourceAsStream(p_283638_)) {
         loadFromJson(inputstream, p_282031_, componentConsumer);
      } catch (IOException | JsonParseException var8) {
         LOGGER.error("Couldn't read strings from {}", p_283638_, var8);
      }
   }

   public static void loadFromJson(InputStream p_128109_, BiConsumer<String, String> p_128110_) {
      loadFromJson(p_128109_, p_128110_, (key, value) -> {});
   }

   public static void loadFromJson(InputStream p_128109_, BiConsumer<String, String> p_128110_, BiConsumer<String, Component> componentConsumer) {
      JsonObject jsonobject = (JsonObject)GSON.fromJson(new InputStreamReader(p_128109_, StandardCharsets.UTF_8), JsonObject.class);

      for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
         if (!entry.getValue().isJsonArray() && !entry.getValue().isJsonObject()) {
            String s = UNSUPPORTED_FORMAT_PATTERN.matcher(GsonHelper.convertToString(entry.getValue(), entry.getKey())).replaceAll("%$1s");
            p_128110_.accept(entry.getKey(), s);
         } else {
            Component component = (Component)ComponentSerialization.CODEC
               .parse(JsonOps.INSTANCE, entry.getValue())
               .getOrThrow(msg -> new JsonParseException("Error parsing translation for " + entry.getKey() + ": " + msg));
            p_128110_.accept(entry.getKey(), component.getString());
            componentConsumer.accept(entry.getKey(), component);
         }
      }
   }

   public static Language getInstance() {
      return instance;
   }

   public static void inject(Language p_128115_) {
      instance = p_128115_;
   }

   public Map<String, String> getLanguageData() {
      return ImmutableMap.of();
   }

   @Nullable
   public Component getComponent(String key) {
      return null;
   }

   public String getOrDefault(String p_128111_) {
      return this.getOrDefault(p_128111_, p_128111_);
   }

   public abstract String getOrDefault(String var1, String var2);

   public abstract boolean has(String var1);

   public abstract boolean isDefaultRightToLeft();

   public abstract FormattedCharSequence getVisualOrder(FormattedText var1);

   public List<FormattedCharSequence> getVisualOrder(List<FormattedText> p_128113_) {
      return p_128113_.stream().map(this::getVisualOrder).collect(ImmutableList.toImmutableList());
   }
}
