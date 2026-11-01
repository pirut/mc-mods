package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record WorldTemplate(
   String id,
   String name,
   String version,
   String author,
   String link,
   @Nullable String image,
   String trailer,
   String recommendedPlayers,
   WorldTemplate.WorldTemplateType type
) {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Nullable
   public static WorldTemplate parse(JsonObject json) {
      try {
         String string = JsonUtils.getStringOr("type", json, null);
         return new WorldTemplate(
            JsonUtils.getStringOr("id", json, ""),
            JsonUtils.getStringOr("name", json, ""),
            JsonUtils.getStringOr("version", json, ""),
            JsonUtils.getStringOr("author", json, ""),
            JsonUtils.getStringOr("link", json, ""),
            JsonUtils.getStringOr("image", json, null),
            JsonUtils.getStringOr("trailer", json, ""),
            JsonUtils.getStringOr("recommendedPlayers", json, ""),
            string == null ? WorldTemplate.WorldTemplateType.WORLD_TEMPLATE : WorldTemplate.WorldTemplateType.valueOf(string)
         );
      } catch (Exception var2) {
         LOGGER.error("Could not parse WorldTemplate", var2);
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum WorldTemplateType {
      WORLD_TEMPLATE,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
