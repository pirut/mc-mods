package net.minecraft.server.jsonrpc.methods;

import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class ServerSettingsService {
   public static boolean autosave(MinecraftApi p_443180_) {
      return p_443180_.serverSettingsService().isAutoSave();
   }

   public static boolean setAutosave(MinecraftApi p_442975_, boolean p_442878_, ClientInfo p_442862_) {
      return p_442975_.serverSettingsService().setAutoSave(p_442878_, p_442862_);
   }

   public static Difficulty difficulty(MinecraftApi p_442581_) {
      return p_442581_.serverSettingsService().getDifficulty();
   }

   public static Difficulty setDifficulty(MinecraftApi p_443254_, Difficulty p_442649_, ClientInfo p_442990_) {
      return p_443254_.serverSettingsService().setDifficulty(p_442649_, p_442990_);
   }

   public static boolean enforceAllowlist(MinecraftApi p_443214_) {
      return p_443214_.serverSettingsService().isEnforceWhitelist();
   }

   public static boolean setEnforceAllowlist(MinecraftApi p_442546_, boolean p_443353_, ClientInfo p_442615_) {
      return p_442546_.serverSettingsService().setEnforceWhitelist(p_443353_, p_442615_);
   }

   public static boolean usingAllowlist(MinecraftApi p_443396_) {
      return p_443396_.serverSettingsService().isUsingWhitelist();
   }

   public static boolean setUsingAllowlist(MinecraftApi p_443512_, boolean p_443583_, ClientInfo p_442585_) {
      return p_443512_.serverSettingsService().setUsingWhitelist(p_443583_, p_442585_);
   }

   public static int maxPlayers(MinecraftApi p_443095_) {
      return p_443095_.serverSettingsService().getMaxPlayers();
   }

   public static int setMaxPlayers(MinecraftApi p_442890_, int p_443311_, ClientInfo p_443200_) {
      return p_442890_.serverSettingsService().setMaxPlayers(p_443311_, p_443200_);
   }

   public static int pauseWhenEmpty(MinecraftApi p_442980_) {
      return p_442980_.serverSettingsService().getPauseWhenEmptySeconds();
   }

   public static int setPauseWhenEmpty(MinecraftApi p_442910_, int p_442921_, ClientInfo p_443479_) {
      return p_442910_.serverSettingsService().setPauseWhenEmptySeconds(p_442921_, p_443479_);
   }

   public static int playerIdleTimeout(MinecraftApi p_442508_) {
      return p_442508_.serverSettingsService().getPlayerIdleTimeout();
   }

   public static int setPlayerIdleTimeout(MinecraftApi p_442699_, int p_443107_, ClientInfo p_442679_) {
      return p_442699_.serverSettingsService().setPlayerIdleTimeout(p_443107_, p_442679_);
   }

   public static boolean allowFlight(MinecraftApi p_443451_) {
      return p_443451_.serverSettingsService().allowFlight();
   }

   public static boolean setAllowFlight(MinecraftApi p_443539_, boolean p_443531_, ClientInfo p_443449_) {
      return p_443539_.serverSettingsService().setAllowFlight(p_443531_, p_443449_);
   }

   public static int spawnProtection(MinecraftApi p_443434_) {
      return p_443434_.serverSettingsService().getSpawnProtectionRadius();
   }

   public static int setSpawnProtection(MinecraftApi p_443541_, int p_443027_, ClientInfo p_443545_) {
      return p_443541_.serverSettingsService().setSpawnProtectionRadius(p_443027_, p_443545_);
   }

   public static String motd(MinecraftApi p_442738_) {
      return p_442738_.serverSettingsService().getMotd();
   }

   public static String setMotd(MinecraftApi p_442658_, String p_443330_, ClientInfo p_443070_) {
      return p_442658_.serverSettingsService().setMotd(p_443330_, p_443070_);
   }

   public static boolean forceGameMode(MinecraftApi p_442655_) {
      return p_442655_.serverSettingsService().forceGameMode();
   }

   public static boolean setForceGameMode(MinecraftApi p_443229_, boolean p_443216_, ClientInfo p_442933_) {
      return p_443229_.serverSettingsService().setForceGameMode(p_443216_, p_442933_);
   }

   public static GameType gameMode(MinecraftApi p_443462_) {
      return p_443462_.serverSettingsService().getGameMode();
   }

   public static GameType setGameMode(MinecraftApi p_443001_, GameType p_442795_, ClientInfo p_443126_) {
      return p_443001_.serverSettingsService().setGameMode(p_442795_, p_443126_);
   }

   public static int viewDistance(MinecraftApi p_442729_) {
      return p_442729_.serverSettingsService().getViewDistance();
   }

   public static int setViewDistance(MinecraftApi p_442978_, int p_443171_, ClientInfo p_442526_) {
      return p_442978_.serverSettingsService().setViewDistance(p_443171_, p_442526_);
   }

   public static int simulationDistance(MinecraftApi p_443282_) {
      return p_443282_.serverSettingsService().getSimulationDistance();
   }

   public static int setSimulationDistance(MinecraftApi p_442690_, int p_442557_, ClientInfo p_443231_) {
      return p_442690_.serverSettingsService().setSimulationDistance(p_442557_, p_443231_);
   }

   public static boolean acceptTransfers(MinecraftApi p_442591_) {
      return p_442591_.serverSettingsService().acceptsTransfers();
   }

   public static boolean setAcceptTransfers(MinecraftApi p_442768_, boolean p_443121_, ClientInfo p_443054_) {
      return p_442768_.serverSettingsService().setAcceptsTransfers(p_443121_, p_443054_);
   }

   public static int statusHeartbeatInterval(MinecraftApi p_442813_) {
      return p_442813_.serverSettingsService().getStatusHeartbeatInterval();
   }

   public static int setStatusHeartbeatInterval(MinecraftApi p_443034_, int p_442977_, ClientInfo p_442903_) {
      return p_443034_.serverSettingsService().setStatusHeartbeatInterval(p_442977_, p_442903_);
   }

   public static PermissionLevel operatorUserPermissionLevel(MinecraftApi p_443540_) {
      return p_443540_.serverSettingsService().getOperatorUserPermissions().level();
   }

   public static PermissionLevel setOperatorUserPermissionLevel(MinecraftApi p_442758_, PermissionLevel p_455709_, ClientInfo p_443568_) {
      return p_442758_.serverSettingsService().setOperatorUserPermissions(LevelBasedPermissionSet.forLevel(p_455709_), p_443568_).level();
   }

   public static boolean hidesOnlinePlayers(MinecraftApi p_442673_) {
      return p_442673_.serverSettingsService().hidesOnlinePlayers();
   }

   public static boolean setHidesOnlinePlayers(MinecraftApi p_443322_, boolean p_443158_, ClientInfo p_442931_) {
      return p_443322_.serverSettingsService().setHidesOnlinePlayers(p_443158_, p_442931_);
   }

   public static boolean repliesToStatus(MinecraftApi p_443480_) {
      return p_443480_.serverSettingsService().repliesToStatus();
   }

   public static boolean setRepliesToStatus(MinecraftApi p_442992_, boolean p_443227_, ClientInfo p_443620_) {
      return p_442992_.serverSettingsService().setRepliesToStatus(p_443227_, p_443620_);
   }

   public static int entityBroadcastRangePercentage(MinecraftApi p_442491_) {
      return p_442491_.serverSettingsService().getEntityBroadcastRangePercentage();
   }

   public static int setEntityBroadcastRangePercentage(MinecraftApi p_442594_, int p_443497_, ClientInfo p_443343_) {
      return p_442594_.serverSettingsService().setEntityBroadcastRangePercentage(p_443497_, p_443343_);
   }
}
