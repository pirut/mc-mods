package net.minecraft.client.resources.server;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.Unit;
import com.mojang.util.UndashedUuid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.main.GameConfig;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.packs.DownloadQueue;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.HttpUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class DownloadedPackSource implements AutoCloseable {
   private static final Component SERVER_NAME = Component.translatable("resourcePack.server.name");
   private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final RepositorySource EMPTY_SOURCE = consumer -> {};
   private static final PackSelectionConfig DOWNLOADED_PACK_SELECTION = new PackSelectionConfig(true, Pack.Position.TOP, true);
   private static final PackLoadFeedback LOG_ONLY_FEEDBACK = new PackLoadFeedback() {
      @Override
      public void reportUpdate(UUID id, PackLoadFeedback.Update update) {
         DownloadedPackSource.LOGGER.debug("Downloaded pack {} changed state to {}", id, update);
      }

      @Override
      public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
         DownloadedPackSource.LOGGER.debug("Downloaded pack {} finished with state {}", id, result);
      }
   };
   final Minecraft minecraft;
   private RepositorySource packSource = EMPTY_SOURCE;
   @Nullable
   private PackReloadConfig.Callbacks pendingReload;
   final ServerPackManager manager;
   private final DownloadQueue downloadQueue;
   private PackSource packType = PackSource.SERVER;
   PackLoadFeedback packFeedback = LOG_ONLY_FEEDBACK;
   private int packIdSerialNumber;

   public DownloadedPackSource(Minecraft minecraft, Path directory, GameConfig.UserData userData) {
      this.minecraft = minecraft;

      try {
         this.downloadQueue = new DownloadQueue(directory);
      } catch (IOException var5) {
         throw new UncheckedIOException("Failed to open download queue in directory " + directory, var5);
      }

      Executor executor = minecraft::schedule;
      this.manager = new ServerPackManager(this.createDownloader(this.downloadQueue, executor, userData.user, userData.proxy), new PackLoadFeedback() {
         @Override
         public void reportUpdate(UUID id, PackLoadFeedback.Update update) {
            DownloadedPackSource.this.packFeedback.reportUpdate(id, update);
         }

         @Override
         public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
            DownloadedPackSource.this.packFeedback.reportFinalResult(id, result);
         }
      }, this.createReloadConfig(), this.createUpdateScheduler(executor), ServerPackManager.PackPromptStatus.PENDING);
   }

   HttpUtil.DownloadProgressListener createDownloadNotifier(int packCount) {
      return new HttpUtil.DownloadProgressListener() {
         private final SystemToast.SystemToastId toastId = new SystemToast.SystemToastId();
         private Component title = Component.empty();
         @Nullable
         private Component message = null;
         private int count;
         private int failCount;
         private OptionalLong totalBytes = OptionalLong.empty();

         private void updateToast() {
            DownloadedPackSource.this.minecraft
               .execute(() -> SystemToast.addOrUpdate(DownloadedPackSource.this.minecraft.getToastManager(), this.toastId, this.title, this.message));
         }

         private void updateProgress(long progress) {
            if (this.totalBytes.isPresent()) {
               this.message = Component.translatable("download.pack.progress.percent", progress * 100L / this.totalBytes.getAsLong());
            } else {
               this.message = Component.translatable("download.pack.progress.bytes", Unit.humanReadable(progress));
            }

            this.updateToast();
         }

         @Override
         public void requestStart() {
            this.count++;
            this.title = Component.translatable("download.pack.title", this.count, packCount);
            this.updateToast();
            DownloadedPackSource.LOGGER.debug("Starting pack {}/{} download", this.count, packCount);
         }

         @Override
         public void downloadStart(OptionalLong totalSize) {
            DownloadedPackSource.LOGGER.debug("File size = {} bytes", totalSize);
            this.totalBytes = totalSize;
            this.updateProgress(0L);
         }

         @Override
         public void downloadedBytes(long progress) {
            DownloadedPackSource.LOGGER.debug("Progress for pack {}: {} bytes", this.count, progress);
            this.updateProgress(progress);
         }

         @Override
         public void requestFinished(boolean success) {
            if (!success) {
               DownloadedPackSource.LOGGER.info("Pack {} failed to download", this.count);
               this.failCount++;
            } else {
               DownloadedPackSource.LOGGER.debug("Download ended for pack {}", this.count);
            }

            if (this.count == packCount) {
               if (this.failCount > 0) {
                  this.title = Component.translatable("download.pack.failed", this.failCount, packCount);
                  this.message = null;
                  this.updateToast();
               } else {
                  SystemToast.forceHide(DownloadedPackSource.this.minecraft.getToastManager(), this.toastId);
               }
            }
         }
      };
   }

   private PackDownloader createDownloader(DownloadQueue downloadQueue, Executor executor, User user, Proxy proxy) {
      return new PackDownloader() {
         private static final int MAX_PACK_SIZE_BYTES = 262144000;
         private static final HashFunction CACHE_HASHING_FUNCTION = Hashing.sha1();

         private Map<String, String> createDownloadHeaders() {
            WorldVersion worldVersion = SharedConstants.getCurrentVersion();
            return Map.of(
               "X-Minecraft-Username",
               user.getName(),
               "X-Minecraft-UUID",
               UndashedUuid.toString(user.getProfileId()),
               "X-Minecraft-Version",
               worldVersion.name(),
               "X-Minecraft-Version-ID",
               worldVersion.id(),
               "X-Minecraft-Pack-Format",
               String.valueOf(worldVersion.packVersion(PackType.CLIENT_RESOURCES)),
               "User-Agent",
               "Minecraft Java/" + worldVersion.name()
            );
         }

         @Override
         public void download(Map<UUID, DownloadQueue.DownloadRequest> packs, Consumer<DownloadQueue.BatchResult> resultConsumer) {
            downloadQueue.downloadBatch(
                  new DownloadQueue.BatchConfig(
                     CACHE_HASHING_FUNCTION, 262144000, this.createDownloadHeaders(), proxy, DownloadedPackSource.this.createDownloadNotifier(packs.size())
                  ),
                  packs
               )
               .thenAcceptAsync(resultConsumer, executor);
         }
      };
   }

   private Runnable createUpdateScheduler(Executor executor) {
      return new Runnable() {
         private boolean scheduledInMainExecutor;
         private boolean hasUpdates;

         @Override
         public void run() {
            this.hasUpdates = true;
            if (!this.scheduledInMainExecutor) {
               this.scheduledInMainExecutor = true;
               executor.execute(this::runAllUpdates);
            }
         }

         private void runAllUpdates() {
            while (this.hasUpdates) {
               this.hasUpdates = false;
               DownloadedPackSource.this.manager.tick();
            }

            this.scheduledInMainExecutor = false;
         }
      };
   }

   private PackReloadConfig createReloadConfig() {
      return this::startReload;
   }

   @Nullable
   private List<Pack> loadRequestedPacks(List<PackReloadConfig.IdAndPath> packs) {
      List<Pack> list = new ArrayList<>(packs.size());

      for (PackReloadConfig.IdAndPath idAndPath : Lists.reverse(packs)) {
         String string = String.format(Locale.ROOT, "server/%08X/%s", this.packIdSerialNumber++, idAndPath.id());
         Path path = idAndPath.path();
         PackLocationInfo packLocationInfo = new PackLocationInfo(string, SERVER_NAME, this.packType, Optional.empty());
         Pack.ResourcesSupplier resourcesSupplier = new FilePackResources.FileResourcesSupplier(path);
         PackFormat packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
         Pack.Metadata metadata = Pack.readPackMetadata(packLocationInfo, resourcesSupplier, packFormat, PackType.CLIENT_RESOURCES);
         if (metadata == null) {
            LOGGER.warn("Invalid pack metadata in {}, ignoring all", path);
            return null;
         }

         list.add(new Pack(packLocationInfo, resourcesSupplier, metadata, DOWNLOADED_PACK_SELECTION));
      }

      return list;
   }

   public RepositorySource createRepositorySource() {
      return consumer -> this.packSource.loadPacks(consumer);
   }

   private static RepositorySource configureSource(List<Pack> packs) {
      return packs.isEmpty() ? EMPTY_SOURCE : packs::forEach;
   }

   private void startReload(PackReloadConfig.Callbacks callbacks) {
      this.pendingReload = callbacks;
      List<PackReloadConfig.IdAndPath> list = callbacks.packsToLoad();
      List<Pack> list2 = this.loadRequestedPacks(list);
      if (list2 == null) {
         callbacks.onFailure(false);
         List<PackReloadConfig.IdAndPath> list3 = callbacks.packsToLoad();
         list2 = this.loadRequestedPacks(list3);
         if (list2 == null) {
            LOGGER.warn("Double failure in loading server packs");
            list2 = List.of();
         }
      }

      this.packSource = configureSource(list2);
      this.minecraft.reloadResourcePacks();
   }

   public void onRecovery() {
      if (this.pendingReload != null) {
         this.pendingReload.onFailure(false);
         List<Pack> list = this.loadRequestedPacks(this.pendingReload.packsToLoad());
         if (list == null) {
            LOGGER.warn("Double failure in loading server packs");
            list = List.of();
         }

         this.packSource = configureSource(list);
      }
   }

   public void onRecoveryFailure() {
      if (this.pendingReload != null) {
         this.pendingReload.onFailure(true);
         this.pendingReload = null;
         this.packSource = EMPTY_SOURCE;
      }
   }

   public void onReloadSuccess() {
      if (this.pendingReload != null) {
         this.pendingReload.onSuccess();
         this.pendingReload = null;
      }
   }

   @Nullable
   private static HashCode tryParseSha1Hash(@Nullable String hash) {
      return hash != null && SHA1.matcher(hash).matches() ? HashCode.fromString(hash.toLowerCase(Locale.ROOT)) : null;
   }

   public void pushPack(UUID uuid, URL url, @Nullable String hash) {
      HashCode hashCode = tryParseSha1Hash(hash);
      this.manager.pushPack(uuid, url, hashCode);
   }

   public void pushLocalPack(UUID uuid, Path path) {
      this.manager.pushLocalPack(uuid, path);
   }

   public void popPack(UUID uuid) {
      this.manager.popPack(uuid);
   }

   public void popAll() {
      this.manager.popAll();
   }

   private static PackLoadFeedback createPackResponseSender(Connection connection) {
      return new PackLoadFeedback() {
         @Override
         public void reportUpdate(UUID id, PackLoadFeedback.Update update) {
            DownloadedPackSource.LOGGER.debug("Pack {} changed status to {}", id, update);

            ServerboundResourcePackPacket.Action action = switch (update) {
               case ACCEPTED -> ServerboundResourcePackPacket.Action.ACCEPTED;
               case DOWNLOADED -> ServerboundResourcePackPacket.Action.DOWNLOADED;
            };
            connection.send(new ServerboundResourcePackPacket(id, action));
         }

         @Override
         public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
            DownloadedPackSource.LOGGER.debug("Pack {} changed status to {}", id, result);

            ServerboundResourcePackPacket.Action action = switch (result) {
               case APPLIED -> ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED;
               case DOWNLOAD_FAILED -> ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD;
               case DECLINED -> ServerboundResourcePackPacket.Action.DECLINED;
               case DISCARDED -> ServerboundResourcePackPacket.Action.DISCARDED;
               case ACTIVATION_FAILED -> ServerboundResourcePackPacket.Action.FAILED_RELOAD;
            };
            connection.send(new ServerboundResourcePackPacket(id, action));
         }
      };
   }

   public void configureForServerControl(Connection connection, ServerPackManager.PackPromptStatus packPromptStatus) {
      this.packType = PackSource.SERVER;
      this.packFeedback = createPackResponseSender(connection);
      switch (packPromptStatus) {
         case ALLOWED:
            this.manager.allowServerPacks();
            break;
         case DECLINED:
            this.manager.rejectServerPacks();
            break;
         case PENDING:
            this.manager.resetPromptStatus();
      }
   }

   public void configureForLocalWorld() {
      this.packType = PackSource.WORLD;
      this.packFeedback = LOG_ONLY_FEEDBACK;
      this.manager.allowServerPacks();
   }

   public void allowServerPacks() {
      this.manager.allowServerPacks();
   }

   public void rejectServerPacks() {
      this.manager.rejectServerPacks();
   }

   public CompletableFuture<Void> waitForPackFeedback(UUID uuid) {
      final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
      final PackLoadFeedback packLoadFeedback = this.packFeedback;
      this.packFeedback = new PackLoadFeedback() {
         @Override
         public void reportUpdate(UUID id, PackLoadFeedback.Update update) {
            packLoadFeedback.reportUpdate(id, update);
         }

         @Override
         public void reportFinalResult(UUID id, PackLoadFeedback.FinalResult result) {
            if (uuid.equals(id)) {
               DownloadedPackSource.this.packFeedback = packLoadFeedback;
               if (result == PackLoadFeedback.FinalResult.APPLIED) {
                  completableFuture.complete(null);
               } else {
                  completableFuture.completeExceptionally(new IllegalStateException("Failed to apply pack " + id + ", reason: " + result));
               }
            }

            packLoadFeedback.reportFinalResult(id, result);
         }
      };
      return completableFuture;
   }

   public void cleanupAfterDisconnect() {
      this.manager.popAll();
      this.packFeedback = LOG_ONLY_FEEDBACK;
      this.manager.resetPromptStatus();
   }

   @Override
   public void close() throws IOException {
      this.downloadQueue.close();
   }
}
