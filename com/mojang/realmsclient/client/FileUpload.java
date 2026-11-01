package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.User;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileUpload {
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
   private final AtomicBoolean cancelled = new AtomicBoolean(false);
   @Nullable
   private CompletableFuture<UploadResult> uploadTask;
   private final RequestConfig requestConfig = RequestConfig.custom()
      .setSocketTimeout((int)TimeUnit.MINUTES.toMillis(10L))
      .setConnectTimeout((int)TimeUnit.SECONDS.toMillis(15L))
      .build();

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
   }

   public void upload(Consumer<UploadResult> p_87085_) {
      if (this.uploadTask == null) {
         this.uploadTask = CompletableFuture.supplyAsync(() -> this.requestUpload(0));
         this.uploadTask.thenAccept(p_87085_);
      }
   }

   public void cancel() {
      this.cancelled.set(true);
      if (this.uploadTask != null) {
         this.uploadTask.cancel(false);
         this.uploadTask = null;
      }
   }

   private UploadResult requestUpload(int p_87080_) {
      UploadResult.Builder $$1 = new UploadResult.Builder();
      if (this.cancelled.get()) {
         return $$1.build();
      } else {
         this.uploadStatus.totalBytes = this.file.length();
         HttpPost $$2 = new HttpPost(this.uploadInfo.getUploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId));
         CloseableHttpClient $$3 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();

         UploadResult var8;
         try {
            this.setupRequest($$2);
            HttpResponse $$4 = $$3.execute($$2);
            long $$5 = this.getRetryDelaySeconds($$4);
            if (!this.shouldRetry($$5, p_87080_)) {
               this.handleResponse($$4, $$1);
               return $$1.build();
            }

            var8 = this.retryUploadAfter($$5, p_87080_);
         } catch (Exception var12) {
            if (!this.cancelled.get()) {
               LOGGER.error("Caught exception while uploading: ", var12);
            }

            return $$1.build();
         } finally {
            this.cleanup($$2, $$3);
         }

         return var8;
      }
   }

   private void cleanup(HttpPost p_87094_, @Nullable CloseableHttpClient p_87095_) {
      p_87094_.releaseConnection();
      if (p_87095_ != null) {
         try {
            p_87095_.close();
         } catch (IOException var4) {
            LOGGER.error("Failed to close Realms upload client");
         }
      }
   }

   private void setupRequest(HttpPost p_87092_) throws FileNotFoundException {
      p_87092_.setHeader(
         "Cookie",
         "sid="
            + this.sessionId
            + ";token="
            + this.uploadInfo.getToken()
            + ";user="
            + this.username
            + ";version="
            + this.clientVersion
            + ";worldVersion="
            + this.worldVersion
      );
      FileUpload.CustomInputStreamEntity $$1 = new FileUpload.CustomInputStreamEntity(new FileInputStream(this.file), this.file.length(), this.uploadStatus);
      $$1.setContentType("application/octet-stream");
      p_87092_.setEntity($$1);
   }

   private void handleResponse(HttpResponse p_87089_, UploadResult.Builder p_87090_) throws IOException {
      int $$2 = p_87089_.getStatusLine().getStatusCode();
      if ($$2 == 401) {
         LOGGER.debug("Realms server returned 401: {}", p_87089_.getFirstHeader("WWW-Authenticate"));
      }

      p_87090_.withStatusCode($$2);
      if (p_87089_.getEntity() != null) {
         String $$3 = EntityUtils.toString(p_87089_.getEntity(), "UTF-8");
         if ($$3 != null) {
            try {
               JsonParser $$4 = new JsonParser();
               JsonElement $$5 = $$4.parse($$3).getAsJsonObject().get("errorMsg");
               Optional<String> $$6 = Optional.ofNullable($$5).map(JsonElement::getAsString);
               p_87090_.withErrorMessage($$6.orElse(null));
            } catch (Exception var8) {
            }
         }
      }
   }

   private boolean shouldRetry(long p_87082_, int p_87083_) {
      return p_87082_ > 0L && p_87083_ + 1 < 5;
   }

   private UploadResult retryUploadAfter(long p_87098_, int p_87099_) throws InterruptedException {
      Thread.sleep(Duration.ofSeconds(p_87098_).toMillis());
      return this.requestUpload(p_87099_ + 1);
   }

   private long getRetryDelaySeconds(HttpResponse p_87087_) {
      return Optional.ofNullable(p_87087_.getFirstHeader("Retry-After")).<String>map(NameValuePair::getValue).map(Long::valueOf).orElse(0L);
   }

   public boolean isFinished() {
      return this.uploadTask.isDone() || this.uploadTask.isCancelled();
   }

   @OnlyIn(Dist.CLIENT)
   static class CustomInputStreamEntity extends InputStreamEntity {
      private final long length;
      private final InputStream content;
      private final UploadStatus uploadStatus;

      public CustomInputStreamEntity(InputStream p_87105_, long p_87106_, UploadStatus p_87107_) {
         super(p_87105_);
         this.content = p_87105_;
         this.length = p_87106_;
         this.uploadStatus = p_87107_;
      }

      public void writeTo(OutputStream p_87109_) throws IOException {
         Args.notNull(p_87109_, "Output stream");
         InputStream $$1 = this.content;

         try {
            byte[] $$2 = new byte[4096];
            int $$3;
            if (this.length < 0L) {
               while (($$3 = $$1.read($$2)) != -1) {
                  p_87109_.write($$2, 0, $$3);
                  this.uploadStatus.bytesWritten += $$3;
               }
            } else {
               long $$4 = this.length;

               while ($$4 > 0L) {
                  $$3 = $$1.read($$2, 0, (int)Math.min(4096L, $$4));
                  if ($$3 == -1) {
                     break;
                  }

                  p_87109_.write($$2, 0, $$3);
                  this.uploadStatus.bytesWritten += $$3;
                  $$4 -= $$3;
                  p_87109_.flush();
               }
            }
         } finally {
            $$1.close();
         }
      }
   }
}
