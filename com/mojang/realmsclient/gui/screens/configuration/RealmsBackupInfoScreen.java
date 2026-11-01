package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupInfoScreen extends RealmsScreen {
   private static final Component TITLE = Component.translatable("mco.backup.info.title");
   private static final Component UNKNOWN = Component.translatable("mco.backup.unknown");
   private final Screen lastScreen;
   final Backup backup;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private RealmsBackupInfoScreen.BackupInfoList backupInfoList;

   public RealmsBackupInfoScreen(Screen p_419999_, Backup p_420002_) {
      super(TITLE);
      this.lastScreen = p_419999_;
      this.backup = p_420002_;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.backupInfoList = this.layout.addToContents(new RealmsBackupInfoScreen.BackupInfoList(this.minecraft));
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_419885_ -> this.onClose()).build());
      this.repositionElements();
      this.layout.visitWidgets(p_419532_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_419532_);
      });
   }

   @Override
   protected void repositionElements() {
      this.backupInfoList.updateSize(this.width, this.layout);
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   Component checkForSpecificMetadata(String p_420060_, String p_419726_) {
      String s = p_420060_.toLowerCase(Locale.ROOT);
      if (s.contains("game") && s.contains("mode")) {
         return this.gameModeMetadata(p_419726_);
      } else if (s.contains("game") && s.contains("difficulty")) {
         return this.gameDifficultyMetadata(p_419726_);
      } else {
         return (Component)(p_420060_.equals("world_type") ? this.parseWorldType(p_419726_) : Component.literal(p_419726_));
      }
   }

   private Component gameDifficultyMetadata(String p_419466_) {
      try {
         return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(p_419466_)).getDisplayName();
      } catch (Exception var3) {
         return UNKNOWN;
      }
   }

   private Component gameModeMetadata(String p_420004_) {
      try {
         return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(p_420004_)).getShortDisplayName();
      } catch (Exception var3) {
         return UNKNOWN;
      }
   }

   private Component parseWorldType(String p_457630_) {
      try {
         return RealmsServer.WorldType.valueOf(p_457630_.toUpperCase(Locale.ROOT)).getDisplayName();
      } catch (Exception var3) {
         return RealmsServer.WorldType.UNKNOWN.getDisplayName();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class BackupInfoList extends ObjectSelectionList<RealmsBackupInfoScreen.BackupInfoListEntry> {
      public BackupInfoList(Minecraft p_419557_) {
         super(
            p_419557_,
            RealmsBackupInfoScreen.this.width,
            RealmsBackupInfoScreen.this.layout.getContentHeight(),
            RealmsBackupInfoScreen.this.layout.getHeaderHeight(),
            36
         );
         if (RealmsBackupInfoScreen.this.backup.changeList != null) {
            RealmsBackupInfoScreen.this.backup
               .changeList
               .forEach((p_419567_, p_419671_) -> this.addEntry(RealmsBackupInfoScreen.this.new BackupInfoListEntry(p_419567_, p_419671_)));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class BackupInfoListEntry extends ObjectSelectionList.Entry<RealmsBackupInfoScreen.BackupInfoListEntry> {
      private static final Component TEMPLATE_NAME = Component.translatable("mco.backup.entry.templateName");
      private static final Component GAME_DIFFICULTY = Component.translatable("mco.backup.entry.gameDifficulty");
      private static final Component NAME = Component.translatable("mco.backup.entry.name");
      private static final Component GAME_SERVER_VERSION = Component.translatable("mco.backup.entry.gameServerVersion");
      private static final Component UPLOADED = Component.translatable("mco.backup.entry.uploaded");
      private static final Component ENABLED_PACK = Component.translatable("mco.backup.entry.enabledPack");
      private static final Component DESCRIPTION = Component.translatable("mco.backup.entry.description");
      private static final Component GAME_MODE = Component.translatable("mco.backup.entry.gameMode");
      private static final Component SEED = Component.translatable("mco.backup.entry.seed");
      private static final Component WORLD_TYPE = Component.translatable("mco.backup.entry.worldType");
      private static final Component UNDEFINED = Component.translatable("mco.backup.entry.undefined");
      private final String key;
      private final String value;
      private final Component keyComponent;
      private final Component valueComponent;

      public BackupInfoListEntry(String p_419829_, String p_419737_) {
         this.key = p_419829_;
         this.value = p_419737_;
         this.keyComponent = this.translateKey(p_419829_);
         this.valueComponent = RealmsBackupInfoScreen.this.checkForSpecificMetadata(p_419829_, p_419737_);
      }

      @Override
      public void renderContent(GuiGraphics p_439863_, int p_438898_, int p_440008_, boolean p_439966_, float p_439342_) {
         p_439863_.drawString(RealmsBackupInfoScreen.this.font, this.keyComponent, this.getContentX(), this.getContentY(), -6250336);
         p_439863_.drawString(RealmsBackupInfoScreen.this.font, this.valueComponent, this.getContentX(), this.getContentY() + 12, -1);
      }

      private Component translateKey(String p_419626_) {
         return switch (p_419626_) {
            case "template_name" -> TEMPLATE_NAME;
            case "game_difficulty" -> GAME_DIFFICULTY;
            case "name" -> NAME;
            case "game_server_version" -> GAME_SERVER_VERSION;
            case "uploaded" -> UPLOADED;
            case "enabled_packs" -> ENABLED_PACK;
            case "description" -> DESCRIPTION;
            case "game_mode" -> GAME_MODE;
            case "seed" -> SEED;
            case "world_type" -> WORLD_TYPE;
            default -> UNDEFINED;
         };
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.key + " " + this.value);
      }
   }
}
