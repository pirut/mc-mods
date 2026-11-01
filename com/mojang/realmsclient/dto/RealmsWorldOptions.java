package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
      int p_420018_, int p_419836_, int p_419763_, boolean p_419614_, String p_420038_, String p_419539_, RealmsServer.Compatibility p_419813_
   ) {
      this.spawnProtection = p_420018_;
      this.difficulty = p_419836_;
      this.gameMode = p_419763_;
      this.forceGameMode = p_419614_;
      this.slotName = p_420038_;
      this.version = p_419539_;
      this.compatibility = p_419813_;
   }

   public static RealmsWorldOptions createDefaults() {
      return new RealmsWorldOptions();
   }

   public static RealmsWorldOptions createDefaultsWith(GameType p_374295_, Difficulty p_374163_, boolean p_374405_, String p_374283_, String p_374182_) {
      RealmsWorldOptions realmsworldoptions = createDefaults();
      realmsworldoptions.difficulty = p_374163_.getId();
      realmsworldoptions.gameMode = p_374295_.getId();
      realmsworldoptions.slotName = p_374182_;
      realmsworldoptions.version = p_374283_;
      return realmsworldoptions;
   }

   public static RealmsWorldOptions createFromSettings(LevelSettings p_374279_, String p_374533_) {
      return createDefaultsWith(p_374279_.gameType(), p_374279_.difficulty(), p_374279_.hardcore(), p_374533_, p_374279_.levelName());
   }

   public static RealmsWorldOptions createEmptyDefaults() {
      RealmsWorldOptions realmsworldoptions = createDefaults();
      realmsworldoptions.setEmpty(true);
      return realmsworldoptions;
   }

   public void setEmpty(boolean p_87631_) {
      this.empty = p_87631_;
   }

   public static RealmsWorldOptions parse(GuardedSerializer p_419639_, String p_419547_) {
      RealmsWorldOptions realmsworldoptions = p_419639_.fromJson(p_419547_, RealmsWorldOptions.class);
      if (realmsworldoptions == null) {
         return createDefaults();
      } else {
         finalize(realmsworldoptions);
         return realmsworldoptions;
      }
   }

   private static void finalize(RealmsWorldOptions p_419806_) {
      if (p_419806_.slotName == null) {
         p_419806_.slotName = "";
      }

      if (p_419806_.version == null) {
         p_419806_.version = "";
      }

      if (p_419806_.compatibility == null) {
         p_419806_.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
      }
   }

   public String getSlotName(int p_87627_) {
      if (StringUtil.isBlank(this.slotName)) {
         return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(p_87627_);
      } else {
         return this.slotName;
      }
   }

   public String getDefaultSlotName(int p_87634_) {
      return I18n.get("mco.configure.world.slot", p_87634_);
   }

   public RealmsWorldOptions copy() {
      return new RealmsWorldOptions(this.spawnProtection, this.difficulty, this.gameMode, this.forceGameMode, this.slotName, this.version, this.compatibility);
   }
}
