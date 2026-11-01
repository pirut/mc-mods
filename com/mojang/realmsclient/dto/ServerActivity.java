package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerActivity extends ValueObject {
   public String profileUuid;
   public long joinTime;
   public long leaveTime;

   public static ServerActivity parse(JsonObject p_167317_) {
      ServerActivity $$1 = new ServerActivity();

      try {
         $$1.profileUuid = JsonUtils.getStringOr("profileUuid", p_167317_, null);
         $$1.joinTime = JsonUtils.getLongOr("joinTime", p_167317_, Long.MIN_VALUE);
         $$1.leaveTime = JsonUtils.getLongOr("leaveTime", p_167317_, Long.MIN_VALUE);
      } catch (Exception var3) {
      }

      return $$1;
   }
}
