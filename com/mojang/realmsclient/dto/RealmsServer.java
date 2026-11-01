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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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

   public void setName(String p_87509_) {
      this.name = p_87509_;
   }

   public void setDescription(String p_87516_) {
      this.motd = p_87516_;
   }

   public static RealmsServer parse(GuardedSerializer p_419772_, String p_419625_) {
      try {
         RealmsServer realmsserver = p_419772_.fromJson(p_419625_, RealmsServer.class);
         if (realmsserver == null) {
            LOGGER.error("Could not parse McoServer: {}", p_419625_);
            return new RealmsServer();
         } else {
            finalize(realmsserver);
            return realmsserver;
         }
      } catch (Exception var3) {
         LOGGER.error("Could not parse McoServer", var3);
         return new RealmsServer();
      }
   }

   public static void finalize(RealmsServer p_419489_) {
      if (p_419489_.players == null) {
         p_419489_.players = Lists.newArrayList();
      }

      if (p_419489_.slotList == null) {
         p_419489_.slotList = createEmptySlots();
      }

      if (p_419489_.slots == null) {
         p_419489_.slots = new HashMap<>();
      }

      if (p_419489_.worldType == null) {
         p_419489_.worldType = RealmsServer.WorldType.NORMAL;
      }

      if (p_419489_.activeVersion == null) {
         p_419489_.activeVersion = "";
      }

      if (p_419489_.compatibility == null) {
         p_419489_.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
      }

      if (p_419489_.regionSelectionPreference == null) {
         p_419489_.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
      }

      sortInvited(p_419489_);
      finalizeSlots(p_419489_);
   }

   private static void sortInvited(RealmsServer p_87505_) {
      p_87505_.players
         .sort(
            (p_454097_, p_454098_) -> ComparisonChain.start()
               .compareFalseFirst(p_454098_.accepted, p_454097_.accepted)
               .compare(p_454097_.name.toLowerCase(Locale.ROOT), p_454098_.name.toLowerCase(Locale.ROOT))
               .result()
         );
   }

   private static void finalizeSlots(RealmsServer p_419934_) {
      p_419934_.slotList.forEach(p_419400_ -> p_419934_.slots.put(p_419400_.slotId, p_419400_));

      for (int i = 1; i <= 3; i++) {
         if (!p_419934_.slots.containsKey(i)) {
            p_419934_.slots.put(i, RealmsSlot.defaults(i));
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
      boolean flag = !this.expired && this.state == RealmsServer.State.OPEN;
      return flag && (this.isCompatible() || this.needsUpgrade() || this.isSelfOwnedServer());
   }

   private boolean isSelfOwnedServer() {
      return Minecraft.getInstance().isLocalPlayer(this.ownerUUID);
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
         RealmsServer realmsserver = (RealmsServer)p_87528_;
         return new EqualsBuilder()
            .append(this.id, realmsserver.id)
            .append(this.name, realmsserver.name)
            .append(this.motd, realmsserver.motd)
            .append(this.state, realmsserver.state)
            .append(this.owner, realmsserver.owner)
            .append(this.expired, realmsserver.expired)
            .append(this.worldType, this.worldType)
            .isEquals();
      }
   }

   public RealmsServer copy() {
      RealmsServer realmsserver = new RealmsServer();
      realmsserver.id = this.id;
      realmsserver.remoteSubscriptionId = this.remoteSubscriptionId;
      realmsserver.name = this.name;
      realmsserver.motd = this.motd;
      realmsserver.state = this.state;
      realmsserver.owner = this.owner;
      realmsserver.players = this.players;
      realmsserver.slotList = this.slotList.stream().map(RealmsSlot::copy).toList();
      realmsserver.slots = this.cloneSlots(this.slots);
      realmsserver.expired = this.expired;
      realmsserver.expiredTrial = this.expiredTrial;
      realmsserver.daysLeft = this.daysLeft;
      realmsserver.worldType = this.worldType;
      realmsserver.isHardcore = this.isHardcore;
      realmsserver.gameMode = this.gameMode;
      realmsserver.ownerUUID = this.ownerUUID;
      realmsserver.minigameName = this.minigameName;
      realmsserver.activeSlot = this.activeSlot;
      realmsserver.minigameId = this.minigameId;
      realmsserver.minigameImage = this.minigameImage;
      realmsserver.parentWorldName = this.parentWorldName;
      realmsserver.parentRealmId = this.parentRealmId;
      realmsserver.activeVersion = this.activeVersion;
      realmsserver.compatibility = this.compatibility;
      realmsserver.regionSelectionPreference = this.regionSelectionPreference != null ? this.regionSelectionPreference.copy() : null;
      return realmsserver;
   }

   public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> p_87511_) {
      Map<Integer, RealmsSlot> map = Maps.newHashMap();

      for (Entry<Integer, RealmsSlot> entry : p_87511_.entrySet()) {
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

   public String getWorldName(int p_87496_) {
      return this.name == null
         ? this.slots.get(p_87496_).options.getSlotName(p_87496_)
         : this.name + " (" + this.slots.get(p_87496_).options.getSlotName(p_87496_) + ")";
   }

   public ServerData toServerData(String p_87523_) {
      return new ServerData(Objects.requireNonNullElse(this.name, "unknown server"), p_87523_, ServerData.Type.REALM);
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
            .compareTrueFirst(Objects.equals(p_87536_.owner, this.refOwner), Objects.equals(p_87537_.owner, this.refOwner))
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
      NORMAL("normal"),
      MINIGAME("minigame"),
      ADVENTUREMAP("adventureMap"),
      EXPERIENCE("experience"),
      INSPIRATION("inspiration"),
      UNKNOWN("unknown");

      private static final String TRANSLATION_PREFIX = "mco.backup.entry.worldType.";
      private final Component displayName;

      private WorldType(String p_458111_) {
         this.displayName = Component.translatable("mco.backup.entry.worldType." + p_458111_);
      }

      public Component getDisplayName() {
         return this.displayName;
      }
   }
}
