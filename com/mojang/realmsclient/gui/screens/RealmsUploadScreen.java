package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ReentrantLock UPLOAD_LOCK = new ReentrantLock();
   private static final int BAR_WIDTH = 200;
   private static final int BAR_TOP = 80;
   private static final int BAR_BOTTOM = 95;
   private static final int BAR_BORDER = 1;
   private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
   private static final Component VERIFYING_TEXT = Component.translatable("mco.upload.verifying");
   private final RealmsResetWorldScreen lastScreen;
   private final LevelSummary selectedLevel;
   @Nullable
   private final RealmCreationTask realmCreationTask;
   private final long realmId;
   private final int slotId;
   private final UploadStatus uploadStatus;
   private final RateLimiter narrationRateLimiter;
   @Nullable
   private volatile Component[] errorMessage;
   private volatile Component status = Component.translatable("mco.upload.preparing");
   @Nullable
   private volatile String progress;
   private volatile boolean cancelled;
   private volatile boolean uploadFinished;
   private volatile boolean showDots = true;
   private volatile boolean uploadStarted;
   @Nullable
   private Button backButton;
   @Nullable
   private Button cancelButton;
   private int tickCount;
   @Nullable
   private Long previousWrittenBytes;
   @Nullable
   private Long previousTimeSnapshot;
   private long bytesPersSecond;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   public RealmsUploadScreen(@Nullable RealmCreationTask p_320023_, long p_90083_, int p_90084_, RealmsResetWorldScreen p_90085_, LevelSummary p_90086_) {
      super(GameNarrator.NO_TITLE);
      this.realmCreationTask = p_320023_;
      this.realmId = p_90083_;
      this.slotId = p_90084_;
      this.lastScreen = p_90085_;
      this.selectedLevel = p_90086_;
      this.uploadStatus = new UploadStatus();
      this.narrationRateLimiter = RateLimiter.create(0.1F);
   }

   @Override
   public void init() {
      this.backButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_90118_ -> this.onBack()).build());
      this.backButton.visible = false;
      this.cancelButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_CANCEL, p_90104_ -> this.onCancel()).build());
      if (!this.uploadStarted) {
         if (this.lastScreen.slot == -1) {
            this.uploadStarted = true;
            this.upload();
         } else {
            List<LongRunningTask> $$0 = new ArrayList<>();
            if (this.realmCreationTask != null) {
               $$0.add(this.realmCreationTask);
            }

            $$0.add(new SwitchSlotTask(this.realmId, this.lastScreen.slot, () -> {
               if (!this.uploadStarted) {
                  this.uploadStarted = true;
                  this.minecraft.execute(() -> {
                     this.minecraft.setScreen(this);
                     this.upload();
                  });
               }
            }));
            this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, $$0.toArray(new LongRunningTask[0])));
         }
      }

      this.layout.visitWidgets(p_321350_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_321350_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   private void onBack() {
      this.minecraft.setScreen(new RealmsConfigureWorldScreen(new RealmsMainScreen(new TitleScreen()), this.realmId));
   }

   private void onCancel() {
      this.cancelled = true;
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public boolean keyPressed(int p_90089_, int p_90090_, int p_90091_) {
      if (p_90089_ == 256) {
         if (this.showDots) {
            this.onCancel();
         } else {
            this.onBack();
         }

         return true;
      } else {
         return super.keyPressed(p_90089_, p_90090_, p_90091_);
      }
   }

   @Override
   public void render(GuiGraphics p_282140_, int p_90097_, int p_90098_, float p_90099_) {
      super.render(p_282140_, p_90097_, p_90098_, p_90099_);
      if (!this.uploadFinished
         && this.uploadStatus.bytesWritten != 0L
         && this.uploadStatus.bytesWritten == this.uploadStatus.totalBytes
         && this.cancelButton != null) {
         this.status = VERIFYING_TEXT;
         this.cancelButton.active = false;
      }

      p_282140_.drawCenteredString(this.font, this.status, this.width / 2, 50, -1);
      if (this.showDots) {
         p_282140_.drawString(this.font, DOTS[this.tickCount / 10 % DOTS.length], this.width / 2 + this.font.width(this.status) / 2 + 5, 50, -1, false);
      }

      if (this.uploadStatus.bytesWritten != 0L && !this.cancelled) {
         this.drawProgressBar(p_282140_);
         this.drawUploadSpeed(p_282140_);
      }

      Component[] $$4 = this.errorMessage;
      if ($$4 != null) {
         for (int $$5 = 0; $$5 < $$4.length; $$5++) {
            p_282140_.drawCenteredString(this.font, $$4[$$5], this.width / 2, 110 + 12 * $$5, -65536);
         }
      }
   }

   private void drawProgressBar(GuiGraphics p_282575_) {
      double $$1 = Math.min((double)this.uploadStatus.bytesWritten / this.uploadStatus.totalBytes, 1.0);
      this.progress = String.format(Locale.ROOT, "%.1f", $$1 * 100.0);
      int $$2 = (this.width - 200) / 2;
      int $$3 = $$2 + (int)Math.round(200.0 * $$1);
      p_282575_.fill($$2 - 1, 79, $$3 + 1, 96, -1);
      p_282575_.fill($$2, 80, $$3, 95, -8355712);
      p_282575_.drawCenteredString(this.font, Component.translatable("mco.upload.percent", this.progress), this.width / 2, 84, -1);
   }

   private void drawUploadSpeed(GuiGraphics p_281884_) {
      if (this.tickCount % 20 == 0) {
         if (this.previousWrittenBytes != null && this.previousTimeSnapshot != null) {
            long $$1 = Util.getMillis() - this.previousTimeSnapshot;
            if ($$1 == 0L) {
               $$1 = 1L;
            }

            this.bytesPersSecond = 1000L * (this.uploadStatus.bytesWritten - this.previousWrittenBytes) / $$1;
            this.drawUploadSpeed0(p_281884_, this.bytesPersSecond);
         }

         this.previousWrittenBytes = this.uploadStatus.bytesWritten;
         this.previousTimeSnapshot = Util.getMillis();
      } else {
         this.drawUploadSpeed0(p_281884_, this.bytesPersSecond);
      }
   }

   private void drawUploadSpeed0(GuiGraphics p_282279_, long p_282827_) {
      String $$2 = this.progress;
      if (p_282827_ > 0L && $$2 != null) {
         int $$3 = this.font.width($$2);
         String $$4 = "(" + Unit.humanReadable(p_282827_) + "/s)";
         p_282279_.drawString(this.font, $$4, this.width / 2 + $$3 / 2 + 15, 84, -1, false);
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.tickCount++;
      if (this.narrationRateLimiter.tryAcquire(1)) {
         Component $$0 = this.createProgressNarrationMessage();
         this.minecraft.getNarrator().sayNow($$0);
      }
   }

   private Component createProgressNarrationMessage() {
      List<Component> $$0 = Lists.newArrayList();
      $$0.add(this.status);
      if (this.progress != null) {
         $$0.add(Component.translatable("mco.upload.percent", this.progress));
      }

      Component[] $$1 = this.errorMessage;
      if ($$1 != null) {
         $$0.addAll(Arrays.asList($$1));
      }

      return CommonComponents.joinLines($$0);
   }

   private void upload() {
      new Thread(
            () -> {
               File $$0 = null;
               RealmsClient $$1 = RealmsClient.create();

               try {
                  if (!UPLOAD_LOCK.tryLock(1L, TimeUnit.SECONDS)) {
                     this.status = Component.translatable("mco.upload.close.failure");
                  } else {
                     UploadInfo $$2 = null;

                     for (int $$3 = 0; $$3 < 20; $$3++) {
                        try {
                           if (this.cancelled) {
                              this.uploadCancelled();
                              return;
                           }

                           $$2 = $$1.requestUploadInfo(this.realmId, UploadTokenCache.get(this.realmId));
                           if ($$2 != null) {
                              break;
                           }
                        } catch (RetryCallException var18) {
                           Thread.sleep(var18.delaySeconds * 1000);
                        }
                     }

                     if ($$2 == null) {
                        this.status = Component.translatable("mco.upload.close.failure");
                     } else {
                        UploadTokenCache.put(this.realmId, $$2.getToken());
                        if (!$$2.isWorldClosed()) {
                           this.status = Component.translatable("mco.upload.close.failure");
                        } else if (this.cancelled) {
                           this.uploadCancelled();
                        } else {
                           File $$5 = new File(this.minecraft.gameDirectory.getAbsolutePath(), "saves");
                           $$0 = this.tarGzipArchive(new File($$5, this.selectedLevel.getLevelId()));
                           if (this.cancelled) {
                              this.uploadCancelled();
                           } else if (this.verify($$0)) {
                              this.status = Component.translatable("mco.upload.uploading", this.selectedLevel.getLevelName());
                              FileUpload $$10 = new FileUpload(
                                 $$0,
                                 this.realmId,
                                 this.slotId,
                                 $$2,
                                 this.minecraft.getUser(),
                                 SharedConstants.getCurrentVersion().getName(),
                                 this.selectedLevel.levelVersion().minecraftVersionName(),
                                 this.uploadStatus
                              );
                              $$10.upload(p_319370_ -> {
                                 if (p_319370_.statusCode >= 200 && p_319370_.statusCode < 300) {
                                    this.uploadFinished = true;
                                    this.status = Component.translatable("mco.upload.done");
                                    if (this.backButton != null) {
                                       this.backButton.setMessage(CommonComponents.GUI_DONE);
                                    }

                                    UploadTokenCache.invalidate(this.realmId);
                                 } else if (p_319370_.statusCode == 400 && p_319370_.errorMessage != null) {
                                    this.setErrorMessage(Component.translatable("mco.upload.failed", p_319370_.errorMessage));
                                 } else {
                                    this.setErrorMessage(Component.translatable("mco.upload.failed", p_319370_.statusCode));
                                 }
                              });

                              while (!$$10.isFinished()) {
                                 if (this.cancelled) {
                                    $$10.cancel();
                                    this.uploadCancelled();
                                    return;
                                 }

                                 try {
                                    Thread.sleep(500L);
                                 } catch (InterruptedException var17) {
                                    LOGGER.error("Failed to check Realms file upload status");
                                 }
                              }
                           } else {
                              long $$6 = $$0.length();
                              Unit $$7 = Unit.getLargest($$6);
                              Unit $$8 = Unit.getLargest(5368709120L);
                              if (Unit.humanReadable($$6, $$7).equals(Unit.humanReadable(5368709120L, $$8)) && $$7 != Unit.B) {
                                 Unit $$9 = Unit.values()[$$7.ordinal() - 1];
                                 this.setErrorMessage(
                                    Component.translatable("mco.upload.size.failure.line1", this.selectedLevel.getLevelName()),
                                    Component.translatable("mco.upload.size.failure.line2", Unit.humanReadable($$6, $$9), Unit.humanReadable(5368709120L, $$9))
                                 );
                              } else {
                                 this.setErrorMessage(
                                    Component.translatable("mco.upload.size.failure.line1", this.selectedLevel.getLevelName()),
                                    Component.translatable("mco.upload.size.failure.line2", Unit.humanReadable($$6, $$7), Unit.humanReadable(5368709120L, $$8))
                                 );
                              }
                           }
                        }
                     }
                  }
               } catch (IOException var19) {
                  this.setErrorMessage(Component.translatable("mco.upload.failed", var19.getMessage()));
               } catch (RealmsServiceException var20) {
                  this.setErrorMessage(Component.translatable("mco.upload.failed", var20.realmsError.errorMessage()));
               } catch (InterruptedException var21) {
                  LOGGER.error("Could not acquire upload lock");
               } finally {
                  this.uploadFinished = true;
                  if (UPLOAD_LOCK.isHeldByCurrentThread()) {
                     UPLOAD_LOCK.unlock();
                     this.showDots = false;
                     if (this.backButton != null) {
                        this.backButton.visible = true;
                     }

                     if (this.cancelButton != null) {
                        this.cancelButton.visible = false;
                     }

                     if ($$0 != null) {
                        LOGGER.debug("Deleting file {}", $$0.getAbsolutePath());
                        $$0.delete();
                     }
                  } else {
                     return;
                  }
               }
            }
         )
         .start();
   }

   private void setErrorMessage(Component... p_90113_) {
      this.errorMessage = p_90113_;
   }

   private void uploadCancelled() {
      this.status = Component.translatable("mco.upload.cancelled");
      LOGGER.debug("Upload was cancelled");
   }

   private boolean verify(File p_90106_) {
      return p_90106_.length() < 5368709120L;
   }

   private File tarGzipArchive(File p_90120_) throws IOException {
      TarArchiveOutputStream $$1 = null;

      File var4;
      try {
         File $$2 = File.createTempFile("realms-upload-file", ".tar.gz");
         $$1 = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream($$2)));
         $$1.setLongFileMode(3);
         this.addFileToTarGz($$1, p_90120_.getAbsolutePath(), "world", true);
         $$1.finish();
         var4 = $$2;
      } finally {
         if ($$1 != null) {
            $$1.close();
         }
      }

      return var4;
   }

   private void addFileToTarGz(TarArchiveOutputStream p_90108_, String p_90109_, String p_90110_, boolean p_90111_) throws IOException {
      if (!this.cancelled) {
         File $$4 = new File(p_90109_);
         String $$5 = p_90111_ ? p_90110_ : p_90110_ + $$4.getName();
         TarArchiveEntry $$6 = new TarArchiveEntry($$4, $$5);
         p_90108_.putArchiveEntry($$6);
         if ($$4.isFile()) {
            try (InputStream $$7 = new FileInputStream($$4)) {
               $$7.transferTo(p_90108_);
            }

            p_90108_.closeArchiveEntry();
         } else {
            p_90108_.closeArchiveEntry();
            File[] $$8 = $$4.listFiles();
            if ($$8 != null) {
               for (File $$9 : $$8) {
                  this.addFileToTarGz(p_90108_, $$9.getAbsolutePath(), $$5 + "/", false);
               }
            }
         }
      }
   }
}
