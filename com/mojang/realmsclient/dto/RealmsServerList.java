package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServerList extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public List<RealmsServer> servers;

   public static RealmsServerList parse(String p_87578_) {
      RealmsServerList $$1 = new RealmsServerList();
      $$1.servers = Lists.newArrayList();

      try {
         JsonParser $$2 = new JsonParser();
         JsonObject $$3 = $$2.parse(p_87578_).getAsJsonObject();
         if ($$3.get("servers").isJsonArray()) {
            JsonArray $$4 = $$3.get("servers").getAsJsonArray();
            Iterator<JsonElement> $$5 = $$4.iterator();

            while ($$5.hasNext()) {
               $$1.servers.add(RealmsServer.parse($$5.next().getAsJsonObject()));
            }
         }
      } catch (Exception var6) {
         LOGGER.error("Could not parse McoServerList: {}", var6.getMessage());
      }

      return $$1;
   }
}
