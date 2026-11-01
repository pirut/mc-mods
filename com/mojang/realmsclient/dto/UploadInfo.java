package com.mojang.realmsclient.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record UploadInfo(boolean worldClosed, @Nullable String token, URI uploadEndpoint) {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_SCHEMA = "http://";
   private static final int DEFAULT_PORT = 8080;
   private static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");

   @Nullable
   public static UploadInfo parse(String json) {
      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         String string = JsonUtils.getStringOr("uploadEndpoint", jsonObject, null);
         if (string != null) {
            int i = JsonUtils.getIntOr("port", jsonObject, -1);
            URI uRI = assembleUri(string, i);
            if (uRI != null) {
               boolean bl = JsonUtils.getBooleanOr("worldClosed", jsonObject, false);
               String string2 = JsonUtils.getStringOr("token", jsonObject, null);
               return new UploadInfo(bl, string2, uRI);
            }
         }
      } catch (Exception var7) {
         LOGGER.error("Could not parse UploadInfo", var7);
      }

      return null;
   }

   @VisibleForTesting
   @Nullable
   public static URI assembleUri(String uri, int port) {
      Matcher matcher = URI_SCHEMA_PATTERN.matcher(uri);
      String string = ensureEndpointSchema(uri, matcher);

      try {
         URI uRI = new URI(string);
         int i = selectPortOrDefault(port, uRI.getPort());
         return i != uRI.getPort() ? new URI(uRI.getScheme(), uRI.getUserInfo(), uRI.getHost(), i, uRI.getPath(), uRI.getQuery(), uRI.getFragment()) : uRI;
      } catch (URISyntaxException var6) {
         LOGGER.warn("Failed to parse URI {}", string, var6);
         return null;
      }
   }

   private static int selectPortOrDefault(int port, int defaultPort) {
      if (port != -1) {
         return port;
      } else {
         return defaultPort != -1 ? defaultPort : 8080;
      }
   }

   private static String ensureEndpointSchema(String uri, Matcher matcher) {
      return matcher.find() ? uri : "http://" + uri;
   }

   public static String createRequest(@Nullable String token) {
      JsonObject jsonObject = new JsonObject();
      if (token != null) {
         jsonObject.addProperty("token", token);
      }

      return jsonObject.toString();
   }
}
