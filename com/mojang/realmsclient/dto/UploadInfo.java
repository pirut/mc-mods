package com.mojang.realmsclient.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class UploadInfo extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_SCHEMA = "http://";
   private static final int DEFAULT_PORT = 8080;
   private static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");
   private final boolean worldClosed;
   @Nullable
   private final String token;
   private final URI uploadEndpoint;

   private UploadInfo(boolean p_87693_, @Nullable String p_87694_, URI p_87695_) {
      this.worldClosed = p_87693_;
      this.token = p_87694_;
      this.uploadEndpoint = p_87695_;
   }

   @Nullable
   public static UploadInfo parse(String p_87701_) {
      try {
         JsonParser $$1 = new JsonParser();
         JsonObject $$2 = $$1.parse(p_87701_).getAsJsonObject();
         String $$3 = JsonUtils.getStringOr("uploadEndpoint", $$2, null);
         if ($$3 != null) {
            int $$4 = JsonUtils.getIntOr("port", $$2, -1);
            URI $$5 = assembleUri($$3, $$4);
            if ($$5 != null) {
               boolean $$6 = JsonUtils.getBooleanOr("worldClosed", $$2, false);
               String $$7 = JsonUtils.getStringOr("token", $$2, null);
               return new UploadInfo($$6, $$7, $$5);
            }
         }
      } catch (Exception var8) {
         LOGGER.error("Could not parse UploadInfo: {}", var8.getMessage());
      }

      return null;
   }

   @Nullable
   @VisibleForTesting
   public static URI assembleUri(String p_87703_, int p_87704_) {
      Matcher $$2 = URI_SCHEMA_PATTERN.matcher(p_87703_);
      String $$3 = ensureEndpointSchema(p_87703_, $$2);

      try {
         URI $$4 = new URI($$3);
         int $$5 = selectPortOrDefault(p_87704_, $$4.getPort());
         return $$5 != $$4.getPort() ? new URI($$4.getScheme(), $$4.getUserInfo(), $$4.getHost(), $$5, $$4.getPath(), $$4.getQuery(), $$4.getFragment()) : $$4;
      } catch (URISyntaxException var6) {
         LOGGER.warn("Failed to parse URI {}", $$3, var6);
         return null;
      }
   }

   private static int selectPortOrDefault(int p_87698_, int p_87699_) {
      if (p_87698_ != -1) {
         return p_87698_;
      } else {
         return p_87699_ != -1 ? p_87699_ : 8080;
      }
   }

   private static String ensureEndpointSchema(String p_87706_, Matcher p_87707_) {
      return p_87707_.find() ? p_87706_ : "http://" + p_87706_;
   }

   public static String createRequest(@Nullable String p_87710_) {
      JsonObject $$1 = new JsonObject();
      if (p_87710_ != null) {
         $$1.addProperty("token", p_87710_);
      }

      return $$1.toString();
   }

   @Nullable
   public String getToken() {
      return this.token;
   }

   public URI getUploadEndpoint() {
      return this.uploadEndpoint;
   }

   public boolean isWorldClosed() {
      return this.worldClosed;
   }
}
