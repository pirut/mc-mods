package com.mojang.realmsclient.client;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.BackupList;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.dto.PendingInvitesList;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsDescriptionDto;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.RealmsWorldResetDto;
import com.mojang.realmsclient.dto.ServerActivityList;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsHttpException;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.WorldGenerationInfo;
import com.mojang.util.UndashedUuid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsClient {
   public static final RealmsClient.Environment ENVIRONMENT = Optional.ofNullable(System.getenv("realms.environment"))
      .or(() -> Optional.ofNullable(System.getProperty("realms.environment")))
      .flatMap(RealmsClient.Environment::byName)
      .orElse(RealmsClient.Environment.PRODUCTION);
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String sessionId;
   private final String username;
   private final Minecraft minecraft;
   private static final String WORLDS_RESOURCE_PATH = "worlds";
   private static final String INVITES_RESOURCE_PATH = "invites";
   private static final String MCO_RESOURCE_PATH = "mco";
   private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
   private static final String ACTIVITIES_RESOURCE = "activities";
   private static final String OPS_RESOURCE = "ops";
   private static final String REGIONS_RESOURCE = "regions/ping/stat";
   private static final String TRIALS_RESOURCE = "trial";
   private static final String NOTIFICATIONS_RESOURCE = "notifications";
   private static final String PATH_LIST_ALL_REALMS = "/listUserWorldsOfType/any";
   private static final String PATH_CREATE_SNAPSHOT_REALM = "/$PARENT_WORLD_ID/createPrereleaseRealm";
   private static final String PATH_SNAPSHOT_ELIGIBLE_REALMS = "/listPrereleaseEligibleWorlds";
   private static final String PATH_INITIALIZE = "/$WORLD_ID/initialize";
   private static final String PATH_GET_ACTIVTIES = "/$WORLD_ID";
   private static final String PATH_GET_LIVESTATS = "/liveplayerlist";
   private static final String PATH_GET_SUBSCRIPTION = "/$WORLD_ID";
   private static final String PATH_OP = "/$WORLD_ID/$PROFILE_UUID";
   private static final String PATH_PUT_INTO_MINIGAMES_MODE = "/minigames/$MINIGAME_ID/$WORLD_ID";
   private static final String PATH_AVAILABLE = "/available";
   private static final String PATH_TEMPLATES = "/templates/$WORLD_TYPE";
   private static final String PATH_WORLD_JOIN = "/v1/$ID/join/pc";
   private static final String PATH_WORLD_GET = "/$ID";
   private static final String PATH_WORLD_INVITES = "/$WORLD_ID";
   private static final String PATH_WORLD_UNINVITE = "/$WORLD_ID/invite/$UUID";
   private static final String PATH_PENDING_INVITES_COUNT = "/count/pending";
   private static final String PATH_PENDING_INVITES = "/pending";
   private static final String PATH_ACCEPT_INVITE = "/accept/$INVITATION_ID";
   private static final String PATH_REJECT_INVITE = "/reject/$INVITATION_ID";
   private static final String PATH_UNINVITE_MYSELF = "/$WORLD_ID";
   private static final String PATH_WORLD_UPDATE = "/$WORLD_ID";
   private static final String PATH_SLOT = "/$WORLD_ID/slot/$SLOT_ID";
   private static final String PATH_WORLD_OPEN = "/$WORLD_ID/open";
   private static final String PATH_WORLD_CLOSE = "/$WORLD_ID/close";
   private static final String PATH_WORLD_RESET = "/$WORLD_ID/reset";
   private static final String PATH_DELETE_WORLD = "/$WORLD_ID";
   private static final String PATH_WORLD_BACKUPS = "/$WORLD_ID/backups";
   private static final String PATH_WORLD_DOWNLOAD = "/$WORLD_ID/slot/$SLOT_ID/download";
   private static final String PATH_WORLD_UPLOAD = "/$WORLD_ID/backups/upload";
   private static final String PATH_CLIENT_COMPATIBLE = "/client/compatible";
   private static final String PATH_TOS_AGREED = "/tos/agreed";
   private static final String PATH_NEWS = "/v1/news";
   private static final String PATH_MARK_NOTIFICATIONS_SEEN = "/seen";
   private static final String PATH_DISMISS_NOTIFICATIONS = "/dismiss";
   private static final GuardedSerializer GSON = new GuardedSerializer();

   public static RealmsClient create() {
      Minecraft $$0 = Minecraft.getInstance();
      return create($$0);
   }

   public static RealmsClient create(Minecraft p_239152_) {
      String $$1 = p_239152_.getUser().getName();
      String $$2 = p_239152_.getUser().getSessionId();
      return new RealmsClient($$2, $$1, p_239152_);
   }

   public RealmsClient(String p_87166_, String p_87167_, Minecraft p_87168_) {
      this.sessionId = p_87166_;
      this.username = p_87167_;
      this.minecraft = p_87168_;
      RealmsClientConfig.setProxy(p_87168_.getProxy());
   }

   public RealmsServerList listRealms() throws RealmsServiceException {
      String $$0 = this.url("worlds");
      if (RealmsMainScreen.isSnapshot()) {
         $$0 = $$0 + "/listUserWorldsOfType/any";
      }

      String $$1 = this.execute(Request.get($$0));
      return RealmsServerList.parse($$1);
   }

   public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
      String $$0 = this.url("worlds/listPrereleaseEligibleWorlds");
      String $$1 = this.execute(Request.get($$0));
      return RealmsServerList.parse($$1).servers;
   }

   public RealmsServer createSnapshotRealm(Long p_306020_) throws RealmsServiceException {
      String $$1 = String.valueOf(p_306020_);
      String $$2 = this.url("worlds" + "/$PARENT_WORLD_ID/createPrereleaseRealm".replace("$PARENT_WORLD_ID", $$1));
      return RealmsServer.parse(this.execute(Request.post($$2, $$1)));
   }

   public List<RealmsNotification> getNotifications() throws RealmsServiceException {
      String $$0 = this.url("notifications");
      String $$1 = this.execute(Request.get($$0));
      return RealmsNotification.parseList($$1);
   }

   private static JsonArray uuidListToJsonArray(List<UUID> p_275393_) {
      JsonArray $$1 = new JsonArray();

      for (UUID $$2 : p_275393_) {
         if ($$2 != null) {
            $$1.add($$2.toString());
         }
      }

      return $$1;
   }

   public void notificationsSeen(List<UUID> p_275212_) throws RealmsServiceException {
      String $$1 = this.url("notifications/seen");
      this.execute(Request.post($$1, GSON.toJson(uuidListToJsonArray(p_275212_))));
   }

   public void notificationsDismiss(List<UUID> p_275407_) throws RealmsServiceException {
      String $$1 = this.url("notifications/dismiss");
      this.execute(Request.post($$1, GSON.toJson(uuidListToJsonArray(p_275407_))));
   }

   public RealmsServer getOwnRealm(long p_87175_) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$ID".replace("$ID", String.valueOf(p_87175_)));
      String $$2 = this.execute(Request.get($$1));
      return RealmsServer.parse($$2);
   }

   public ServerActivityList getActivity(long p_167279_) throws RealmsServiceException {
      String $$1 = this.url("activities" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_167279_)));
      String $$2 = this.execute(Request.get($$1));
      return ServerActivityList.parse($$2);
   }

   public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
      String $$0 = this.url("activities/liveplayerlist");
      String $$1 = this.execute(Request.get($$0));
      return RealmsServerPlayerLists.parse($$1);
   }

   public RealmsServerAddress join(long p_87208_) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/v1/$ID/join/pc".replace("$ID", p_87208_ + ""));
      String $$2 = this.execute(Request.get($$1, 5000, 30000));
      return RealmsServerAddress.parse($$2);
   }

   public void initializeRealm(long p_87192_, String p_87193_, String p_87194_) throws RealmsServiceException {
      RealmsDescriptionDto $$3 = new RealmsDescriptionDto(p_87193_, p_87194_);
      String $$4 = this.url("worlds" + "/$WORLD_ID/initialize".replace("$WORLD_ID", String.valueOf(p_87192_)));
      String $$5 = GSON.toJson($$3);
      this.execute(Request.post($$4, $$5, 5000, 10000));
   }

   public boolean hasParentalConsent() throws RealmsServiceException {
      String $$0 = this.url("mco/available");
      String $$1 = this.execute(Request.get($$0));
      return Boolean.parseBoolean($$1);
   }

   public RealmsClient.CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
      String $$0 = this.url("mco/client/compatible");
      String $$1 = this.execute(Request.get($$0));

      try {
         return RealmsClient.CompatibleVersionResponse.valueOf($$1);
      } catch (IllegalArgumentException var5) {
         throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse($$1));
      }
   }

   public void uninvite(long p_87184_, UUID p_295611_) throws RealmsServiceException {
      String $$2 = this.url(
         "invites" + "/$WORLD_ID/invite/$UUID".replace("$WORLD_ID", String.valueOf(p_87184_)).replace("$UUID", UndashedUuid.toString(p_295611_))
      );
      this.execute(Request.delete($$2));
   }

   public void uninviteMyselfFrom(long p_87223_) throws RealmsServiceException {
      String $$1 = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87223_)));
      this.execute(Request.delete($$1));
   }

   public RealmsServer invite(long p_87213_, String p_87214_) throws RealmsServiceException {
      PlayerInfo $$2 = new PlayerInfo();
      $$2.setName(p_87214_);
      String $$3 = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87213_)));
      String $$4 = this.execute(Request.post($$3, GSON.toJson($$2)));
      return RealmsServer.parse($$4);
   }

   public BackupList backupsFor(long p_87231_) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(p_87231_)));
      String $$2 = this.execute(Request.get($$1));
      return BackupList.parse($$2);
   }

   public void update(long p_87216_, String p_87217_, String p_87218_) throws RealmsServiceException {
      RealmsDescriptionDto $$3 = new RealmsDescriptionDto(p_87217_, p_87218_);
      String $$4 = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87216_)));
      this.execute(Request.post($$4, GSON.toJson($$3)));
   }

   public void updateSlot(long p_87180_, int p_87181_, RealmsWorldOptions p_87182_) throws RealmsServiceException {
      String $$3 = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(p_87180_)).replace("$SLOT_ID", String.valueOf(p_87181_)));
      String $$4 = p_87182_.toJson();
      this.execute(Request.post($$3, $$4));
   }

   public boolean switchSlot(long p_87177_, int p_87178_) throws RealmsServiceException {
      String $$2 = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(p_87177_)).replace("$SLOT_ID", String.valueOf(p_87178_)));
      String $$3 = this.execute(Request.put($$2, ""));
      return Boolean.valueOf($$3);
   }

   public void restoreWorld(long p_87225_, String p_87226_) throws RealmsServiceException {
      String $$2 = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(p_87225_)), "backupId=" + p_87226_);
      this.execute(Request.put($$2, "", 40000, 600000));
   }

   public WorldTemplatePaginatedList fetchWorldTemplates(int p_87171_, int p_87172_, RealmsServer.WorldType p_87173_) throws RealmsServiceException {
      String $$3 = this.url(
         "worlds" + "/templates/$WORLD_TYPE".replace("$WORLD_TYPE", p_87173_.toString()), String.format(Locale.ROOT, "page=%d&pageSize=%d", p_87171_, p_87172_)
      );
      String $$4 = this.execute(Request.get($$3));
      return WorldTemplatePaginatedList.parse($$4);
   }

   public Boolean putIntoMinigameMode(long p_87233_, String p_87234_) throws RealmsServiceException {
      String $$2 = "/minigames/$MINIGAME_ID/$WORLD_ID".replace("$MINIGAME_ID", p_87234_).replace("$WORLD_ID", String.valueOf(p_87233_));
      String $$3 = this.url("worlds" + $$2);
      return Boolean.valueOf(this.execute(Request.put($$3, "")));
   }

   public Ops op(long p_87239_, UUID p_295948_) throws RealmsServiceException {
      String $$2 = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(p_87239_)).replace("$PROFILE_UUID", UndashedUuid.toString(p_295948_));
      String $$3 = this.url("ops" + $$2);
      return Ops.parse(this.execute(Request.post($$3, "")));
   }

   public Ops deop(long p_87245_, UUID p_294880_) throws RealmsServiceException {
      String $$2 = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(p_87245_)).replace("$PROFILE_UUID", UndashedUuid.toString(p_294880_));
      String $$3 = this.url("ops" + $$2);
      return Ops.parse(this.execute(Request.delete($$3)));
   }

   public Boolean open(long p_87237_) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID/open".replace("$WORLD_ID", String.valueOf(p_87237_)));
      String $$2 = this.execute(Request.put($$1, ""));
      return Boolean.valueOf($$2);
   }

   public Boolean close(long p_87243_) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID/close".replace("$WORLD_ID", String.valueOf(p_87243_)));
      String $$2 = this.execute(Request.put($$1, ""));
      return Boolean.valueOf($$2);
   }

   public Boolean resetWorldWithSeed(long p_167276_, WorldGenerationInfo p_167277_) throws RealmsServiceException {
      RealmsWorldResetDto $$2 = new RealmsWorldResetDto(
         p_167277_.seed(), -1L, p_167277_.levelType().getDtoIndex(), p_167277_.generateStructures(), p_167277_.experiments()
      );
      String $$3 = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(p_167276_)));
      String $$4 = this.execute(Request.post($$3, GSON.toJson($$2), 30000, 80000));
      return Boolean.valueOf($$4);
   }

   public Boolean resetWorldWithTemplate(long p_87251_, String p_87252_) throws RealmsServiceException {
      RealmsWorldResetDto $$2 = new RealmsWorldResetDto(null, Long.valueOf(p_87252_), -1, false, Set.of());
      String $$3 = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(p_87251_)));
      String $$4 = this.execute(Request.post($$3, GSON.toJson($$2), 30000, 80000));
      return Boolean.valueOf($$4);
   }

   public Subscription subscriptionFor(long p_87249_) throws RealmsServiceException {
      String $$1 = this.url("subscriptions" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87249_)));
      String $$2 = this.execute(Request.get($$1));
      return Subscription.parse($$2);
   }

   public int pendingInvitesCount() throws RealmsServiceException {
      return this.pendingInvites().pendingInvites.size();
   }

   public PendingInvitesList pendingInvites() throws RealmsServiceException {
      String $$0 = this.url("invites/pending");
      String $$1 = this.execute(Request.get($$0));
      PendingInvitesList $$2 = PendingInvitesList.parse($$1);
      $$2.pendingInvites.removeIf(this::isBlocked);
      return $$2;
   }

   private boolean isBlocked(PendingInvite p_87198_) {
      return this.minecraft.getPlayerSocialManager().isBlocked(p_87198_.realmOwnerUuid);
   }

   public void acceptInvitation(String p_87202_) throws RealmsServiceException {
      String $$1 = this.url("invites" + "/accept/$INVITATION_ID".replace("$INVITATION_ID", p_87202_));
      this.execute(Request.put($$1, ""));
   }

   public WorldDownload requestDownloadInfo(long p_87210_, int p_87211_) throws RealmsServiceException {
      String $$2 = this.url(
         "worlds" + "/$WORLD_ID/slot/$SLOT_ID/download".replace("$WORLD_ID", String.valueOf(p_87210_)).replace("$SLOT_ID", String.valueOf(p_87211_))
      );
      String $$3 = this.execute(Request.get($$2));
      return WorldDownload.parse($$3);
   }

   @Nullable
   public UploadInfo requestUploadInfo(long p_87257_, @Nullable String p_87258_) throws RealmsServiceException {
      String $$2 = this.url("worlds" + "/$WORLD_ID/backups/upload".replace("$WORLD_ID", String.valueOf(p_87257_)));
      return UploadInfo.parse(this.execute(Request.put($$2, UploadInfo.createRequest(p_87258_))));
   }

   public void rejectInvitation(String p_87220_) throws RealmsServiceException {
      String $$1 = this.url("invites" + "/reject/$INVITATION_ID".replace("$INVITATION_ID", p_87220_));
      this.execute(Request.put($$1, ""));
   }

   public void agreeToTos() throws RealmsServiceException {
      String $$0 = this.url("mco/tos/agreed");
      this.execute(Request.post($$0, ""));
   }

   public RealmsNews getNews() throws RealmsServiceException {
      String $$0 = this.url("mco/v1/news");
      String $$1 = this.execute(Request.get($$0, 5000, 10000));
      return RealmsNews.parse($$1);
   }

   public void sendPingResults(PingResult p_87200_) throws RealmsServiceException {
      String $$1 = this.url("regions/ping/stat");
      this.execute(Request.post($$1, GSON.toJson(p_87200_)));
   }

   public Boolean trialAvailable() throws RealmsServiceException {
      String $$0 = this.url("trial");
      String $$1 = this.execute(Request.get($$0));
      return Boolean.valueOf($$1);
   }

   public void deleteRealm(long p_87255_) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(p_87255_)));
      this.execute(Request.delete($$1));
   }

   private String url(String p_87228_) {
      return this.url(p_87228_, null);
   }

   private String url(String p_87204_, @Nullable String p_87205_) {
      try {
         return new URI(ENVIRONMENT.protocol, ENVIRONMENT.baseUrl, "/" + p_87204_, p_87205_, null).toASCIIString();
      } catch (URISyntaxException var4) {
         throw new IllegalArgumentException(p_87204_, var4);
      }
   }

   private String execute(Request<?> p_87196_) throws RealmsServiceException {
      p_87196_.cookie("sid", this.sessionId);
      p_87196_.cookie("user", this.username);
      p_87196_.cookie("version", SharedConstants.getCurrentVersion().getName());
      p_87196_.addSnapshotHeader(RealmsMainScreen.isSnapshot());

      try {
         int $$1 = p_87196_.responseCode();
         if ($$1 != 503 && $$1 != 277) {
            String $$3 = p_87196_.text();
            if ($$1 >= 200 && $$1 < 300) {
               return $$3;
            } else if ($$1 == 401) {
               String $$4 = p_87196_.getHeader("WWW-Authenticate");
               LOGGER.info("Could not authorize you against Realms server: {}", $$4);
               throw new RealmsServiceException(new RealmsError.AuthenticationError($$4));
            } else {
               RealmsError $$5 = RealmsError.parse($$1, $$3);
               throw new RealmsServiceException($$5);
            }
         } else {
            int $$2 = p_87196_.getRetryAfterHeader();
            throw new RetryCallException($$2, $$1);
         }
      } catch (RealmsHttpException var5) {
         throw new RealmsServiceException(RealmsError.CustomError.connectivityError(var5));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum CompatibleVersionResponse {
      COMPATIBLE,
      OUTDATED,
      OTHER;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Environment {
      PRODUCTION("pc.realms.minecraft.net", "https"),
      STAGE("pc-stage.realms.minecraft.net", "https"),
      LOCAL("localhost:8080", "http");

      public final String baseUrl;
      public final String protocol;

      private Environment(String p_87286_, String p_87287_) {
         this.baseUrl = p_87286_;
         this.protocol = p_87287_;
      }

      public static Optional<RealmsClient.Environment> byName(String p_289688_) {
         String var1 = p_289688_.toLowerCase(Locale.ROOT);

         return switch (var1) {
            case "production" -> Optional.of(PRODUCTION);
            case "local" -> Optional.of(LOCAL);
            case "stage", "staging" -> Optional.of(STAGE);
            default -> Optional.empty();
         };
      }
   }
}
