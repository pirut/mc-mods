package net.minecraft.locale;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;

public record DeprecatedTranslationsInfo(List<String> removed, Map<String, String> renamed) {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final DeprecatedTranslationsInfo EMPTY = new DeprecatedTranslationsInfo(List.of(), Map.of());
   public static final Codec<DeprecatedTranslationsInfo> CODEC = RecordCodecBuilder.create(
      p_362449_ -> p_362449_.group(
            Codec.STRING.listOf().fieldOf("removed").forGetter(DeprecatedTranslationsInfo::removed),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("renamed").forGetter(DeprecatedTranslationsInfo::renamed)
         )
         .apply(p_362449_, DeprecatedTranslationsInfo::new)
   );

   public static DeprecatedTranslationsInfo loadFromJson(InputStream p_362627_) {
      JsonElement jsonelement = StrictJsonParser.parse(new InputStreamReader(p_362627_, StandardCharsets.UTF_8));
      return (DeprecatedTranslationsInfo)CODEC.parse(JsonOps.INSTANCE, jsonelement)
         .getOrThrow(p_362302_ -> new IllegalStateException("Failed to parse deprecated language data: " + p_362302_));
   }

   public static DeprecatedTranslationsInfo loadFromResource(String p_364845_) {
      try {
         DeprecatedTranslationsInfo var2;
         try (InputStream inputstream = Language.class.getResourceAsStream(p_364845_)) {
            var2 = inputstream != null ? loadFromJson(inputstream) : EMPTY;
         }

         return var2;
      } catch (Exception var6) {
         LOGGER.error("Failed to read {}", p_364845_, var6);
         return EMPTY;
      }
   }

   public static DeprecatedTranslationsInfo loadFromDefaultResource() {
      return loadFromResource("/assets/minecraft/lang/deprecated.json");
   }

   public void applyToMap(Map<String, String> p_364396_) {
      for (String s : this.removed) {
         p_364396_.remove(s);
      }

      this.renamed.forEach((p_363658_, p_364541_) -> {
         String s1 = p_364396_.remove(p_363658_);
         if (s1 == null) {
            LOGGER.warn("Missing translation key for rename: {}", p_363658_);
            p_364396_.remove(p_364541_);
         } else {
            p_364396_.put(p_364541_, s1);
         }
      });
   }
}
