package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServerAddress extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String address;
   public String resourcePackUrl;
   public String resourcePackHash;

   public static RealmsServerAddress parse(String p_87572_) {
      JsonParser $$1 = new JsonParser();
      RealmsServerAddress $$2 = new RealmsServerAddress();

      try {
         JsonObject $$3 = $$1.parse(p_87572_).getAsJsonObject();
         $$2.address = JsonUtils.getStringOr("address", $$3, null);
         $$2.resourcePackUrl = JsonUtils.getStringOr("resourcePackUrl", $$3, null);
         $$2.resourcePackHash = JsonUtils.getStringOr("resourcePackHash", $$3, null);
      } catch (Exception var4) {
         LOGGER.error("Could not parse RealmsServerAddress: {}", var4.getMessage());
      }

      return $$2;
   }
}
