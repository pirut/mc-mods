package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileDownload {
   static final Logger LOGGER = LogUtils.getLogger();
   volatile boolean cancelled;
   volatile boolean finished;
   volatile boolean error;
   volatile boolean extracting;
   @Nullable
   private volatile File tempFile;
   volatile File resourcePackPath;
   @Nullable
   private volatile HttpGet request;
   @Nullable
   private Thread currentThread;
   private final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build();
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

   public long contentLength(String p_86990_) {
      CloseableHttpClient $$1 = null;
      HttpGet $$2 = null;

      long var5;
      try {
         $$2 = new HttpGet(p_86990_);
         $$1 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
         CloseableHttpResponse $$3 = $$1.execute($$2);
         return Long.parseLong($$3.getFirstHeader("Content-Length").getValue());
      } catch (Throwable var16) {
         LOGGER.error("Unable to get content length for download");
         var5 = 0L;
      } finally {
         if ($$2 != null) {
            $$2.releaseConnection();
         }

         if ($$1 != null) {
            try {
               $$1.close();
            } catch (IOException var15) {
               LOGGER.error("Could not close http client", var15);
            }
         }
      }

      return var5;
   }

   public void download(WorldDownload p_86983_, String p_86984_, RealmsDownloadLatestWorldScreen.DownloadStatus p_86985_, LevelStorageSource p_86986_) {
      if (this.currentThread == null) {
         this.currentThread = new Thread(() -> {
            CloseableHttpClient $$4 = null;

            try {
               this.tempFile = File.createTempFile("backup", ".tar.gz");
               this.request = new HttpGet(p_86983_.downloadLink);
               $$4 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
               HttpResponse $$5 = $$4.execute(this.request);
               p_86985_.totalBytes = Long.parseLong($$5.getFirstHeader("Content-Length").getValue());
               if ($$5.getStatusLine().getStatusCode() == 200) {
                  OutputStream $$12 = new FileOutputStream(this.tempFile);
                  FileDownload.ProgressListener $$13 = new FileDownload.ProgressListener(p_86984_.trim(), this.tempFile, p_86986_, p_86985_);
                  FileDownload.DownloadCountingOutputStream $$14 = new FileDownload.DownloadCountingOutputStream($$12);
                  $$14.setListener($$13);
                  IOUtils.copy($$5.getEntity().getContent(), $$14);
                  return;
               }

               this.error = true;
               this.request.abort();
            } catch (Exception var93) {
               LOGGER.error("Caught exception while downloading: {}", var93.getMessage());
               this.error = true;
               return;
            } finally {
               this.request.releaseConnection();
               if (this.tempFile != null) {
                  this.tempFile.delete();
               }

               if (!this.error) {
                  if (!p_86983_.resourcePackUrl.isEmpty() && !p_86983_.resourcePackHash.isEmpty()) {
                     try {
                        this.tempFile = File.createTempFile("resources", ".tar.gz");
                        this.request = new HttpGet(p_86983_.resourcePackUrl);
                        HttpResponse $$28 = $$4.execute(this.request);
                        p_86985_.totalBytes = Long.parseLong($$28.getFirstHeader("Content-Length").getValue());
                        if ($$28.getStatusLine().getStatusCode() != 200) {
                           this.error = true;
                           this.request.abort();
                           return;
                        }

                        OutputStream $$29 = new FileOutputStream(this.tempFile);
                        FileDownload.ResourcePackProgressListener $$30 = new FileDownload.ResourcePackProgressListener(this.tempFile, p_86985_, p_86983_);
                        FileDownload.DownloadCountingOutputStream $$31 = new FileDownload.DownloadCountingOutputStream($$29);
                        $$31.setListener($$30);
                        IOUtils.copy($$28.getEntity().getContent(), $$31);
                     } catch (Exception var91) {
                        LOGGER.error("Caught exception while downloading: {}", var91.getMessage());
                        this.error = true;
                     } finally {
                        this.request.releaseConnection();
                        if (this.tempFile != null) {
                           this.tempFile.delete();
                        }
                     }
                  } else {
                     this.finished = true;
                  }
               }

               if ($$4 != null) {
                  try {
                     $$4.close();
                  } catch (IOException var90) {
                     LOGGER.error("Failed to close Realms download client");
                  }
               }
            }
         });
         this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
         this.currentThread.start();
      }
   }

   public void cancel() {
      if (this.request != null) {
         this.request.abort();
      }

      if (this.tempFile != null) {
         this.tempFile.delete();
      }

      this.cancelled = true;
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

   public static String findAvailableFolderName(String p_87002_) {
      p_87002_ = p_87002_.replaceAll("[\\./\"]", "_");

      for (String $$1 : INVALID_FILE_NAMES) {
         if (p_87002_.equalsIgnoreCase($$1)) {
            p_87002_ = "_" + p_87002_ + "_";
         }
      }

      return p_87002_;
   }

   void untarGzipArchive(String p_86992_, @Nullable File p_86993_, LevelStorageSource p_86994_) throws IOException {
      Pattern $$3 = Pattern.compile(".*-([0-9]+)$");
      int $$4 = 1;

      for (char $$5 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         p_86992_ = p_86992_.replace($$5, '_');
      }

      if (StringUtils.isEmpty(p_86992_)) {
         p_86992_ = "Realm";
      }

      p_86992_ = findAvailableFolderName(p_86992_);

      try {
         for (LevelStorageSource.LevelDirectory $$6 : p_86994_.findLevelCandidates()) {
            String $$7 = $$6.directoryName();
            if ($$7.toLowerCase(Locale.ROOT).startsWith(p_86992_.toLowerCase(Locale.ROOT))) {
               Matcher $$8 = $$3.matcher($$7);
               if ($$8.matches()) {
                  int $$9 = Integer.parseInt($$8.group(1));
                  if ($$9 > $$4) {
                     $$4 = $$9;
                  }
               } else {
                  $$4++;
               }
            }
         }
      } catch (Exception var43) {
         LOGGER.error("Error getting level list", var43);
         this.error = true;
         return;
      }

      String $$13;
      if (p_86994_.isNewLevelIdAcceptable(p_86992_) && $$4 <= 1) {
         $$13 = p_86992_;
      } else {
         $$13 = p_86992_ + ($$4 == 1 ? "" : "-" + $$4);
         if (!p_86994_.isNewLevelIdAcceptable($$13)) {
            boolean $$12 = false;

            while (!$$12) {
               $$4++;
               $$13 = p_86992_ + ($$4 == 1 ? "" : "-" + $$4);
               if (p_86994_.isNewLevelIdAcceptable($$13)) {
                  $$12 = true;
               }
            }
         }
      }

      TarArchiveInputStream $$14 = null;
      File $$15 = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "saves");

      try {
         $$15.mkdir();
         $$14 = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(p_86993_))));

         for (TarArchiveEntry $$16 = $$14.getNextTarEntry(); $$16 != null; $$16 = $$14.getNextTarEntry()) {
            File $$17 = new File($$15, $$16.getName().replace("world", $$13));
            if ($$16.isDirectory()) {
               $$17.mkdirs();
            } else {
               $$17.createNewFile();

               try (FileOutputStream $$18 = new FileOutputStream($$17)) {
                  IOUtils.copy($$14, $$18);
               }
            }
         }
      } catch (Exception var41) {
         LOGGER.error("Error extracting world", var41);
         this.error = true;
      } finally {
         if ($$14 != null) {
            $$14.close();
         }

         if (p_86993_ != null) {
            p_86993_.delete();
         }

         try (LevelStorageSource.LevelStorageAccess $$26 = p_86994_.validateAndCreateAccess($$13)) {
            $$26.renameAndDropPlayer($$13);
         } catch (NbtException | ReportedNbtException | IOException var39) {
            LOGGER.error("Failed to modify unpacked realms level {}", $$13, var39);
         } catch (ContentValidationException var40) {
            LOGGER.warn("{}", var40.getMessage());
         }

         this.resourcePackPath = new File($$15, $$13 + File.separator + "resources.zip");
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DownloadCountingOutputStream extends CountingOutputStream {
      @Nullable
      private ActionListener listener;

      public DownloadCountingOutputStream(OutputStream p_193509_) {
         super(p_193509_);
      }

      public void setListener(ActionListener p_87017_) {
         this.listener = p_87017_;
      }

      protected void afterWrite(int p_87019_) throws IOException {
         super.afterWrite(p_87019_);
         if (this.listener != null) {
            this.listener.actionPerformed(new ActionEvent(this, 0, null));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ProgressListener implements ActionListener {
      private final String worldName;
      private final File tempFile;
      private final LevelStorageSource levelStorageSource;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

      ProgressListener(String p_87027_, File p_87028_, LevelStorageSource p_87029_, RealmsDownloadLatestWorldScreen.DownloadStatus p_87030_) {
         this.worldName = p_87027_;
         this.tempFile = p_87028_;
         this.levelStorageSource = p_87029_;
         this.downloadStatus = p_87030_;
      }

      @Override
      public void actionPerformed(ActionEvent p_87039_) {
         this.downloadStatus.bytesWritten = ((FileDownload.DownloadCountingOutputStream)p_87039_.getSource()).getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled && !FileDownload.this.error) {
            try {
               FileDownload.this.extracting = true;
               FileDownload.this.untarGzipArchive(this.worldName, this.tempFile, this.levelStorageSource);
            } catch (IOException var3) {
               FileDownload.LOGGER.error("Error extracting archive", var3);
               FileDownload.this.error = true;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ResourcePackProgressListener implements ActionListener {
      private final File tempFile;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
      private final WorldDownload worldDownload;

      ResourcePackProgressListener(File p_87046_, RealmsDownloadLatestWorldScreen.DownloadStatus p_87047_, WorldDownload p_87048_) {
         this.tempFile = p_87046_;
         this.downloadStatus = p_87047_;
         this.worldDownload = p_87048_;
      }

      @Override
      public void actionPerformed(ActionEvent p_87056_) {
         this.downloadStatus.bytesWritten = ((FileDownload.DownloadCountingOutputStream)p_87056_.getSource()).getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled) {
            try {
               String $$1 = Hashing.sha1().hashBytes(Files.toByteArray(this.tempFile)).toString();
               if ($$1.equals(this.worldDownload.resourcePackHash)) {
                  FileUtils.copyFile(this.tempFile, FileDownload.this.resourcePackPath);
                  FileDownload.this.finished = true;
               } else {
                  FileDownload.LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", this.worldDownload.resourcePackHash, $$1);
                  FileUtils.deleteQuietly(this.tempFile);
                  FileDownload.this.error = true;
               }
            } catch (IOException var3) {
               FileDownload.LOGGER.error("Error copying resourcepack file: {}", var3.getMessage());
               FileDownload.this.error = true;
            }
         }
      }
   }
}
