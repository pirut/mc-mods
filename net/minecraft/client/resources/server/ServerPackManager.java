package net.minecraft.client.resources.server;

import com.google.common.hash.HashCode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.packs.DownloadQueue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerPackManager {
   private final PackDownloader downloader;
   final PackLoadFeedback packLoadFeedback;
   private final PackReloadConfig reloadConfig;
   private final Runnable updateRequest;
   private ServerPackManager.PackPromptStatus packPromptStatus;
   final List<ServerPackManager.ServerPackData> packs = new ArrayList<>();

   public ServerPackManager(
      PackDownloader p_314651_, PackLoadFeedback p_314441_, PackReloadConfig p_314471_, Runnable p_314453_, ServerPackManager.PackPromptStatus p_314476_
   ) {
      this.downloader = p_314651_;
      this.packLoadFeedback = p_314441_;
      this.reloadConfig = p_314471_;
      this.updateRequest = p_314453_;
      this.packPromptStatus = p_314476_;
   }

   void registerForUpdate() {
      this.updateRequest.run();
   }

   private void markExistingPacksAsRemoved(UUID p_314529_) {
      for (ServerPackManager.ServerPackData $$1 : this.packs) {
         if ($$1.id.equals(p_314529_)) {
            $$1.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REPLACED);
         }
      }
   }

   public void pushPack(UUID p_314641_, URL p_314493_, @Nullable HashCode p_314571_) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(p_314641_, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         this.pushNewPack(p_314641_, new ServerPackManager.ServerPackData(p_314641_, p_314493_, p_314571_));
      }
   }

   public void pushLocalPack(UUID p_314448_, Path p_314520_) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(p_314448_, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         URL $$2;
         try {
            $$2 = p_314520_.toUri().toURL();
         } catch (MalformedURLException var5) {
            throw new IllegalStateException("Can't convert path to URL " + p_314520_, var5);
         }

         ServerPackManager.ServerPackData $$5 = new ServerPackManager.ServerPackData(p_314448_, $$2, null);
         $$5.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
         $$5.path = p_314520_;
         this.pushNewPack(p_314448_, $$5);
      }
   }

   private void pushNewPack(UUID p_314541_, ServerPackManager.ServerPackData p_314473_) {
      this.markExistingPacksAsRemoved(p_314541_);
      this.packs.add(p_314473_);
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.ALLOWED) {
         this.acceptPack(p_314473_);
      }

      this.registerForUpdate();
   }

   private void acceptPack(ServerPackManager.ServerPackData p_314599_) {
      this.packLoadFeedback.reportUpdate(p_314599_.id, PackLoadFeedback.Update.ACCEPTED);
      p_314599_.promptAccepted = true;
   }

   @Nullable
   private ServerPackManager.ServerPackData findPackInfo(UUID p_314517_) {
      for (ServerPackManager.ServerPackData $$1 : this.packs) {
         if (!$$1.isRemoved() && $$1.id.equals(p_314517_)) {
            return $$1;
         }
      }

      return null;
   }

   public void popPack(UUID p_314547_) {
      ServerPackManager.ServerPackData $$1 = this.findPackInfo(p_314547_);
      if ($$1 != null) {
         $$1.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
         this.registerForUpdate();
      }
   }

   public void popAll() {
      for (ServerPackManager.ServerPackData $$0 : this.packs) {
         $$0.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
      }

      this.registerForUpdate();
   }

   public void allowServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;

      for (ServerPackManager.ServerPackData $$0 : this.packs) {
         if (!$$0.promptAccepted && !$$0.isRemoved()) {
            this.acceptPack($$0);
         }
      }

      this.registerForUpdate();
   }

   public void rejectServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.DECLINED;

      for (ServerPackManager.ServerPackData $$0 : this.packs) {
         if (!$$0.promptAccepted) {
            $$0.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DECLINED);
         }
      }

      this.registerForUpdate();
   }

   public void resetPromptStatus() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.PENDING;
   }

   public void tick() {
      boolean $$0 = this.updateDownloads();
      if (!$$0) {
         this.triggerReloadIfNeeded();
      }

      this.cleanupRemovedPacks();
   }

   private void cleanupRemovedPacks() {
      this.packs.removeIf(p_314901_ -> {
         if (p_314901_.activationStatus != ServerPackManager.ActivationStatus.INACTIVE) {
            return false;
         } else if (p_314901_.removalReason != null) {
            PackLoadFeedback.FinalResult $$1 = p_314901_.removalReason.serverResponse;
            if ($$1 != null) {
               this.packLoadFeedback.reportFinalResult(p_314901_.id, $$1);
            }

            return true;
         } else {
            return false;
         }
      });
   }

   private void onDownload(Collection<ServerPackManager.ServerPackData> p_314562_, DownloadQueue.BatchResult p_314549_) {
      if (!p_314549_.failed().isEmpty()) {
         for (ServerPackManager.ServerPackData $$2 : this.packs) {
            if ($$2.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               if (p_314549_.failed().contains($$2.id)) {
                  $$2.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DOWNLOAD_FAILED);
               } else {
                  $$2.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
               }
            }
         }
      }

      for (ServerPackManager.ServerPackData $$3 : p_314562_) {
         Path $$4 = p_314549_.downloaded().get($$3.id);
         if ($$4 != null) {
            $$3.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
            $$3.path = $$4;
            if (!$$3.isRemoved()) {
               this.packLoadFeedback.reportUpdate($$3.id, PackLoadFeedback.Update.DOWNLOADED);
            }
         }
      }

      this.registerForUpdate();
   }

   private boolean updateDownloads() {
      List<ServerPackManager.ServerPackData> $$0 = new ArrayList<>();
      boolean $$1 = false;

      for (ServerPackManager.ServerPackData $$2 : this.packs) {
         if (!$$2.isRemoved() && $$2.promptAccepted) {
            if ($$2.downloadStatus != ServerPackManager.PackDownloadStatus.DONE) {
               $$1 = true;
            }

            if ($$2.downloadStatus == ServerPackManager.PackDownloadStatus.REQUESTED) {
               $$2.downloadStatus = ServerPackManager.PackDownloadStatus.PENDING;
               $$0.add($$2);
            }
         }
      }

      if (!$$0.isEmpty()) {
         Map<UUID, DownloadQueue.DownloadRequest> $$3 = new HashMap<>();

         for (ServerPackManager.ServerPackData $$4 : $$0) {
            $$3.put($$4.id, new DownloadQueue.DownloadRequest($$4.url, $$4.hash));
         }

         this.downloader.download($$3, p_314466_ -> this.onDownload($$0, p_314466_));
      }

      return $$1;
   }

   private void triggerReloadIfNeeded() {
      boolean $$0 = false;
      final List<ServerPackManager.ServerPackData> $$1 = new ArrayList<>();
      final List<ServerPackManager.ServerPackData> $$2 = new ArrayList<>();

      for (ServerPackManager.ServerPackData $$3 : this.packs) {
         if ($$3.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
            return;
         }

         boolean $$4 = $$3.promptAccepted && $$3.downloadStatus == ServerPackManager.PackDownloadStatus.DONE && !$$3.isRemoved();
         if ($$4 && $$3.activationStatus == ServerPackManager.ActivationStatus.INACTIVE) {
            $$1.add($$3);
            $$0 = true;
         }

         if ($$3.activationStatus == ServerPackManager.ActivationStatus.ACTIVE) {
            if (!$$4) {
               $$0 = true;
               $$2.add($$3);
            } else {
               $$1.add($$3);
            }
         }
      }

      if ($$0) {
         for (ServerPackManager.ServerPackData $$5 : $$1) {
            if ($$5.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               $$5.activationStatus = ServerPackManager.ActivationStatus.PENDING;
            }
         }

         for (ServerPackManager.ServerPackData $$6 : $$2) {
            $$6.activationStatus = ServerPackManager.ActivationStatus.PENDING;
         }

         this.reloadConfig.scheduleReload(new PackReloadConfig.Callbacks() {
            @Override
            public void onSuccess() {
               for (ServerPackManager.ServerPackData $$0x : $$1) {
                  $$0x.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                  if ($$0x.removalReason == null) {
                     ServerPackManager.this.packLoadFeedback.reportFinalResult($$0x.id, PackLoadFeedback.FinalResult.APPLIED);
                  }
               }

               for (ServerPackManager.ServerPackData $$1x : $$2) {
                  $$1x.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
               }

               ServerPackManager.this.registerForUpdate();
            }

            @Override
            public void onFailure(boolean p_314558_) {
               if (!p_314558_) {
                  $$1.clear();

                  for (ServerPackManager.ServerPackData $$1x : ServerPackManager.this.packs) {
                     switch ($$1x.activationStatus) {
                        case INACTIVE:
                           $$1x.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
                           break;
                        case PENDING:
                           $$1x.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                           $$1x.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.ACTIVATION_FAILED);
                           break;
                        case ACTIVE:
                           $$1.add($$1x);
                     }
                  }

                  ServerPackManager.this.registerForUpdate();
               } else {
                  for (ServerPackManager.ServerPackData $$2x : ServerPackManager.this.packs) {
                     if ($$2x.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
                        $$2x.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                     }
                  }
               }
            }

            @Override
            public List<PackReloadConfig.IdAndPath> packsToLoad() {
               return $$1.stream().map(p_314577_ -> new PackReloadConfig.IdAndPath(p_314577_.id, p_314577_.path)).toList();
            }
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum ActivationStatus {
      INACTIVE,
      PENDING,
      ACTIVE;
   }

   @OnlyIn(Dist.CLIENT)
   static enum PackDownloadStatus {
      REQUESTED,
      PENDING,
      DONE;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum PackPromptStatus {
      PENDING,
      ALLOWED,
      DECLINED;
   }

   @OnlyIn(Dist.CLIENT)
   static enum RemovalReason {
      DOWNLOAD_FAILED(PackLoadFeedback.FinalResult.DOWNLOAD_FAILED),
      ACTIVATION_FAILED(PackLoadFeedback.FinalResult.ACTIVATION_FAILED),
      DECLINED(PackLoadFeedback.FinalResult.DECLINED),
      DISCARDED(PackLoadFeedback.FinalResult.DISCARDED),
      SERVER_REMOVED(null),
      SERVER_REPLACED(null);

      @Nullable
      final PackLoadFeedback.FinalResult serverResponse;

      private RemovalReason(@Nullable PackLoadFeedback.FinalResult p_314987_) {
         this.serverResponse = p_314987_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ServerPackData {
      final UUID id;
      final URL url;
      @Nullable
      final HashCode hash;
      @Nullable
      Path path;
      @Nullable
      ServerPackManager.RemovalReason removalReason;
      ServerPackManager.PackDownloadStatus downloadStatus = ServerPackManager.PackDownloadStatus.REQUESTED;
      ServerPackManager.ActivationStatus activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
      boolean promptAccepted;

      ServerPackData(UUID p_314450_, URL p_314542_, @Nullable HashCode p_314445_) {
         this.id = p_314450_;
         this.url = p_314542_;
         this.hash = p_314445_;
      }

      public void setRemovalReasonIfNotSet(ServerPackManager.RemovalReason p_314504_) {
         if (this.removalReason == null) {
            this.removalReason = p_314504_;
         }
      }

      public boolean isRemoved() {
         return this.removalReason != null;
      }
   }
}
