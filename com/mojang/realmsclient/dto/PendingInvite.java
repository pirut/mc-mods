package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Date;
import java.util.UUID;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PendingInvite extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String invitationId;
   public String realmName;
   public String realmOwnerName;
   public UUID realmOwnerUuid;
   public Date date;

   public static PendingInvite parse(JsonObject p_87431_) {
      PendingInvite $$1 = new PendingInvite();

      try {
         $$1.invitationId = JsonUtils.getStringOr("invitationId", p_87431_, "");
         $$1.realmName = JsonUtils.getStringOr("worldName", p_87431_, "");
         $$1.realmOwnerName = JsonUtils.getStringOr("worldOwnerName", p_87431_, "");
         $$1.realmOwnerUuid = JsonUtils.getUuidOr("worldOwnerUuid", p_87431_, Util.NIL_UUID);
         $$1.date = JsonUtils.getDateOr("date", p_87431_);
      } catch (Exception var3) {
         LOGGER.error("Could not parse PendingInvite: {}", var3.getMessage());
      }

      return $$1;
   }
}
