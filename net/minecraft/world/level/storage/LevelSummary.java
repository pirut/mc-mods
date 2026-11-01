package net.minecraft.world.level.storage;

import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import org.apache.commons.lang3.StringUtils;

public class LevelSummary implements Comparable<LevelSummary> {
   public static final Component PLAY_WORLD = Component.translatable("selectWorld.select");
   private final LevelSettings settings;
   private final LevelVersion levelVersion;
   private final String levelId;
   private final boolean requiresManualConversion;
   private final boolean locked;
   private final boolean experimental;
   private final Path icon;
   @Nullable
   private Component info;

   public LevelSummary(
      LevelSettings p_251217_, LevelVersion p_249179_, String p_250462_, boolean p_252096_, boolean p_251054_, boolean p_252271_, Path p_252001_
   ) {
      this.settings = p_251217_;
      this.levelVersion = p_249179_;
      this.levelId = p_250462_;
      this.locked = p_251054_;
      this.experimental = p_252271_;
      this.icon = p_252001_;
      this.requiresManualConversion = p_252096_;
   }

   public String getLevelId() {
      return this.levelId;
   }

   public String getLevelName() {
      return StringUtils.isEmpty(this.settings.levelName()) ? this.levelId : this.settings.levelName();
   }

   public Path getIcon() {
      return this.icon;
   }

   public boolean requiresManualConversion() {
      return this.requiresManualConversion;
   }

   public boolean isExperimental() {
      return this.experimental;
   }

   public long getLastPlayed() {
      return this.levelVersion.lastPlayed();
   }

   public int compareTo(LevelSummary p_78360_) {
      if (this.getLastPlayed() < p_78360_.getLastPlayed()) {
         return 1;
      } else {
         return this.getLastPlayed() > p_78360_.getLastPlayed() ? -1 : this.levelId.compareTo(p_78360_.levelId);
      }
   }

   public LevelSettings getSettings() {
      return this.settings;
   }

   public GameType getGameMode() {
      return this.settings.gameType();
   }

   public boolean isHardcore() {
      return this.settings.hardcore();
   }

   public boolean hasCommands() {
      return this.settings.allowCommands();
   }

   public MutableComponent getWorldVersionName() {
      return StringUtil.isNullOrEmpty(this.levelVersion.minecraftVersionName())
         ? Component.translatable("selectWorld.versionUnknown")
         : Component.literal(this.levelVersion.minecraftVersionName());
   }

   public LevelVersion levelVersion() {
      return this.levelVersion;
   }

   public boolean shouldBackup() {
      return this.backupStatus().shouldBackup();
   }

   public boolean isDowngrade() {
      return this.backupStatus() == LevelSummary.BackupStatus.DOWNGRADE;
   }

   public LevelSummary.BackupStatus backupStatus() {
      WorldVersion $$0 = SharedConstants.getCurrentVersion();
      int $$1 = $$0.getDataVersion().getVersion();
      int $$2 = this.levelVersion.minecraftVersion().getVersion();
      if (!$$0.isStable() && $$2 < $$1) {
         return LevelSummary.BackupStatus.UPGRADE_TO_SNAPSHOT;
      } else {
         return $$2 > $$1 ? LevelSummary.BackupStatus.DOWNGRADE : LevelSummary.BackupStatus.NONE;
      }
   }

   public boolean isLocked() {
      return this.locked;
   }

   public boolean isDisabled() {
      return !this.isLocked() && !this.requiresManualConversion() ? !this.isCompatible() : true;
   }

   public boolean isCompatible() {
      return SharedConstants.getCurrentVersion().getDataVersion().isCompatible(this.levelVersion.minecraftVersion());
   }

   public Component getInfo() {
      if (this.info == null) {
         this.info = this.createInfo();
      }

      return this.info;
   }

   private Component createInfo() {
      if (this.isLocked()) {
         return Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
      } else if (this.requiresManualConversion()) {
         return Component.translatable("selectWorld.conversion").withStyle(ChatFormatting.RED);
      } else if (!this.isCompatible()) {
         return Component.translatable("selectWorld.incompatible.info", this.getWorldVersionName()).withStyle(ChatFormatting.RED);
      } else {
         MutableComponent $$0 = this.isHardcore()
            ? Component.empty().append(Component.translatable("gameMode.hardcore").withColor(-65536))
            : Component.translatable("gameMode." + this.getGameMode().getName());
         if (this.hasCommands()) {
            $$0.append(", ").append(Component.translatable("selectWorld.commands"));
         }

         if (this.isExperimental()) {
            $$0.append(", ").append(Component.translatable("selectWorld.experimental").withStyle(ChatFormatting.YELLOW));
         }

         MutableComponent $$1 = this.getWorldVersionName();
         MutableComponent $$2 = Component.literal(", ").append(Component.translatable("selectWorld.version")).append(CommonComponents.SPACE);
         if (this.shouldBackup()) {
            $$2.append($$1.withStyle(this.isDowngrade() ? ChatFormatting.RED : ChatFormatting.ITALIC));
         } else {
            $$2.append($$1);
         }

         $$0.append($$2);
         return $$0;
      }
   }

   public Component primaryActionMessage() {
      return PLAY_WORLD;
   }

   public boolean primaryActionActive() {
      return !this.isDisabled();
   }

   public boolean canUpload() {
      return !this.requiresManualConversion() && !this.isLocked();
   }

   public boolean canEdit() {
      return !this.isDisabled();
   }

   public boolean canRecreate() {
      return !this.isDisabled();
   }

   public boolean canDelete() {
      return true;
   }

   public static enum BackupStatus {
      NONE(false, false, ""),
      DOWNGRADE(true, true, "downgrade"),
      UPGRADE_TO_SNAPSHOT(true, false, "snapshot");

      private final boolean shouldBackup;
      private final boolean severe;
      private final String translationKey;

      private BackupStatus(boolean p_164928_, boolean p_164929_, String p_164930_) {
         this.shouldBackup = p_164928_;
         this.severe = p_164929_;
         this.translationKey = p_164930_;
      }

      public boolean shouldBackup() {
         return this.shouldBackup;
      }

      public boolean isSevere() {
         return this.severe;
      }

      public String getTranslationKey() {
         return this.translationKey;
      }
   }

   public static class CorruptedLevelSummary extends LevelSummary {
      private static final Component INFO = Component.translatable("recover_world.warning").withStyle(p_307345_ -> p_307345_.withColor(-65536));
      private static final Component RECOVER = Component.translatable("recover_world.button");
      private final long lastPlayed;

      public CorruptedLevelSummary(String p_307261_, Path p_307409_, long p_307353_) {
         super(null, null, p_307261_, false, false, false, p_307409_);
         this.lastPlayed = p_307353_;
      }

      @Override
      public String getLevelName() {
         return this.getLevelId();
      }

      @Override
      public Component getInfo() {
         return INFO;
      }

      @Override
      public long getLastPlayed() {
         return this.lastPlayed;
      }

      @Override
      public boolean isDisabled() {
         return false;
      }

      @Override
      public Component primaryActionMessage() {
         return RECOVER;
      }

      @Override
      public boolean primaryActionActive() {
         return true;
      }

      @Override
      public boolean canUpload() {
         return false;
      }

      @Override
      public boolean canEdit() {
         return false;
      }

      @Override
      public boolean canRecreate() {
         return false;
      }
   }

   public static class SymlinkLevelSummary extends LevelSummary {
      private static final Component MORE_INFO_BUTTON = Component.translatable("symlink_warning.more_info");
      private static final Component INFO = Component.translatable("symlink_warning.title").withColor(-65536);

      public SymlinkLevelSummary(String p_289942_, Path p_289953_) {
         super(null, null, p_289942_, false, false, false, p_289953_);
      }

      @Override
      public String getLevelName() {
         return this.getLevelId();
      }

      @Override
      public Component getInfo() {
         return INFO;
      }

      @Override
      public long getLastPlayed() {
         return -1L;
      }

      @Override
      public boolean isDisabled() {
         return false;
      }

      @Override
      public Component primaryActionMessage() {
         return MORE_INFO_BUTTON;
      }

      @Override
      public boolean primaryActionActive() {
         return true;
      }

      @Override
      public boolean canUpload() {
         return false;
      }

      @Override
      public boolean canEdit() {
         return false;
      }

      @Override
      public boolean canRecreate() {
         return false;
      }
   }
}
