package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServer extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_VALUE = -1;
   public long id;
   public String remoteSubscriptionId;
   public String name;
   public String motd;
   public RealmsServer.State state;
   public String owner;
   public UUID ownerUUID = Util.NIL_UUID;
   public List<PlayerInfo> players;
   public Map<Integer, RealmsWorldOptions> slots;
   public boolean expired;
   public boolean expiredTrial;
   public int daysLeft;
   public RealmsServer.WorldType worldType;
   public int activeSlot;
   @Nullable
   public String minigameName;
   public int minigameId;
   public String minigameImage;
   public long parentRealmId = -1L;
   @Nullable
   public String parentWorldName;
   public String activeVersion = "";
   public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;

   public String getDescription() {
      return this.motd;
   }

   public String getName() {
      return this.name;
   }

   @Nullable
   public String getMinigameName() {
      return this.minigameName;
   }

   public void setName(String p_87509_) {
      this.name = p_87509_;
   }

   public void setDescription(String p_87516_) {
      this.motd = p_87516_;
   }

   public static RealmsServer parse(JsonObject p_87500_) {
      RealmsServer $$1 = new RealmsServer();

      try {
         $$1.id = JsonUtils.getLongOr("id", p_87500_, -1L);
         $$1.remoteSubscriptionId = JsonUtils.getStringOr("remoteSubscriptionId", p_87500_, null);
         $$1.name = JsonUtils.getStringOr("name", p_87500_, null);
         $$1.motd = JsonUtils.getStringOr("motd", p_87500_, null);
         $$1.state = getState(JsonUtils.getStringOr("state", p_87500_, RealmsServer.State.CLOSED.name()));
         $$1.owner = JsonUtils.getStringOr("owner", p_87500_, null);
         if (p_87500_.get("players") != null && p_87500_.get("players").isJsonArray()) {
            $$1.players = parseInvited(p_87500_.get("players").getAsJsonArray());
            sortInvited($$1);
         } else {
            $$1.players = Lists.newArrayList();
         }

         $$1.daysLeft = JsonUtils.getIntOr("daysLeft", p_87500_, 0);
         $$1.expired = JsonUtils.getBooleanOr("expired", p_87500_, false);
         $$1.expiredTrial = JsonUtils.getBooleanOr("expiredTrial", p_87500_, false);
         $$1.worldType = getWorldType(JsonUtils.getStringOr("worldType", p_87500_, RealmsServer.WorldType.NORMAL.name()));
         $$1.ownerUUID = JsonUtils.getUuidOr("ownerUUID", p_87500_, Util.NIL_UUID);
         if (p_87500_.get("slots") != null && p_87500_.get("slots").isJsonArray()) {
            $$1.slots = parseSlots(p_87500_.get("slots").getAsJsonArray());
         } else {
            $$1.slots = createEmptySlots();
         }

         $$1.minigameName = JsonUtils.getStringOr("minigameName", p_87500_, null);
         $$1.activeSlot = JsonUtils.getIntOr("activeSlot", p_87500_, -1);
         $$1.minigameId = JsonUtils.getIntOr("minigameId", p_87500_, -1);
         $$1.minigameImage = JsonUtils.getStringOr("minigameImage", p_87500_, null);
         $$1.parentRealmId = JsonUtils.getLongOr("parentWorldId", p_87500_, -1L);
         $$1.parentWorldName = JsonUtils.getStringOr("parentWorldName", p_87500_, null);
         $$1.activeVersion = JsonUtils.getStringOr("activeVersion", p_87500_, "");
         $$1.compatibility = getCompatibility(JsonUtils.getStringOr("compatibility", p_87500_, RealmsServer.Compatibility.UNVERIFIABLE.name()));
      } catch (Exception var3) {
         LOGGER.error("Could not parse McoServer: {}", var3.getMessage());
      }

      return $$1;
   }

   private static void sortInvited(RealmsServer p_87505_) {
      p_87505_.players
         .sort(
            (p_87502_, p_87503_) -> ComparisonChain.start()
               .compareFalseFirst(p_87503_.getAccepted(), p_87502_.getAccepted())
               .compare(p_87502_.getName().toLowerCase(Locale.ROOT), p_87503_.getName().toLowerCase(Locale.ROOT))
               .result()
         );
   }

   private static List<PlayerInfo> parseInvited(JsonArray p_87498_) {
      List<PlayerInfo> $$1 = Lists.newArrayList();

      for (JsonElement $$2 : p_87498_) {
         try {
            JsonObject $$3 = $$2.getAsJsonObject();
            PlayerInfo $$4 = new PlayerInfo();
            $$4.setName(JsonUtils.getStringOr("name", $$3, null));
            $$4.setUuid(JsonUtils.getUuidOr("uuid", $$3, Util.NIL_UUID));
            $$4.setOperator(JsonUtils.getBooleanOr("operator", $$3, false));
            $$4.setAccepted(JsonUtils.getBooleanOr("accepted", $$3, false));
            $$4.setOnline(JsonUtils.getBooleanOr("online", $$3, false));
            $$1.add($$4);
         } catch (Exception var6) {
         }
      }

      return $$1;
   }

   private static Map<Integer, RealmsWorldOptions> parseSlots(JsonArray p_87514_) {
      Map<Integer, RealmsWorldOptions> $$1 = Maps.newHashMap();

      for (JsonElement $$2 : p_87514_) {
         try {
            JsonObject $$3 = $$2.getAsJsonObject();
            JsonParser $$4 = new JsonParser();
            JsonElement $$5 = $$4.parse($$3.get("options").getAsString());
            RealmsWorldOptions $$6;
            if ($$5 == null) {
               $$6 = RealmsWorldOptions.createDefaults();
            } else {
               $$6 = RealmsWorldOptions.parse($$5.getAsJsonObject());
            }

            int $$8 = JsonUtils.getIntOr("slotId", $$3, -1);
            $$1.put($$8, $$6);
         } catch (Exception var9) {
         }
      }

      for (int $$9 = 1; $$9 <= 3; $$9++) {
         if (!$$1.containsKey($$9)) {
            $$1.put($$9, RealmsWorldOptions.createEmptyDefaults());
         }
      }

      return $$1;
   }

   private static Map<Integer, RealmsWorldOptions> createEmptySlots() {
      Map<Integer, RealmsWorldOptions> $$0 = Maps.newHashMap();
      $$0.put(1, RealmsWorldOptions.createEmptyDefaults());
      $$0.put(2, RealmsWorldOptions.createEmptyDefaults());
      $$0.put(3, RealmsWorldOptions.createEmptyDefaults());
      return $$0;
   }

   public static RealmsServer parse(String p_87519_) {
      try {
         return parse(new JsonParser().parse(p_87519_).getAsJsonObject());
      } catch (Exception var2) {
         LOGGER.error("Could not parse McoServer: {}", var2.getMessage());
         return new RealmsServer();
      }
   }

   private static RealmsServer.State getState(String p_87526_) {
      try {
         return RealmsServer.State.valueOf(p_87526_);
      } catch (Exception var2) {
         return RealmsServer.State.CLOSED;
      }
   }

   private static RealmsServer.WorldType getWorldType(String p_87530_) {
      try {
         return RealmsServer.WorldType.valueOf(p_87530_);
      } catch (Exception var2) {
         return RealmsServer.WorldType.NORMAL;
      }
   }

   public static RealmsServer.Compatibility getCompatibility(@Nullable String p_305806_) {
      try {
         return RealmsServer.Compatibility.valueOf(p_305806_);
      } catch (Exception var2) {
         return RealmsServer.Compatibility.UNVERIFIABLE;
      }
   }

   public boolean isCompatible() {
      return this.compatibility.isCompatible();
   }

   public boolean needsUpgrade() {
      return this.compatibility.needsUpgrade();
   }

   public boolean needsDowngrade() {
      return this.compatibility.needsDowngrade();
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
   }

   @Override
   public boolean equals(Object p_87528_) {
      if (p_87528_ == null) {
         return false;
      } else if (p_87528_ == this) {
         return true;
      } else if (p_87528_.getClass() != this.getClass()) {
         return false;
      } else {
         RealmsServer $$1 = (RealmsServer)p_87528_;
         return new EqualsBuilder()
            .append(this.id, $$1.id)
            .append(this.name, $$1.name)
            .append(this.motd, $$1.motd)
            .append(this.state, $$1.state)
            .append(this.owner, $$1.owner)
            .append(this.expired, $$1.expired)
            .append(this.worldType, this.worldType)
            .isEquals();
      }
   }

   public RealmsServer clone() {
      RealmsServer $$0 = new RealmsServer();
      $$0.id = this.id;
      $$0.remoteSubscriptionId = this.remoteSubscriptionId;
      $$0.name = this.name;
      $$0.motd = this.motd;
      $$0.state = this.state;
      $$0.owner = this.owner;
      $$0.players = this.players;
      $$0.slots = this.cloneSlots(this.slots);
      $$0.expired = this.expired;
      $$0.expiredTrial = this.expiredTrial;
      $$0.daysLeft = this.daysLeft;
      $$0.worldType = this.worldType;
      $$0.ownerUUID = this.ownerUUID;
      $$0.minigameName = this.minigameName;
      $$0.activeSlot = this.activeSlot;
      $$0.minigameId = this.minigameId;
      $$0.minigameImage = this.minigameImage;
      $$0.parentWorldName = this.parentWorldName;
      $$0.parentRealmId = this.parentRealmId;
      $$0.activeVersion = this.activeVersion;
      $$0.compatibility = this.compatibility;
      return $$0;
   }

   public Map<Integer, RealmsWorldOptions> cloneSlots(Map<Integer, RealmsWorldOptions> p_87511_) {
      Map<Integer, RealmsWorldOptions> $$1 = Maps.newHashMap();

      for (Entry<Integer, RealmsWorldOptions> $$2 : p_87511_.entrySet()) {
         $$1.put($$2.getKey(), $$2.getValue().clone());
      }

      return $$1;
   }

   public boolean isSnapshotRealm() {
      return this.parentRealmId != -1L;
   }

   public boolean isMinigameActive() {
      return this.worldType == RealmsServer.WorldType.MINIGAME;
   }

   public String getWorldName(int p_87496_) {
      return this.name + " (" + this.slots.get(p_87496_).getSlotName(p_87496_) + ")";
   }

   public ServerData toServerData(String p_87523_) {
      return new ServerData(this.name, p_87523_, ServerData.Type.REALM);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Compatibility {
      UNVERIFIABLE,
      INCOMPATIBLE,
      RELEASE_TYPE_INCOMPATIBLE,
      NEEDS_DOWNGRADE,
      NEEDS_UPGRADE,
      COMPATIBLE;

      public boolean isCompatible() {
         return this == COMPATIBLE;
      }

      public boolean needsUpgrade() {
         return this == NEEDS_UPGRADE;
      }

      public boolean needsDowngrade() {
         return this == NEEDS_DOWNGRADE;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class McoServerComparator implements Comparator<RealmsServer> {
      private final String refOwner;

      public McoServerComparator(String p_87534_) {
         this.refOwner = p_87534_;
      }

      public int compare(RealmsServer p_87536_, RealmsServer p_87537_) {
         return ComparisonChain.start()
            .compareTrueFirst(p_87536_.isSnapshotRealm(), p_87537_.isSnapshotRealm())
            .compareTrueFirst(p_87536_.state == RealmsServer.State.UNINITIALIZED, p_87537_.state == RealmsServer.State.UNINITIALIZED)
            .compareTrueFirst(p_87536_.expiredTrial, p_87537_.expiredTrial)
            .compareTrueFirst(p_87536_.owner.equals(this.refOwner), p_87537_.owner.equals(this.refOwner))
            .compareFalseFirst(p_87536_.expired, p_87537_.expired)
            .compareTrueFirst(p_87536_.state == RealmsServer.State.OPEN, p_87537_.state == RealmsServer.State.OPEN)
            .compare(p_87536_.id, p_87537_.id)
            .result();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum State {
      CLOSED,
      OPEN,
      UNINITIALIZED;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum WorldType {
      NORMAL,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
