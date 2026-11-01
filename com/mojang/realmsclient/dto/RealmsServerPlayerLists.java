package com.mojang.realmsclient.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServerPlayerLists extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public Map<Long, List<ProfileResult>> servers = Map.of();

   public static RealmsServerPlayerLists parse(String p_87597_) {
      RealmsServerPlayerLists $$1 = new RealmsServerPlayerLists();
      Builder<Long, List<ProfileResult>> $$2 = ImmutableMap.builder();

      try {
         JsonObject $$3 = GsonHelper.parse(p_87597_);
         if (GsonHelper.isArrayNode($$3, "lists")) {
            for (JsonElement $$5 : $$3.getAsJsonArray("lists")) {
               JsonObject $$6 = $$5.getAsJsonObject();
               String $$7 = JsonUtils.getStringOr("playerList", $$6, null);
               List<ProfileResult> $$9;
               if ($$7 != null) {
                  JsonElement $$8 = JsonParser.parseString($$7);
                  if ($$8.isJsonArray()) {
                     $$9 = parsePlayers($$8.getAsJsonArray());
                  } else {
                     $$9 = Lists.newArrayList();
                  }
               } else {
                  $$9 = Lists.newArrayList();
               }

               $$2.put(JsonUtils.getLongOr("serverId", $$6, -1L), $$9);
            }
         }
      } catch (Exception var11) {
         LOGGER.error("Could not parse RealmsServerPlayerLists: {}", var11.getMessage());
      }

      $$1.servers = $$2.build();
      return $$1;
   }

   private static List<ProfileResult> parsePlayers(JsonArray p_350938_) {
      List<ProfileResult> $$1 = new ArrayList<>(p_350938_.size());
      MinecraftSessionService $$2 = Minecraft.getInstance().getMinecraftSessionService();

      for (JsonElement $$3 : p_350938_) {
         if ($$3.isJsonObject()) {
            UUID $$4 = JsonUtils.getUuidOr("playerId", $$3.getAsJsonObject(), null);
            if ($$4 != null && !Minecraft.getInstance().isLocalPlayer($$4)) {
               try {
                  ProfileResult $$5 = $$2.fetchProfile($$4, false);
                  if ($$5 != null) {
                     $$1.add($$5);
                  }
               } catch (Exception var7) {
                  LOGGER.error("Could not get name for {}", $$4, var7);
               }
            }
         }
      }

      return $$1;
   }

   public List<ProfileResult> getProfileResultsFor(long p_350315_) {
      List<ProfileResult> $$1 = this.servers.get(p_350315_);
      return $$1 != null ? $$1 : List.of();
   }
}
