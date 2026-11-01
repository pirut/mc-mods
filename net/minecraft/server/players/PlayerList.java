package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.FileUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IPlayerListExtension;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class PlayerList implements IPlayerListExtension {
   public static final File USERBANLIST_FILE = new File("banned-players.json");
   public static final File IPBANLIST_FILE = new File("banned-ips.json");
   public static final File OPLIST_FILE = new File("ops.json");
   public static final File WHITELIST_FILE = new File("whitelist.json");
   public static final Component CHAT_FILTERED_FULL = Component.translatable("chat.filtered_full");
   public static final Component DUPLICATE_LOGIN_DISCONNECT_MESSAGE = Component.translatable("multiplayer.disconnect.duplicate_login");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SEND_PLAYER_INFO_INTERVAL = 600;
   private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z", Locale.ROOT);
   private final MinecraftServer server;
   private final List<ServerPlayer> players = Lists.newArrayList();
   private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
   private final UserBanList bans;
   private final IpBanList ipBans;
   private final ServerOpList ops;
   private final UserWhiteList whitelist;
   private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
   private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
   private final PlayerDataStorage playerIo;
   private final LayeredRegistryAccess<RegistryLayer> registries;
   private int viewDistance;
   private int simulationDistance;
   private boolean allowCommandsForAllPlayers;
   private int sendAllPlayerInfoIn;
   private final List<ServerPlayer> playersView = Collections.unmodifiableList(this.players);

   public PlayerList(MinecraftServer p_203842_, LayeredRegistryAccess<RegistryLayer> p_251844_, PlayerDataStorage p_203844_, NotificationService p_443409_) {
      this.server = p_203842_;
      this.registries = p_251844_;
      this.playerIo = p_203844_;
      this.whitelist = new UserWhiteList(WHITELIST_FILE, p_443409_);
      this.ops = new ServerOpList(OPLIST_FILE, p_443409_);
      this.bans = new UserBanList(USERBANLIST_FILE, p_443409_);
      this.ipBans = new IpBanList(IPBANLIST_FILE, p_443409_);
   }

   public void placeNewPlayer(Connection p_11262_, ServerPlayer p_11263_, CommonListenerCookie p_301988_) {
      NameAndId nameandid = p_11263_.nameAndId();
      UserNameToIdResolver usernametoidresolver = this.server.services().nameToIdCache();
      Optional<NameAndId> optional = usernametoidresolver.get(nameandid.id());
      String s = optional.map(NameAndId::name).orElse(nameandid.name());
      usernametoidresolver.add(nameandid);
      ServerLevel serverlevel = p_11263_.level();
      String s1 = p_11262_.getLoggableAddress(this.server.logIPs());
      LOGGER.info(
         "{}[{}] logged in with entity id {} at ({}, {}, {})",
         new Object[]{p_11263_.getPlainTextName(), s1, p_11263_.getId(), p_11263_.getX(), p_11263_.getY(), p_11263_.getZ()}
      );
      LevelData leveldata = serverlevel.getLevelData();
      ServerGamePacketListenerImpl servergamepacketlistenerimpl = new ServerGamePacketListenerImpl(this.server, p_11262_, p_11263_, p_301988_);
      p_11262_.setupInboundProtocol(
         GameProtocols.SERVERBOUND_TEMPLATE
            .bind(
               RegistryFriendlyByteBuf.decorator(this.server.registryAccess(), servergamepacketlistenerimpl.getConnectionType()), servergamepacketlistenerimpl
            ),
         servergamepacketlistenerimpl
      );
      servergamepacketlistenerimpl.suspendFlushing();
      GameRules gamerules = serverlevel.getGameRules();
      boolean flag = gamerules.get(GameRules.IMMEDIATE_RESPAWN);
      boolean flag1 = gamerules.get(GameRules.REDUCED_DEBUG_INFO);
      boolean flag2 = gamerules.get(GameRules.LIMITED_CRAFTING);
      servergamepacketlistenerimpl.send(
         new ClientboundLoginPacket(
            p_11263_.getId(),
            leveldata.isHardcore(),
            this.server.levelKeys(),
            this.getMaxPlayers(),
            this.getViewDistance(),
            this.getSimulationDistance(),
            flag1,
            !flag,
            flag2,
            p_11263_.createCommonSpawnInfo(serverlevel),
            this.server.enforceSecureProfile()
         )
      );
      servergamepacketlistenerimpl.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
      servergamepacketlistenerimpl.send(new ClientboundPlayerAbilitiesPacket(p_11263_.getAbilities()));
      servergamepacketlistenerimpl.send(new ClientboundSetHeldSlotPacket(p_11263_.getInventory().getSelectedSlot()));
      OnDatapackSyncEvent dataSyncEvent = (OnDatapackSyncEvent)NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(this, p_11263_));
      RecipeManager recipemanager = this.server.getRecipeManager();
      servergamepacketlistenerimpl.send(
         new ClientboundUpdateRecipesPacket(recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes())
      );
      CommonHooks.sendRecipes(p_11263_, dataSyncEvent.getRecipeTypesToSend(), recipemanager.recipeMap());
      this.sendPlayerPermissionLevel(p_11263_);
      p_11263_.getStats().markAllDirty();
      p_11263_.getRecipeBook().sendInitialRecipeBook(p_11263_);
      this.updateEntireScoreboard(serverlevel.getScoreboard(), p_11263_);
      this.server.invalidateStatus();
      MutableComponent mutablecomponent;
      if (p_11263_.getGameProfile().name().equalsIgnoreCase(s)) {
         mutablecomponent = Component.translatable("multiplayer.player.joined", p_11263_.getDisplayName());
      } else {
         mutablecomponent = Component.translatable("multiplayer.player.joined.renamed", p_11263_.getDisplayName(), s);
      }

      this.broadcastSystemMessage(mutablecomponent.withStyle(ChatFormatting.YELLOW), false);
      servergamepacketlistenerimpl.teleport(p_11263_.getX(), p_11263_.getY(), p_11263_.getZ(), p_11263_.getYRot(), p_11263_.getXRot());
      ServerStatus serverstatus = this.server.getStatus();
      if (serverstatus != null && !p_301988_.transferred()) {
         p_11263_.sendServerStatus(serverstatus);
      }

      p_11263_.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
      this.players.add(p_11263_);
      this.playersByUUID.put(p_11263_.getUUID(), p_11263_);
      this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p_11263_)));
      this.sendLevelInfo(p_11263_, serverlevel);
      serverlevel.addNewPlayer(p_11263_);
      this.server.getCustomBossEvents().onPlayerConnect(p_11263_);
      this.sendActivePlayerEffects(p_11263_);
      p_11263_.initInventoryMenu();
      this.server.notificationManager().playerJoined(p_11263_);
      servergamepacketlistenerimpl.resumeFlushing();
      AttachmentSync.syncInitialPlayerAttachments(p_11263_);
      EventHooks.firePlayerLoggedIn(p_11263_);
   }

   protected void updateEntireScoreboard(ServerScoreboard p_11274_, ServerPlayer p_11275_) {
      Set<Objective> set = Sets.newHashSet();

      for (PlayerTeam playerteam : p_11274_.getPlayerTeams()) {
         p_11275_.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerteam, true));
      }

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         Objective objective = p_11274_.getDisplayObjective(displayslot);
         if (objective != null && !set.contains(objective)) {
            for (Packet<?> packet : p_11274_.getStartTrackingPackets(objective)) {
               p_11275_.connection.send(packet);
            }

            set.add(objective);
         }
      }
   }

   public void addWorldborderListener(final ServerLevel p_184210_) {
      p_184210_.getWorldBorder().addListener(new BorderChangeListener() {
         @Override
         public void onSetSize(WorldBorder p_446639_, double p_446495_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(p_446639_), p_184210_.dimension());
         }

         @Override
         public void onLerpSize(WorldBorder p_446360_, double p_445647_, double p_445492_, long p_446273_, long p_458983_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(p_446360_), p_184210_.dimension());
         }

         @Override
         public void onSetCenter(WorldBorder p_445389_, double p_446055_, double p_447368_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(p_445389_), p_184210_.dimension());
         }

         @Override
         public void onSetWarningTime(WorldBorder p_445713_, int p_446020_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(p_445713_), p_184210_.dimension());
         }

         @Override
         public void onSetWarningBlocks(WorldBorder p_447253_, int p_447297_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(p_447253_), p_184210_.dimension());
         }

         @Override
         public void onSetDamagePerBlock(WorldBorder p_446881_, double p_445420_) {
         }

         @Override
         public void onSetSafeZone(WorldBorder p_446156_, double p_446480_) {
         }
      });
   }

   public Optional<CompoundTag> loadPlayerData(NameAndId p_433478_) {
      CompoundTag compoundtag = this.server.getWorldData().getLoadedPlayerTag();
      if (this.server.isSingleplayerOwner(p_433478_) && compoundtag != null) {
         LOGGER.debug("loading single player");
         return Optional.of(compoundtag);
      } else {
         return this.playerIo.load(p_433478_);
      }
   }

   protected void save(ServerPlayer p_11277_) {
      if (p_11277_.connection != null) {
         this.playerIo.save(p_11277_);
         ServerStatsCounter serverstatscounter = this.stats.get(p_11277_.getUUID());
         if (serverstatscounter != null) {
            serverstatscounter.save();
         }

         PlayerAdvancements playeradvancements = this.advancements.get(p_11277_.getUUID());
         if (playeradvancements != null) {
            playeradvancements.save();
         }
      }
   }

   public void remove(ServerPlayer p_11287_) {
      EventHooks.firePlayerLoggedOut(p_11287_);
      ServerLevel serverlevel = p_11287_.level();
      p_11287_.awardStat(Stats.LEAVE_GAME);
      this.save(p_11287_);
      if (p_11287_.isPassenger()) {
         Entity entity = p_11287_.getRootVehicle();
         if (entity.hasExactlyOnePlayerPassenger()) {
            LOGGER.debug("Removing player mount");
            p_11287_.stopRiding();
            entity.getPassengersAndSelf().forEach(p_215620_ -> p_215620_.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
         }
      }

      p_11287_.unRide();

      for (ThrownEnderpearl thrownenderpearl : p_11287_.getEnderPearls()) {
         thrownenderpearl.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
      }

      serverlevel.removePlayerImmediately(p_11287_, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
      p_11287_.getAdvancements().stopListening();
      this.players.remove(p_11287_);
      this.server.getCustomBossEvents().onPlayerDisconnect(p_11287_);
      UUID uuid = p_11287_.getUUID();
      ServerPlayer serverplayer = this.playersByUUID.get(uuid);
      if (serverplayer == p_11287_) {
         this.playersByUUID.remove(uuid);
         this.stats.remove(uuid);
         this.advancements.remove(uuid);
         this.server.notificationManager().playerLeft(p_11287_);
      }

      this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(p_11287_.getUUID())));
   }

   @Nullable
   public Component canPlayerLogin(SocketAddress p_11257_, NameAndId p_432865_) {
      if (this.bans.isBanned(p_432865_)) {
         UserBanListEntry userbanlistentry = this.bans.get(p_432865_);
         MutableComponent mutablecomponent1 = Component.translatable("multiplayer.disconnect.banned.reason", userbanlistentry.getReasonMessage());
         if (userbanlistentry.getExpires() != null) {
            mutablecomponent1.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(userbanlistentry.getExpires())));
         }

         return mutablecomponent1;
      } else if (!this.isWhiteListed(p_432865_)) {
         return Component.translatable("multiplayer.disconnect.not_whitelisted");
      } else if (this.ipBans.isBanned(p_11257_)) {
         IpBanListEntry ipbanlistentry = this.ipBans.get(p_11257_);
         MutableComponent mutablecomponent = Component.translatable("multiplayer.disconnect.banned_ip.reason", ipbanlistentry.getReasonMessage());
         if (ipbanlistentry.getExpires() != null) {
            mutablecomponent.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipbanlistentry.getExpires())));
         }

         return mutablecomponent;
      } else {
         return this.players.size() >= this.getMaxPlayers() && !this.canBypassPlayerLimit(p_432865_)
            ? Component.translatable("multiplayer.disconnect.server_full")
            : null;
      }
   }

   public boolean disconnectAllPlayersWithProfile(UUID p_434192_) {
      Set<ServerPlayer> set = Sets.newIdentityHashSet();

      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.getUUID().equals(p_434192_)) {
            set.add(serverplayer);
         }
      }

      ServerPlayer serverplayer2 = this.playersByUUID.get(p_434192_);
      if (serverplayer2 != null) {
         set.add(serverplayer2);
      }

      for (ServerPlayer serverplayer1 : set) {
         serverplayer1.connection.disconnect(DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
      }

      return !set.isEmpty();
   }

   public ServerPlayer respawn(ServerPlayer p_11237_, boolean p_11238_, Entity.RemovalReason p_348558_) {
      TeleportTransition teleporttransition = p_11237_.findRespawnPositionAndUseSpawnBlock(!p_11238_, TeleportTransition.DO_NOTHING);
      this.players.remove(p_11237_);
      p_11237_.level().removePlayerImmediately(p_11237_, p_348558_);
      PlayerRespawnPositionEvent event = EventHooks.firePlayerRespawnPositionEvent(p_11237_, teleporttransition, p_11238_);
      teleporttransition = event.getTeleportTransition();
      ServerLevel serverlevel = teleporttransition.newLevel();
      ServerPlayer serverplayer = new ServerPlayer(this.server, serverlevel, p_11237_.getGameProfile(), p_11237_.clientInformation());
      serverplayer.connection = p_11237_.connection;
      serverplayer.restoreFrom(p_11237_, p_11238_);
      serverplayer.setId(p_11237_.getId());
      serverplayer.setMainArm(p_11237_.getMainArm());
      if (event.copyOriginalSpawnPosition()) {
         serverplayer.copyRespawnPosition(p_11237_);
      }

      for (String s : p_11237_.getTags()) {
         serverplayer.addTag(s);
      }

      Vec3 vec3 = teleporttransition.position();
      serverplayer.snapTo(vec3.x, vec3.y, vec3.z, teleporttransition.yRot(), teleporttransition.xRot());
      if (teleporttransition.missingRespawnBlock()) {
         serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
      }

      byte b0 = (byte)(p_11238_ ? 1 : 0);
      ServerLevel serverlevel1 = serverplayer.level();
      LevelData leveldata = serverlevel1.getLevelData();
      serverplayer.connection.send(new ClientboundRespawnPacket(serverplayer.createCommonSpawnInfo(serverlevel1), b0));
      serverplayer.connection.teleport(serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), serverplayer.getYRot(), serverplayer.getXRot());
      serverplayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverlevel.getRespawnData()));
      serverplayer.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
      serverplayer.connection
         .send(new ClientboundSetExperiencePacket(serverplayer.experienceProgress, serverplayer.totalExperience, serverplayer.experienceLevel));
      this.sendActivePlayerEffects(serverplayer);
      this.sendLevelInfo(serverplayer, serverlevel);
      this.sendPlayerPermissionLevel(serverplayer);
      serverlevel.addRespawnedPlayer(serverplayer);
      this.players.add(serverplayer);
      this.playersByUUID.put(serverplayer.getUUID(), serverplayer);
      serverplayer.initInventoryMenu();
      serverplayer.setHealth(serverplayer.getHealth());
      AttachmentSync.syncInitialPlayerAttachments(serverplayer);
      EventHooks.firePlayerRespawnEvent(serverplayer, p_11238_);
      ServerPlayer.RespawnConfig serverplayer$respawnconfig = serverplayer.getRespawnConfig();
      if (!p_11238_ && serverplayer$respawnconfig != null) {
         LevelData.RespawnData leveldata$respawndata = serverplayer$respawnconfig.respawnData();
         ServerLevel serverlevel2 = this.server.getLevel(leveldata$respawndata.dimension());
         if (serverlevel2 != null) {
            BlockPos blockpos = leveldata$respawndata.pos();
            BlockState blockstate = serverlevel2.getBlockState(blockpos);
            if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
               serverplayer.connection
                  .send(
                     new ClientboundSoundPacket(
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                        SoundSource.BLOCKS,
                        blockpos.getX(),
                        blockpos.getY(),
                        blockpos.getZ(),
                        1.0F,
                        1.0F,
                        serverlevel.getRandom().nextLong()
                     )
                  );
            }
         }
      }

      return serverplayer;
   }

   public void sendActivePlayerEffects(ServerPlayer p_348494_) {
      this.sendActiveEffects(p_348494_, p_348494_.connection);
   }

   public void sendActiveEffects(LivingEntity p_348624_, ServerGamePacketListenerImpl p_348496_) {
      for (MobEffectInstance mobeffectinstance : p_348624_.getActiveEffects()) {
         p_348496_.send(new ClientboundUpdateMobEffectPacket(p_348624_.getId(), mobeffectinstance, false));
      }
   }

   public void sendPlayerPermissionLevel(ServerPlayer p_11290_) {
      LevelBasedPermissionSet levelbasedpermissionset = this.server.getProfilePermissions(p_11290_.nameAndId());
      this.sendPlayerPermissionLevel(p_11290_, levelbasedpermissionset);
   }

   public void tick() {
      if (++this.sendAllPlayerInfoIn > 600) {
         this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
         this.sendAllPlayerInfoIn = 0;
      }
   }

   public void broadcastAll(Packet<?> p_11269_) {
      for (ServerPlayer serverplayer : this.players) {
         serverplayer.connection.send(p_11269_);
      }
   }

   public void broadcastAll(Packet<?> p_11271_, ResourceKey<Level> p_11272_) {
      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.level().dimension() == p_11272_) {
            serverplayer.connection.send(p_11271_);
         }
      }
   }

   public void broadcastSystemToTeam(Player p_215622_, Component p_215623_) {
      Team team = p_215622_.getTeam();
      if (team != null) {
         for (String s : team.getPlayers()) {
            ServerPlayer serverplayer = this.getPlayerByName(s);
            if (serverplayer != null && serverplayer != p_215622_) {
               serverplayer.sendSystemMessage(p_215623_);
            }
         }
      }
   }

   public void broadcastSystemToAllExceptTeam(Player p_215650_, Component p_215651_) {
      Team team = p_215650_.getTeam();
      if (team == null) {
         this.broadcastSystemMessage(p_215651_, false);
      } else {
         for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer serverplayer = this.players.get(i);
            if (serverplayer.getTeam() != team) {
               serverplayer.sendSystemMessage(p_215651_);
            }
         }
      }
   }

   public String[] getPlayerNamesArray() {
      String[] astring = new String[this.players.size()];

      for (int i = 0; i < this.players.size(); i++) {
         astring[i] = this.players.get(i).getGameProfile().name();
      }

      return astring;
   }

   public UserBanList getBans() {
      return this.bans;
   }

   public IpBanList getIpBans() {
      return this.ipBans;
   }

   public void op(NameAndId p_435723_) {
      this.op(p_435723_, Optional.empty(), Optional.empty());
   }

   public void op(NameAndId p_442929_, Optional<LevelBasedPermissionSet> p_443490_, Optional<Boolean> p_443080_) {
      if (!EventHooks.onPermissionChanged(p_442929_, this.server.operatorUserPermissions(), this)) {
         this.ops
            .add(
               new ServerOpListEntry(
                  p_442929_, p_443490_.orElse(this.server.operatorUserPermissions()), p_443080_.orElse(this.ops.canBypassPlayerLimit(p_442929_))
               )
            );
         ServerPlayer serverplayer = this.getPlayer(p_442929_.id());
         if (serverplayer != null) {
            this.sendPlayerPermissionLevel(serverplayer);
         }
      }
   }

   public void deop(NameAndId p_435771_) {
      if (!EventHooks.onPermissionChanged(p_435771_, LevelBasedPermissionSet.ALL, this)) {
         if (this.ops.remove(p_435771_)) {
            ServerPlayer serverplayer = this.getPlayer(p_435771_.id());
            if (serverplayer != null) {
               this.sendPlayerPermissionLevel(serverplayer);
            }
         }
      }
   }

   private void sendPlayerPermissionLevel(ServerPlayer p_11227_, LevelBasedPermissionSet p_455779_) {
      if (p_11227_.connection != null) {
         byte b0 = switch (p_455779_.level()) {
            case ALL -> 24;
            case MODERATORS -> 25;
            case GAMEMASTERS -> 26;
            case ADMINS -> 27;
            case OWNERS -> 28;
         };
         p_11227_.connection.send(new ClientboundEntityEventPacket(p_11227_, b0));
      }

      this.server.getCommands().sendCommands(p_11227_);
   }

   public boolean isWhiteListed(NameAndId p_434490_) {
      return !this.isUsingWhitelist() || this.ops.contains(p_434490_) || this.whitelist.contains(p_434490_);
   }

   public boolean isOp(NameAndId p_435357_) {
      return this.ops.contains(p_435357_)
         || this.server.isSingleplayerOwner(p_435357_) && this.server.getWorldData().isAllowCommands()
         || this.allowCommandsForAllPlayers;
   }

   @Nullable
   public ServerPlayer getPlayerByName(String p_11256_) {
      int i = this.players.size();

      for (int j = 0; j < i; j++) {
         ServerPlayer serverplayer = this.players.get(j);
         if (serverplayer.getGameProfile().name().equalsIgnoreCase(p_11256_)) {
            return serverplayer;
         }
      }

      return null;
   }

   public void broadcast(
      @Nullable Player p_11242_, double p_11243_, double p_11244_, double p_11245_, double p_11246_, ResourceKey<Level> p_11247_, Packet<?> p_11248_
   ) {
      for (int i = 0; i < this.players.size(); i++) {
         ServerPlayer serverplayer = this.players.get(i);
         if (serverplayer != p_11242_ && serverplayer.level().dimension() == p_11247_) {
            double d0 = p_11243_ - serverplayer.getX();
            double d1 = p_11244_ - serverplayer.getY();
            double d2 = p_11245_ - serverplayer.getZ();
            if (d0 * d0 + d1 * d1 + d2 * d2 < p_11246_ * p_11246_) {
               serverplayer.connection.send(p_11248_);
            }
         }
      }
   }

   public void saveAll() {
      for (int i = 0; i < this.players.size(); i++) {
         this.save(this.players.get(i));
      }
   }

   public UserWhiteList getWhiteList() {
      return this.whitelist;
   }

   public String[] getWhiteListNames() {
      return this.whitelist.getUserList();
   }

   public ServerOpList getOps() {
      return this.ops;
   }

   public String[] getOpNames() {
      return this.ops.getUserList();
   }

   public void reloadWhiteList() {
   }

   public void sendLevelInfo(ServerPlayer p_11230_, ServerLevel p_11231_) {
      WorldBorder worldborder = p_11231_.getWorldBorder();
      p_11230_.connection.send(new ClientboundInitializeBorderPacket(worldborder));
      if (p_11230_.connection.hasChannel(ClientboundCustomSetTimePayload.TYPE)) {
         p_11230_.connection
            .send(
               new ClientboundCustomSetTimePayload(
                  p_11231_.getGameTime(),
                  p_11231_.getDayTime(),
                  p_11231_.getGameRules().get(GameRules.ADVANCE_TIME),
                  p_11231_.getDayTimeFraction(),
                  p_11231_.getDayTimePerTick()
               )
            );
      } else {
         p_11230_.connection
            .send(new ClientboundSetTimePacket(p_11231_.getGameTime(), p_11231_.getDayTime(), p_11231_.getGameRules().get(GameRules.ADVANCE_TIME)));
      }

      p_11230_.connection.send(new ClientboundSetDefaultSpawnPositionPacket(p_11231_.getRespawnData()));
      if (p_11231_.isRaining()) {
         p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
         p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, p_11231_.getRainLevel(1.0F)));
         p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, p_11231_.getThunderLevel(1.0F)));
      }

      p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0F));
      this.server.tickRateManager().updateJoiningPlayer(p_11230_);
      AttachmentSync.syncInitialLevelAttachments(p_11231_, p_11230_);
   }

   public void sendAllPlayerInfo(ServerPlayer p_11293_) {
      p_11293_.inventoryMenu.sendAllDataToRemote();
      p_11293_.resetSentInfo();
      p_11293_.connection.send(new ClientboundSetHeldSlotPacket(p_11293_.getInventory().getSelectedSlot()));
   }

   public int getPlayerCount() {
      return this.players.size();
   }

   public int getMaxPlayers() {
      return this.server.getMaxPlayers();
   }

   public boolean isUsingWhitelist() {
      return this.server.isUsingWhitelist();
   }

   public List<ServerPlayer> getPlayersWithAddress(String p_11283_) {
      List<ServerPlayer> list = Lists.newArrayList();

      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.getIpAddress().equals(p_11283_)) {
            list.add(serverplayer);
         }
      }

      return list;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public int getSimulationDistance() {
      return this.simulationDistance;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   @Nullable
   public CompoundTag getSingleplayerData() {
      return null;
   }

   public void setAllowCommandsForAllPlayers(boolean p_321498_) {
      this.allowCommandsForAllPlayers = p_321498_;
   }

   public void removeAll() {
      for (int i = 0; i < this.players.size(); i++) {
         this.players.get(i).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
      }
   }

   public void broadcastSystemMessage(Component p_240618_, boolean p_240644_) {
      this.broadcastSystemMessage(p_240618_, p_215639_ -> p_240618_, p_240644_);
   }

   public void broadcastSystemMessage(Component p_240526_, Function<ServerPlayer, Component> p_240594_, boolean p_240648_) {
      this.server.sendSystemMessage(p_240526_);

      for (ServerPlayer serverplayer : this.players) {
         Component component = p_240594_.apply(serverplayer);
         if (component != null) {
            serverplayer.sendSystemMessage(component, p_240648_);
         }
      }
   }

   public void broadcastChatMessage(PlayerChatMessage p_243229_, CommandSourceStack p_243254_, ChatType.Bound p_243255_) {
      this.broadcastChatMessage(p_243229_, p_243254_::shouldFilterMessageTo, p_243254_.getPlayer(), p_243255_);
   }

   public void broadcastChatMessage(PlayerChatMessage p_243264_, ServerPlayer p_243234_, ChatType.Bound p_243204_) {
      this.broadcastChatMessage(p_243264_, p_243234_::shouldFilterMessageTo, p_243234_, p_243204_);
   }

   private void broadcastChatMessage(PlayerChatMessage p_249952_, Predicate<ServerPlayer> p_250784_, @Nullable ServerPlayer p_249623_, ChatType.Bound p_250276_) {
      boolean flag = this.verifyChatTrusted(p_249952_);
      this.server.logChatMessage(p_249952_.decoratedContent(), p_250276_, flag ? null : "Not Secure");
      OutgoingChatMessage outgoingchatmessage = OutgoingChatMessage.create(p_249952_);
      boolean flag1 = false;

      for (ServerPlayer serverplayer : this.players) {
         boolean flag2 = p_250784_.test(serverplayer);
         serverplayer.sendChatMessage(outgoingchatmessage, flag2, p_250276_);
         flag1 |= flag2 && p_249952_.isFullyFiltered();
      }

      if (flag1 && p_249623_ != null) {
         p_249623_.sendSystemMessage(CHAT_FILTERED_FULL);
      }
   }

   private boolean verifyChatTrusted(PlayerChatMessage p_251384_) {
      return p_251384_.hasSignature() && !p_251384_.hasExpiredServer(Instant.now());
   }

   public ServerStatsCounter getPlayerStats(Player p_11240_) {
      GameProfile gameprofile = p_11240_.getGameProfile();
      return this.stats.computeIfAbsent(gameprofile.id(), p_460349_ -> {
         Path path = this.locateStatsFile(gameprofile);
         return new ServerStatsCounter(this.server, path);
      });
   }

   private Path locateStatsFile(GameProfile p_460971_) {
      Path path = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
      Path path1 = path.resolve(p_460971_.id() + ".json");
      if (Files.exists(path1)) {
         return path1;
      } else {
         String s = p_460971_.name() + ".json";
         if (FileUtil.isValidPathSegment(s)) {
            Path path2 = path.resolve(s);
            if (Files.isRegularFile(path2)) {
               try {
                  return Files.move(path2, path1);
               } catch (IOException var7) {
                  LOGGER.warn("Failed to copy file {} to {}", s, path1);
                  return path2;
               }
            }
         }

         return path1;
      }
   }

   public PlayerAdvancements getPlayerAdvancements(ServerPlayer p_11297_) {
      UUID uuid = p_11297_.getUUID();
      PlayerAdvancements playeradvancements = this.advancements.get(uuid);
      if (playeradvancements == null) {
         Path path = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
         playeradvancements = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), path, p_11297_);
         this.advancements.put(uuid, playeradvancements);
      }

      if (!(p_11297_ instanceof FakePlayer)) {
         playeradvancements.setPlayer(p_11297_);
      }

      return playeradvancements;
   }

   public void setViewDistance(int p_11218_) {
      this.viewDistance = p_11218_;
      this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(p_11218_));

      for (ServerLevel serverlevel : this.server.getAllLevels()) {
         serverlevel.getChunkSource().setViewDistance(p_11218_);
      }
   }

   public void setSimulationDistance(int p_184212_) {
      this.simulationDistance = p_184212_;
      this.broadcastAll(new ClientboundSetSimulationDistancePacket(p_184212_));

      for (ServerLevel serverlevel : this.server.getAllLevels()) {
         serverlevel.getChunkSource().setSimulationDistance(p_184212_);
      }
   }

   public List<ServerPlayer> getPlayers() {
      return this.playersView;
   }

   @Nullable
   public ServerPlayer getPlayer(UUID p_11260_) {
      return this.playersByUUID.get(p_11260_);
   }

   @Nullable
   public ServerPlayer getPlayer(String p_442920_) {
      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.getGameProfile().name().equalsIgnoreCase(p_442920_)) {
            return serverplayer;
         }
      }

      return null;
   }

   public boolean canBypassPlayerLimit(NameAndId p_435364_) {
      return false;
   }

   public void reloadResources() {
      for (PlayerAdvancements playeradvancements : this.advancements.values()) {
         playeradvancements.reload(this.server.getAdvancements());
      }

      OnDatapackSyncEvent dataSyncEvent = (OnDatapackSyncEvent)NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(this, null));
      this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
      RecipeManager recipemanager = this.server.getRecipeManager();
      ClientboundUpdateRecipesPacket clientboundupdaterecipespacket = new ClientboundUpdateRecipesPacket(
         recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes()
      );

      for (ServerPlayer serverplayer : this.players) {
         serverplayer.connection.send(clientboundupdaterecipespacket);
         serverplayer.getRecipeBook().sendInitialRecipeBook(serverplayer);
         CommonHooks.sendRecipes(serverplayer, dataSyncEvent.getRecipeTypesToSend(), recipemanager.recipeMap());
      }
   }

   public boolean isAllowCommandsForAllPlayers() {
      return this.allowCommandsForAllPlayers;
   }

   public PlayerDataStorage getPlayerIo() {
      return this.playerIo;
   }
}
