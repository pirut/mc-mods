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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
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

   public FileUpload(
      File file, long realmId, int slotId, UploadInfo uploadInfo, User user, String clientVersion, String worldVersion, UploadStatus uploadStatus
   ) {
      this.file = file;
      this.realmId = realmId;
      this.slotId = slotId;
      this.uploadInfo = uploadInfo;
      this.sessionId = user.getSessionId();
      this.username = user.getName();
      this.clientVersion = clientVersion;
      this.worldVersion = worldVersion;
      this.uploadStatus = uploadStatus;
      this.client = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(15L)).build();
   }

   @Override
   public void close() {
      this.client.close();
   }

   public CompletableFuture<UploadResult> startUpload() {
      long l = this.file.length();
      this.uploadStatus.setTotalBytes(l);
      return this.requestUpload(0, l);
   }

   private CompletableFuture<UploadResult> requestUpload(int retries, long size) {
      BodyPublisher bodyPublisher = inputStreamPublisherWithSize(() -> {
         try {
            return new FileUpload.UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
         } catch (IOException var2) {
            LOGGER.warn("Failed to open file {}", this.file, var2);
            return null;
         }
      }, size);
      HttpRequest httpRequest = HttpRequest.newBuilder(this.uploadInfo.uploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId))
         .timeout(Duration.ofMinutes(10L))
         .setHeader("Cookie", this.uploadCookie())
         .setHeader("Content-Type", "application/octet-stream")
         .POST(bodyPublisher)
         .build();
      return this.client.sendAsync(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(httpResponse -> {
         long m = this.getRetryDelaySeconds((HttpResponse<?>)httpResponse);
         if (this.shouldRetry(m, retries)) {
            this.uploadStatus.restart();

            try {
               Thread.sleep(Duration.ofSeconds(m));
            } catch (InterruptedException var8) {
            }

            return this.requestUpload(retries + 1, size);
         } else {
            return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>)httpResponse));
         }
      });
   }

   private static BodyPublisher inputStreamPublisherWithSize(Supplier<InputStream> streamSupplier, long size) {
      return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(streamSupplier), size);
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

   private UploadResult handleResponse(HttpResponse<String> response) {
      int i = response.statusCode();
      if (i == 401) {
         LOGGER.debug("Realms server returned 401: {}", response.headers().firstValue("WWW-Authenticate"));
      }

      String string = null;
      String string2 = response.body();
      if (string2 != null && !string2.isBlank()) {
         try {
            JsonElement jsonElement = LenientJsonParser.parse(string2).getAsJsonObject().get("errorMsg");
            if (jsonElement != null) {
               string = jsonElement.getAsString();
            }
         } catch (Exception var6) {
            LOGGER.warn("Failed to parse response {}", string2, var6);
         }
      }

      return new UploadResult(i, string);
   }

   private boolean shouldRetry(long retryDelaySeconds, int retries) {
      return retryDelaySeconds > 0L && retries + 1 < 5;
   }

   private long getRetryDelaySeconds(HttpResponse<?> response) {
      return response.headers().firstValueAsLong("Retry-After").orElse(0L);
   }

   @Environment(EnvType.CLIENT)
   static class UploadCountingInputStream extends CountingInputStream {
      private final UploadStatus uploadStatus;

      UploadCountingInputStream(InputStream parent, UploadStatus uploadStatus) {
         super(parent);
         this.uploadStatus = uploadStatus;
      }

      protected void afterRead(int i) throws IOException {
         super.afterRead(i);
         this.uploadStatus.onWrite(this.getByteCount());
      }
   }
}
