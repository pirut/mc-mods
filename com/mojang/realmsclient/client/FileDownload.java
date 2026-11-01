package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckReturnValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class FileDownload {
   private static final Logger LOGGER = LogUtils.getLogger();
   private volatile boolean cancelled;
   private volatile boolean finished;
   private volatile boolean error;
   private volatile boolean extracting;
   @Nullable
   private volatile File tempFile;
   private volatile File resourcePackPath;
   @Nullable
   private volatile CompletableFuture<?> pendingRequest;
   @Nullable
   private Thread currentThread;
   private static final String[] INVALID_FILE_NAMES = new String[]{
      "CON",
      "COM",
      "PRN",
      "AUX",
      "CLOCK$",
      "NUL",
      "COM1",
      "COM2",
      "COM3",
      "COM4",
      "COM5",
      "COM6",
      "COM7",
      "COM8",
      "COM9",
      "LPT1",
      "LPT2",
      "LPT3",
      "LPT4",
      "LPT5",
      "LPT6",
      "LPT7",
      "LPT8",
      "LPT9"
   };

   @Nullable
   private <T> T joinCancellableRequest(CompletableFuture<T> request) throws Throwable {
      this.pendingRequest = request;
      if (this.cancelled) {
         request.cancel(true);
         return null;
      } else {
         try {
            try {
               return request.join();
            } catch (CompletionException var3) {
               throw var3.getCause();
            }
         } catch (CancellationException var4) {
            return null;
         }
      }
   }

   private static HttpClient createClient() {
      return HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofMinutes(2L)).build();
   }

   private static Builder createRequest(String url) {
      return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2L));
   }

   @CheckReturnValue
   public static OptionalLong contentLength(String url) {
      try {
         OptionalLong var3;
         try (HttpClient httpClient = createClient()) {
            HttpResponse<Void> httpResponse = httpClient.send(createRequest(url).HEAD().build(), BodyHandlers.discarding());
            var3 = httpResponse.headers().firstValueAsLong("Content-Length");
         }

         return var3;
      } catch (Exception var6) {
         LOGGER.error("Unable to get content length for download");
         return OptionalLong.empty();
      }
   }

   public void download(WorldDownload download, String worldName, RealmsDownloadLatestWorldScreen.DownloadStatus status, LevelStorageSource source) {
      if (this.currentThread == null) {
         this.currentThread = new Thread(() -> {
            try (HttpClient httpClient = createClient()) {
               try {
                  this.tempFile = File.createTempFile("backup", ".tar.gz");
                  this.download(status, httpClient, download.downloadLink(), this.tempFile);
                  this.finishWorldDownload(worldName.trim(), this.tempFile, source, status);
               } catch (Exception var23) {
                  LOGGER.error("Caught exception while downloading world", var23);
                  this.error = true;
               } finally {
                  this.pendingRequest = null;
                  if (this.tempFile != null) {
                     this.tempFile.delete();
                  }

                  this.tempFile = null;
               }

               if (this.error) {
                  return;
               }

               String string2 = download.resourcePackUrl();
               if (!string2.isEmpty() && !download.resourcePackHash().isEmpty()) {
                  try {
                     this.tempFile = File.createTempFile("resources", ".tar.gz");
                     this.download(status, httpClient, string2, this.tempFile);
                     this.finishResourcePackDownload(status, this.tempFile, download);
                  } catch (Exception var22) {
                     LOGGER.error("Caught exception while downloading resource pack", var22);
                     this.error = true;
                  } finally {
                     this.pendingRequest = null;
                     if (this.tempFile != null) {
                        this.tempFile.delete();
                     }

                     this.tempFile = null;
                  }
               }

               this.finished = true;
            }
         });
         this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
         this.currentThread.start();
      }
   }

   private void download(RealmsDownloadLatestWorldScreen.DownloadStatus status, HttpClient client, String url, File tempFile) throws IOException {
      HttpRequest httpRequest = createRequest(url).GET().build();

      HttpResponse<InputStream> httpResponse;
      try {
         httpResponse = this.joinCancellableRequest(client.sendAsync(httpRequest, BodyHandlers.ofInputStream()));
      } catch (Error var14) {
         throw var14;
      } catch (Throwable var15) {
         LOGGER.error("Failed to download {}", url, var15);
         this.error = true;
         return;
      }

      if (httpResponse != null && !this.cancelled) {
         if (httpResponse.statusCode() != 200) {
            this.error = true;
         } else {
            status.totalBytes = httpResponse.headers().firstValueAsLong("Content-Length").orElse(0L);

            try (
               InputStream inputStream = httpResponse.body();
               OutputStream outputStream = new FileOutputStream(tempFile);
            ) {
               inputStream.transferTo(new FileDownload.DownloadCountingOutputStream(outputStream, status));
            }
         }
      }
   }

   public void cancel() {
      if (this.tempFile != null) {
         this.tempFile.delete();
         this.tempFile = null;
      }

      this.cancelled = true;
      CompletableFuture<?> completableFuture = this.pendingRequest;
      if (completableFuture != null) {
         completableFuture.cancel(true);
      }
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isError() {
      return this.error;
   }

   public boolean isExtracting() {
      return this.extracting;
   }

   public static String findAvailableFolderName(String folderName) {
      folderName = folderName.replaceAll("[\\./\"]", "_");

      for (String string : INVALID_FILE_NAMES) {
         if (folderName.equalsIgnoreCase(string)) {
            folderName = "_" + folderName + "_";
         }
      }

      return folderName;
   }

   private void untarGzipArchive(String worldName, @Nullable File tempFile, LevelStorageSource levelStorageSource) throws IOException {
      Pattern pattern = Pattern.compile(".*-([0-9]+)$");
      int i = 1;

      for (char c : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         worldName = worldName.replace(c, '_');
      }

      if (StringUtils.isEmpty(worldName)) {
         worldName = "Realm";
      }

      worldName = findAvailableFolderName(worldName);

      try {
         for (LevelStorageSource.LevelDirectory levelDirectory : levelStorageSource.findLevelCandidates()) {
            String string = levelDirectory.directoryName();
            if (string.toLowerCase(Locale.ROOT).startsWith(worldName.toLowerCase(Locale.ROOT))) {
               Matcher matcher = pattern.matcher(string);
               if (matcher.matches()) {
                  int j = Integer.parseInt(matcher.group(1));
                  if (j > i) {
                     i = j;
                  }
               } else {
                  i++;
               }
            }
         }
      } catch (Exception var43) {
         LOGGER.error("Error getting level list", var43);
         this.error = true;
         return;
      }

      String string2;
      if (levelStorageSource.isNewLevelIdAcceptable(worldName) && i <= 1) {
         string2 = worldName;
      } else {
         string2 = worldName + (i == 1 ? "" : "-" + i);
         if (!levelStorageSource.isNewLevelIdAcceptable(string2)) {
            boolean bl = false;

            while (!bl) {
               i++;
               string2 = worldName + (i == 1 ? "" : "-" + i);
               if (levelStorageSource.isNewLevelIdAcceptable(string2)) {
                  bl = true;
               }
            }
         }
      }

      TarArchiveInputStream tarArchiveInputStream = null;
      File file = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "saves");

      try {
         file.mkdir();
         tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))));

         for (TarArchiveEntry tarArchiveEntry = tarArchiveInputStream.getNextTarEntry();
            tarArchiveEntry != null;
            tarArchiveEntry = tarArchiveInputStream.getNextTarEntry()
         ) {
            File file2 = new File(file, tarArchiveEntry.getName().replace("world", string2));
            if (tarArchiveEntry.isDirectory()) {
               file2.mkdirs();
            } else {
               file2.createNewFile();

               try (FileOutputStream fileOutputStream = new FileOutputStream(file2)) {
                  IOUtils.copy(tarArchiveInputStream, fileOutputStream);
               }
            }
         }
      } catch (Exception var41) {
         LOGGER.error("Error extracting world", var41);
         this.error = true;
      } finally {
         if (tarArchiveInputStream != null) {
            tarArchiveInputStream.close();
         }

         if (tempFile != null) {
            tempFile.delete();
         }

         try (LevelStorageSource.LevelStorageAccess levelStorageAccess2 = levelStorageSource.validateAndCreateAccess(string2)) {
            levelStorageAccess2.renameAndDropPlayer(string2);
         } catch (NbtException | ReportedNbtException | IOException var39) {
            LOGGER.error("Failed to modify unpacked realms level {}", string2, var39);
         } catch (ContentValidationException var40) {
            LOGGER.warn("Failed to download file", var40);
         }

         this.resourcePackPath = new File(file, string2 + File.separator + "resources.zip");
      }
   }

   private void finishWorldDownload(String worldName, File tempFile, LevelStorageSource source, RealmsDownloadLatestWorldScreen.DownloadStatus status) {
      if (status.bytesWritten >= status.totalBytes && !this.cancelled && !this.error) {
         try {
            this.extracting = true;
            this.untarGzipArchive(worldName, tempFile, source);
         } catch (IOException var6) {
            LOGGER.error("Error extracting archive", var6);
            this.error = true;
         }
      }
   }

   private void finishResourcePackDownload(RealmsDownloadLatestWorldScreen.DownloadStatus status, File tempFile, WorldDownload worldDownload) {
      if (status.bytesWritten >= status.totalBytes && !this.cancelled) {
         try {
            String string = Hashing.sha1().hashBytes(Files.toByteArray(tempFile)).toString();
            if (string.equals(worldDownload.resourcePackHash())) {
               FileUtils.copyFile(tempFile, this.resourcePackPath);
               this.finished = true;
            } else {
               LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", worldDownload.resourcePackHash(), string);
               FileUtils.deleteQuietly(tempFile);
               this.error = true;
            }
         } catch (IOException var5) {
            LOGGER.error("Error copying resourcepack file: {}", var5.getMessage());
            this.error = true;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class DownloadCountingOutputStream extends CountingOutputStream {
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

      public DownloadCountingOutputStream(OutputStream parent, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus) {
         super(parent);
         this.downloadStatus = downloadStatus;
      }

      protected void afterWrite(int i) throws IOException {
         super.afterWrite(i);
         this.downloadStatus.bytesWritten = this.getByteCount();
      }
   }
}
