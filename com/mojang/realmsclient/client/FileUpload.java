package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileUpload implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_RETRIES = 5;
   private static final String UPLOAD_PATH = "/upload";
   private final File file;
   private final long realmId;
   private final int slotId;
   private final UploadInfo uploadInfo;
   private final String sessionId;
   private final String username;
   private final String clientVersion;
   private final String worldVersion;
   private final UploadStatus uploadStatus;
   private final HttpClient client;

   public FileUpload(File p_87071_, long p_87072_, int p_87073_, UploadInfo p_87074_, User p_87075_, String p_87076_, String p_325917_, UploadStatus p_87077_) {
      this.file = p_87071_;
      this.realmId = p_87072_;
      this.slotId = p_87073_;
      this.uploadInfo = p_87074_;
      this.sessionId = p_87075_.getSessionId();
      this.username = p_87075_.getName();
      this.clientVersion = p_87076_;
      this.worldVersion = p_325917_;
      this.uploadStatus = p_87077_;
      this.client = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(15L)).build();
   }

   @Override
   public void close() {
      this.client.close();
   }

   public CompletableFuture<UploadResult> startUpload() {
      long i = this.file.length();
      this.uploadStatus.setTotalBytes(i);
      return this.requestUpload(0, i);
   }

   private CompletableFuture<UploadResult> requestUpload(int p_87080_, long p_460718_) {
      BodyPublisher bodypublisher = inputStreamPublisherWithSize(() -> {
         try {
            return new FileUpload.UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
         } catch (IOException var2x) {
            LOGGER.warn("Failed to open file {}", this.file, var2x);
            return null;
         }
      }, p_460718_);
      HttpRequest httprequest = HttpRequest.newBuilder(this.uploadInfo.uploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId))
         .timeout(Duration.ofMinutes(10L))
         .setHeader("Cookie", this.uploadCookie())
         .setHeader("Content-Type", "application/octet-stream")
         .POST(bodypublisher)
         .build();
      return this.client.sendAsync(httprequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(p_460220_ -> {
         long i = this.getRetryDelaySeconds((HttpResponse<?>)p_460220_);
         if (this.shouldRetry(i, p_87080_)) {
            this.uploadStatus.restart();

            try {
               Thread.sleep(Duration.ofSeconds(i));
            } catch (InterruptedException var8) {
            }

            return this.requestUpload(p_87080_ + 1, p_460718_);
         } else {
            return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>)p_460220_));
         }
      });
   }

   private static BodyPublisher inputStreamPublisherWithSize(Supplier<InputStream> p_461095_, long p_460981_) {
      return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(p_461095_), p_460981_);
   }

   private String uploadCookie() {
      return "sid="
         + this.sessionId
         + ";token="
         + this.uploadInfo.token()
         + ";user="
         + this.username
         + ";version="
         + this.clientVersion
         + ";worldVersion="
         + this.worldVersion;
   }

   private UploadResult handleResponse(HttpResponse<String> p_461199_) {
      int i = p_461199_.statusCode();
      if (i == 401) {
         LOGGER.debug("Realms server returned 401: {}", p_461199_.headers().firstValue("WWW-Authenticate"));
      }

      String s = null;
      String s1 = p_461199_.body();
      if (s1 != null && !s1.isBlank()) {
         try {
            JsonElement jsonelement = LenientJsonParser.parse(s1).getAsJsonObject().get("errorMsg");
            if (jsonelement != null) {
               s = jsonelement.getAsString();
            }
         } catch (Exception var6) {
            LOGGER.warn("Failed to parse response {}", s1, var6);
         }
      }

      return new UploadResult(i, s);
   }

   private boolean shouldRetry(long p_87082_, int p_87083_) {
      return p_87082_ > 0L && p_87083_ + 1 < 5;
   }

   private long getRetryDelaySeconds(HttpResponse<?> p_461270_) {
      return p_461270_.headers().firstValueAsLong("Retry-After").orElse(0L);
   }

   @OnlyIn(Dist.CLIENT)
   static class UploadCountingInputStream extends CountingInputStream {
      private final UploadStatus uploadStatus;

      UploadCountingInputStream(InputStream p_460825_, UploadStatus p_461078_) {
         super(p_460825_);
         this.uploadStatus = p_461078_;
      }

      protected void afterRead(int p_460665_) throws IOException {
         super.afterRead(p_460665_);
         this.uploadStatus.onWrite(this.getByteCount());
      }
   }
}
