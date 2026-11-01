package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.backup");
   static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
   static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
   private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
   private static final Component DOWNLOAD_LATEST = Component.translatable("mco.backup.button.download");
   private static final String UPLOADED_KEY = "uploaded";
   private static final int PADDING = 8;
   final RealmsConfigureWorldScreen lastScreen;
   List<Backup> backups = Collections.emptyList();
   @Nullable
   RealmsBackupScreen.BackupObjectSelectionList backupList;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final int slotId;
   @Nullable
   Button downloadButton;
   final RealmsServer serverData;
   boolean noBackups = false;

   public RealmsBackupScreen(RealmsConfigureWorldScreen p_88126_, RealmsServer p_88127_, int p_88128_) {
      super(TITLE);
      this.lastScreen = p_88126_;
      this.serverData = p_88127_;
      this.slotId = p_88128_;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.backupList = this.layout.addToContents(new RealmsBackupScreen.BackupObjectSelectionList());
      LinearLayout $$0 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.downloadButton = $$0.addChild(Button.builder(DOWNLOAD_LATEST, p_88185_ -> this.downloadClicked()).build());
      this.downloadButton.active = false;
      $$0.addChild(Button.builder(CommonComponents.GUI_BACK, p_329634_ -> this.onClose()).build());
      this.layout.visitWidgets(p_329637_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_329637_);
      });
      this.repositionElements();
      this.fetchRealmsBackups();
   }

   @Override
   public void render(GuiGraphics p_283405_, int p_282020_, int p_282404_, float p_281280_) {
      super.render(p_283405_, p_282020_, p_282404_, p_281280_);
      if (this.noBackups && this.backupList != null) {
         p_283405_.drawString(
            this.font,
            NO_BACKUPS_LABEL,
            this.width / 2 - this.font.width(NO_BACKUPS_LABEL) / 2,
            this.backupList.getY() + this.backupList.getHeight() / 2 - 9 / 2,
            -1,
            false
         );
      }
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.backupList != null) {
         this.backupList.updateSize(this.width, this.layout);
      }
   }

   private void fetchRealmsBackups() {
      (new Thread("Realms-fetch-backups") {
         @Override
         public void run() {
            RealmsClient $$0 = RealmsClient.create();

            try {
               List<Backup> $$1 = $$0.backupsFor(RealmsBackupScreen.this.serverData.id).backups;
               RealmsBackupScreen.this.minecraft.execute(() -> {
                  RealmsBackupScreen.this.backups = $$1;
                  RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                  if (!RealmsBackupScreen.this.noBackups && RealmsBackupScreen.this.downloadButton != null) {
                     RealmsBackupScreen.this.downloadButton.active = true;
                  }

                  if (RealmsBackupScreen.this.backupList != null) {
                     RealmsBackupScreen.this.backupList.children().clear();

                     for (Backup $$1x : RealmsBackupScreen.this.backups) {
                        RealmsBackupScreen.this.backupList.addEntry($$1x);
                     }
                  }
               });
            } catch (RealmsServiceException var3) {
               RealmsBackupScreen.LOGGER.error("Couldn't request backups", var3);
            }
         }
      }).start();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void downloadClicked() {
      this.minecraft
         .setScreen(
            RealmsPopups.infoPopupScreen(
               this,
               Component.translatable("mco.configure.world.restore.download.question.line1"),
               p_344114_ -> this.minecraft
                  .setScreen(
                     new RealmsLongRunningMcoTaskScreen(
                        this.lastScreen.getNewScreen(),
                        new DownloadTask(
                           this.serverData.id,
                           this.slotId,
                           this.serverData.name + " (" + this.serverData.slots.get(this.serverData.activeSlot).getSlotName(this.serverData.activeSlot) + ")",
                           this
                        )
                     )
                  )
            )
         );
   }

   @OnlyIn(Dist.CLIENT)
   class BackupObjectSelectionList extends ContainerObjectSelectionList<RealmsBackupScreen.Entry> {
      private static final int ITEM_HEIGHT = 36;

      public BackupObjectSelectionList() {
         super(
            Minecraft.getInstance(),
            RealmsBackupScreen.this.width,
            RealmsBackupScreen.this.layout.getContentHeight(),
            RealmsBackupScreen.this.layout.getHeaderHeight(),
            36
         );
      }

      public void addEntry(Backup p_88235_) {
         this.addEntry(RealmsBackupScreen.this.new Entry(p_88235_));
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 36 + this.headerHeight;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ContainerObjectSelectionList.Entry<RealmsBackupScreen.Entry> {
      private static final int Y_PADDING = 2;
      private final Backup backup;
      @Nullable
      private Button restoreButton;
      @Nullable
      private Button changesButton;
      private final List<AbstractWidget> children = new ArrayList<>();

      public Entry(Backup p_88250_) {
         this.backup = p_88250_;
         this.populateChangeList(p_88250_);
         if (!p_88250_.changeList.isEmpty()) {
            this.changesButton = Button.builder(
                  RealmsBackupScreen.HAS_CHANGES_TOOLTIP,
                  p_344115_ -> RealmsBackupScreen.this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.backup))
               )
               .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
               .createNarration(
                  p_329639_ -> CommonComponents.joinForNarration(Component.translatable("mco.backup.narration", this.getShortBackupDate()), p_329639_.get())
               )
               .build();
            this.children.add(this.changesButton);
         }

         if (!RealmsBackupScreen.this.serverData.expired) {
            this.restoreButton = Button.builder(RealmsBackupScreen.RESTORE_TOOLTIP, p_329642_ -> this.restoreClicked())
               .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
               .createNarration(
                  p_329640_ -> CommonComponents.joinForNarration(Component.translatable("mco.backup.narration", this.getShortBackupDate()), p_329640_.get())
               )
               .build();
            this.children.add(this.restoreButton);
         }
      }

      private void populateChangeList(Backup p_279365_) {
         int $$1 = RealmsBackupScreen.this.backups.indexOf(p_279365_);
         if ($$1 != RealmsBackupScreen.this.backups.size() - 1) {
            Backup $$2 = RealmsBackupScreen.this.backups.get($$1 + 1);

            for (String $$3 : p_279365_.metadata.keySet()) {
               if (!$$3.contains("uploaded") && $$2.metadata.containsKey($$3)) {
                  if (!p_279365_.metadata.get($$3).equals($$2.metadata.get($$3))) {
                     this.addToChangeList($$3);
                  }
               } else {
                  this.addToChangeList($$3);
               }
            }
         }
      }

      private void addToChangeList(String p_279195_) {
         if (p_279195_.contains("uploaded")) {
            String $$1 = DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
            this.backup.changeList.put(p_279195_, $$1);
            this.backup.setUploadedVersion(true);
         } else {
            this.backup.changeList.put(p_279195_, this.backup.metadata.get(p_279195_));
         }
      }

      private String getShortBackupDate() {
         return DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
      }

      private void restoreClicked() {
         Component $$0 = RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate);
         Component $$1 = Component.translatable("mco.configure.world.restore.question.line1", this.getShortBackupDate(), $$0);
         RealmsBackupScreen.this.minecraft
            .setScreen(
               RealmsPopups.warningPopupScreen(
                  RealmsBackupScreen.this,
                  $$1,
                  p_344116_ -> RealmsBackupScreen.this.minecraft
                     .setScreen(
                        new RealmsLongRunningMcoTaskScreen(
                           RealmsBackupScreen.this.lastScreen.getNewScreen(),
                           new RestoreTask(this.backup, RealmsBackupScreen.this.serverData.id, RealmsBackupScreen.this.lastScreen)
                        )
                     )
               )
            );
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      @Override
      public void render(
         GuiGraphics p_281408_,
         int p_281974_,
         int p_282495_,
         int p_282463_,
         int p_281562_,
         int p_282782_,
         int p_281638_,
         int p_283190_,
         boolean p_283105_,
         float p_282066_
      ) {
         int $$10 = p_282495_ + p_282782_ / 2;
         int $$11 = $$10 - 9 - 2;
         int $$12 = $$10 + 2;
         int $$13 = this.backup.isUploadedVersion() ? -8388737 : -1;
         p_281408_.drawString(
            RealmsBackupScreen.this.font,
            Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate)),
            p_282463_,
            $$11,
            $$13,
            false
         );
         p_281408_.drawString(RealmsBackupScreen.this.font, this.getMediumDatePresentation(this.backup.lastModifiedDate), p_282463_, $$12, 5000268, false);
         int $$14 = 0;
         int $$15 = p_282495_ + p_282782_ / 2 - 10;
         if (this.restoreButton != null) {
            $$14 += this.restoreButton.getWidth() + 8;
            this.restoreButton.setX(p_282463_ + p_281562_ - $$14);
            this.restoreButton.setY($$15);
            this.restoreButton.render(p_281408_, p_281638_, p_283190_, p_282066_);
         }

         if (this.changesButton != null) {
            $$14 += this.changesButton.getWidth() + 8;
            this.changesButton.setX(p_282463_ + p_281562_ - $$14);
            this.changesButton.setY($$15);
            this.changesButton.render(p_281408_, p_281638_, p_283190_, p_282066_);
         }
      }

      private String getMediumDatePresentation(Date p_88276_) {
         return DateFormat.getDateTimeInstance(3, 3).format(p_88276_);
      }
   }
}
