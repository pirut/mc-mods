package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RealmsWorldOptions extends ValueObject implements ReflectionBasedSerialization {
   @SerializedName("spawnProtection")
   public int spawnProtection = 0;
   @SerializedName("forceGameMode")
   public boolean forceGameMode = false;
   @SerializedName("difficulty")
   public int difficulty = 2;
   @SerializedName("gameMode")
   public int gameMode = 0;
   @SerializedName("slotName")
   private String slotName = "";
   @SerializedName("version")
   public String version = "";
   @SerializedName("compatibility")
   public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
   @SerializedName("worldTemplateId")
   public long templateId = -1L;
   @SerializedName("worldTemplateImage")
   @Nullable
   public String templateImage = null;
   @Exclude
   public boolean empty;

   private RealmsWorldOptions() {
   }

   public RealmsWorldOptions(
      int spawnProtection, int difficulty, int gameMode, boolean forceGameMode, String slotName, String version, RealmsServer.Compatibility compatibility
   ) {
      this.spawnProtection = spawnProtection;
      this.difficulty = difficulty;
      this.gameMode = gameMode;
      this.forceGameMode = forceGameMode;
      this.slotName = slotName;
      this.version = version;
      this.compatibility = compatibility;
   }

   public static RealmsWorldOptions createDefaults() {
      return new RealmsWorldOptions();
   }

   public static RealmsWorldOptions createDefaultsWith(GameType gameMode, Difficulty difficulty, boolean hardcore, String version, String slotName) {
      RealmsWorldOptions realmsWorldOptions = createDefaults();
      realmsWorldOptions.difficulty = difficulty.getId();
      realmsWorldOptions.gameMode = gameMode.getId();
      realmsWorldOptions.slotName = slotName;
      realmsWorldOptions.version = version;
      return realmsWorldOptions;
   }

   public static RealmsWorldOptions createFromSettings(LevelSettings settings, String slotName) {
      return createDefaultsWith(settings.gameType(), settings.difficulty(), settings.hardcore(), slotName, settings.levelName());
   }

   public static RealmsWorldOptions createEmptyDefaults() {
      RealmsWorldOptions realmsWorldOptions = createDefaults();
      realmsWorldOptions.setEmpty(true);
      return realmsWorldOptions;
   }

   public void setEmpty(boolean empty) {
      this.empty = empty;
   }

   public static RealmsWorldOptions parse(GuardedSerializer serializer, String json) {
      RealmsWorldOptions realmsWorldOptions = serializer.fromJson(json, RealmsWorldOptions.class);
      if (realmsWorldOptions == null) {
         return createDefaults();
      } else {
         finalize(realmsWorldOptions);
         return realmsWorldOptions;
      }
   }

   private static void finalize(RealmsWorldOptions options) {
      if (options.slotName == null) {
         options.slotName = "";
      }

      if (options.version == null) {
         options.version = "";
      }

      if (options.compatibility == null) {
         options.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
      }
   }

   public String getSlotName(int slotIndex) {
      if (StringUtil.isBlank(this.slotName)) {
         return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(slotIndex);
      } else {
         return this.slotName;
      }
   }

   public String getDefaultSlotName(int slotIndex) {
      return I18n.get("mco.configure.world.slot", slotIndex);
   }

   public RealmsWorldOptions copy() {
      return new RealmsWorldOptions(this.spawnProtection, this.difficulty, this.gameMode, this.forceGameMode, this.slotName, this.version, this.compatibility);
   }
}
