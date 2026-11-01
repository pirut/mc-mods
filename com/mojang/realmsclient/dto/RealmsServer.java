package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.util.UUIDTypeAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsServer extends ValueObject implements ReflectionBasedSerialization {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_VALUE = -1;
   public static final Component WORLD_CLOSED_COMPONENT = Component.translatable("mco.play.button.realm.closed");
   @SerializedName("id")
   public long id = -1L;
   @SerializedName("remoteSubscriptionId")
   @Nullable
   public String remoteSubscriptionId;
   @SerializedName("name")
   @Nullable
   public String name;
   @SerializedName("motd")
   public String motd = "";
   @SerializedName("state")
   public RealmsServer.State state = RealmsServer.State.CLOSED;
   @SerializedName("owner")
   @Nullable
   public String owner;
   @SerializedName("ownerUUID")
   @JsonAdapter(UUIDTypeAdapter.class)
   public UUID ownerUUID = Util.NIL_UUID;
   @SerializedName("players")
   public List<PlayerInfo> players = Lists.newArrayList();
   @SerializedName("slots")
   private List<RealmsSlot> slotList = createEmptySlots();
   @Exclude
   public Map<Integer, RealmsSlot> slots = new HashMap<>();
   @SerializedName("expired")
   public boolean expired;
   @SerializedName("expiredTrial")
   public boolean expiredTrial = false;
   @SerializedName("daysLeft")
   public int daysLeft;
   @SerializedName("worldType")
   public RealmsServer.WorldType worldType = RealmsServer.WorldType.NORMAL;
   @SerializedName("isHardcore")
   public boolean isHardcore = false;
   @SerializedName("gameMode")
   public int gameMode = -1;
   @SerializedName("activeSlot")
   public int activeSlot = -1;
   @SerializedName("minigameName")
   @Nullable
   public String minigameName;
   @SerializedName("minigameId")
   public int minigameId = -1;
   @SerializedName("minigameImage")
   @Nullable
   public String minigameImage;
   @SerializedName("parentWorldId")
   public long parentRealmId = -1L;
   @SerializedName("parentWorldName")
   @Nullable
   public String parentWorldName;
   @SerializedName("activeVersion")
   public String activeVersion = "";
   @SerializedName("compatibility")
   public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
   @SerializedName("regionSelectionPreference")
   @Nullable
   public RegionSelectionPreferenceDto regionSelectionPreference;

   public String getDescription() {
      return this.motd;
   }

   @Nullable
   public String getName() {
      return this.name;
   }

   @Nullable
   public String getMinigameName() {
      return this.minigameName;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setDescription(String motd) {
      this.motd = motd;
   }

   public static RealmsServer parse(GuardedSerializer serializer, String json) {
      try {
         RealmsServer realmsServer = serializer.fromJson(json, RealmsServer.class);
         if (realmsServer == null) {
            LOGGER.error("Could not parse McoServer: {}", json);
            return new RealmsServer();
         } else {
            finalize(realmsServer);
            return realmsServer;
         }
      } catch (Exception var3) {
         LOGGER.error("Could not parse McoServer", var3);
         return new RealmsServer();
      }
   }

   public static void finalize(RealmsServer server) {
      if (server.players == null) {
         server.players = Lists.newArrayList();
      }

      if (server.slotList == null) {
         server.slotList = createEmptySlots();
      }

      if (server.slots == null) {
         server.slots = new HashMap<>();
      }

      if (server.worldType == null) {
         server.worldType = RealmsServer.WorldType.NORMAL;
      }

      if (server.activeVersion == null) {
         server.activeVersion = "";
      }

      if (server.compatibility == null) {
         server.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
      }

      if (server.regionSelectionPreference == null) {
         server.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
      }

      sortInvited(server);
      finalizeSlots(server);
   }

   private static void sortInvited(RealmsServer server) {
      server.players
         .sort(
            (playerInfo, playerInfo2) -> ComparisonChain.start()
               .compareFalseFirst(playerInfo2.accepted, playerInfo.accepted)
               .compare(playerInfo.name.toLowerCase(Locale.ROOT), playerInfo2.name.toLowerCase(Locale.ROOT))
               .result()
         );
   }

   private static void finalizeSlots(RealmsServer server) {
      server.slotList.forEach(realmsSlot -> server.slots.put(realmsSlot.slotId, realmsSlot));

      for (int i = 1; i <= 3; i++) {
         if (!server.slots.containsKey(i)) {
            server.slots.put(i, RealmsSlot.defaults(i));
         }
      }
   }

   private static List<RealmsSlot> createEmptySlots() {
      List<RealmsSlot> list = new ArrayList<>();
      list.add(RealmsSlot.defaults(1));
      list.add(RealmsSlot.defaults(2));
      list.add(RealmsSlot.defaults(3));
      return list;
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

   public boolean shouldPlayButtonBeActive() {
      boolean bl = !this.expired && this.state == RealmsServer.State.OPEN;
      return bl && (this.isCompatible() || this.needsUpgrade() || this.isSelfOwnedServer());
   }

   private boolean isSelfOwnedServer() {
      return Minecraft.getInstance().isLocalPlayer(this.ownerUUID);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
   }

   @Override
   public boolean equals(Object object) {
      if (object == null) {
         return false;
      } else if (object == this) {
         return true;
      } else if (object.getClass() != this.getClass()) {
         return false;
      } else {
         RealmsServer realmsServer = (RealmsServer)object;
         return new EqualsBuilder()
            .append(this.id, realmsServer.id)
            .append(this.name, realmsServer.name)
            .append(this.motd, realmsServer.motd)
            .append(this.state, realmsServer.state)
            .append(this.owner, realmsServer.owner)
            .append(this.expired, realmsServer.expired)
            .append(this.worldType, this.worldType)
            .isEquals();
      }
   }

   public RealmsServer copy() {
      RealmsServer realmsServer = new RealmsServer();
      realmsServer.id = this.id;
      realmsServer.remoteSubscriptionId = this.remoteSubscriptionId;
      realmsServer.name = this.name;
      realmsServer.motd = this.motd;
      realmsServer.state = this.state;
      realmsServer.owner = this.owner;
      realmsServer.players = this.players;
      realmsServer.slotList = this.slotList.stream().map(RealmsSlot::copy).toList();
      realmsServer.slots = this.cloneSlots(this.slots);
      realmsServer.expired = this.expired;
      realmsServer.expiredTrial = this.expiredTrial;
      realmsServer.daysLeft = this.daysLeft;
      realmsServer.worldType = this.worldType;
      realmsServer.isHardcore = this.isHardcore;
      realmsServer.gameMode = this.gameMode;
      realmsServer.ownerUUID = this.ownerUUID;
      realmsServer.minigameName = this.minigameName;
      realmsServer.activeSlot = this.activeSlot;
      realmsServer.minigameId = this.minigameId;
      realmsServer.minigameImage = this.minigameImage;
      realmsServer.parentWorldName = this.parentWorldName;
      realmsServer.parentRealmId = this.parentRealmId;
      realmsServer.activeVersion = this.activeVersion;
      realmsServer.compatibility = this.compatibility;
      realmsServer.regionSelectionPreference = this.regionSelectionPreference != null ? this.regionSelectionPreference.copy() : null;
      return realmsServer;
   }

   public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> slots) {
      Map<Integer, RealmsSlot> map = Maps.newHashMap();

      for (Entry<Integer, RealmsSlot> entry : slots.entrySet()) {
         map.put(entry.getKey(), new RealmsSlot(entry.getKey(), entry.getValue().options.copy(), entry.getValue().settings));
      }

      return map;
   }

   public boolean isSnapshotRealm() {
      return this.parentRealmId != -1L;
   }

   public boolean isMinigameActive() {
      return this.worldType == RealmsServer.WorldType.MINIGAME;
   }

   public String getWorldName(int slot) {
      return this.name == null ? this.slots.get(slot).options.getSlotName(slot) : this.name + " (" + this.slots.get(slot).options.getSlotName(slot) + ")";
   }

   public ServerData toServerData(String ip) {
      return new ServerData(Objects.requireNonNullElse(this.name, "unknown server"), ip, ServerData.Type.REALM);
   }

   @Environment(EnvType.CLIENT)
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

   @Environment(EnvType.CLIENT)
   public static class McoServerComparator implements Comparator<RealmsServer> {
      private final String refOwner;

      public McoServerComparator(String refOwner) {
         this.refOwner = refOwner;
      }

      public int compare(RealmsServer first, RealmsServer second) {
         return ComparisonChain.start()
            .compareTrueFirst(first.isSnapshotRealm(), second.isSnapshotRealm())
            .compareTrueFirst(first.state == RealmsServer.State.UNINITIALIZED, second.state == RealmsServer.State.UNINITIALIZED)
            .compareTrueFirst(first.expiredTrial, second.expiredTrial)
            .compareTrueFirst(Objects.equals(first.owner, this.refOwner), Objects.equals(second.owner, this.refOwner))
            .compareFalseFirst(first.expired, second.expired)
            .compareTrueFirst(first.state == RealmsServer.State.OPEN, second.state == RealmsServer.State.OPEN)
            .compare(first.id, second.id)
            .result();
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum State {
      CLOSED,
      OPEN,
      UNINITIALIZED;
   }

   @Environment(EnvType.CLIENT)
   public static enum WorldType {
      NORMAL("normal"),
      MINIGAME("minigame"),
      ADVENTUREMAP("adventureMap"),
      EXPERIENCE("experience"),
      INSPIRATION("inspiration"),
      UNKNOWN("unknown");

      private static final String TRANSLATION_PREFIX = "mco.backup.entry.worldType.";
      private final Component displayName;

      private WorldType(final String name) {
         this.displayName = Component.translatable("mco.backup.entry.worldType." + name);
      }

      public Component getDisplayName() {
         return this.displayName;
      }
   }
}
