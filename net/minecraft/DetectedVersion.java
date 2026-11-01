package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

public class DetectedVersion {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final WorldVersion BUILT_IN = createBuiltIn(UUID.randomUUID().toString().replaceAll("-", ""), "Development Version");

   public static WorldVersion createBuiltIn(String p_433428_, String p_433812_) {
      return createBuiltIn(p_433428_, p_433812_, true);
   }

   public static WorldVersion createBuiltIn(String p_434552_, String p_434237_, boolean p_435203_) {
      return new WorldVersion.Simple(
         p_434552_,
         p_434237_,
         new DataVersion(4671, "main"),
         SharedConstants.getProtocolVersion(),
         PackFormat.of(75, 0),
         PackFormat.of(94, 1),
         new Date(),
         p_435203_
      );
   }

   private static WorldVersion createFromJson(JsonObject p_415640_) {
      JsonObject jsonobject = GsonHelper.getAsJsonObject(p_415640_, "pack_version");
      return new WorldVersion.Simple(
         GsonHelper.getAsString(p_415640_, "id"),
         GsonHelper.getAsString(p_415640_, "name"),
         new DataVersion(GsonHelper.getAsInt(p_415640_, "world_version"), GsonHelper.getAsString(p_415640_, "series_id", "main")),
         GsonHelper.getAsInt(p_415640_, "protocol_version"),
         PackFormat.of(GsonHelper.getAsInt(jsonobject, "resource_major"), GsonHelper.getAsInt(jsonobject, "resource_minor")),
         PackFormat.of(GsonHelper.getAsInt(jsonobject, "data_major"), GsonHelper.getAsInt(jsonobject, "data_minor")),
         Date.from(ZonedDateTime.parse(GsonHelper.getAsString(p_415640_, "build_time")).toInstant()),
         GsonHelper.getAsBoolean(p_415640_, "stable")
      );
   }

   public static WorldVersion tryDetectVersion() {
      try {
         WorldVersion worldversion;
         try (InputStream inputstream = DetectedVersion.class.getResourceAsStream("/version.json")) {
            if (inputstream == null) {
               LOGGER.warn("Missing version information!");
               return BUILT_IN;
            }

            try (InputStreamReader inputstreamreader = new InputStreamReader(inputstream, StandardCharsets.UTF_8)) {
               worldversion = createFromJson(GsonHelper.parse(inputstreamreader));
            }
         }

         return worldversion;
      } catch (IOException | JsonParseException var9) {
         throw new IllegalStateException("Game version information is corrupt", var9);
      }
   }
}
