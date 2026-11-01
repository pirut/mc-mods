package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
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
public class PendingInvitesList extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public List<PendingInvite> pendingInvites = Lists.newArrayList();

   public static PendingInvitesList parse(String p_87437_) {
      PendingInvitesList $$1 = new PendingInvitesList();

      try {
         JsonParser $$2 = new JsonParser();
         JsonObject $$3 = $$2.parse(p_87437_).getAsJsonObject();
         if ($$3.get("invites").isJsonArray()) {
            Iterator<JsonElement> $$4 = $$3.get("invites").getAsJsonArray().iterator();

            while ($$4.hasNext()) {
               $$1.pendingInvites.add(PendingInvite.parse($$4.next().getAsJsonObject()));
            }
         }
      } catch (Exception var5) {
         LOGGER.error("Could not parse PendingInvitesList: {}", var5.getMessage());
      }

      return $$1;
   }
}
