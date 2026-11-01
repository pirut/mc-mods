package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ServerTextFilter implements AutoCloseable {
   protected static final Logger LOGGER = LogUtils.getLogger();
   private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
   private static final ThreadFactory THREAD_FACTORY = p_361370_ -> {
      Thread thread = new Thread(p_361370_);
      thread.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
      return thread;
   };
   private final URL chatEndpoint;
   private final ServerTextFilter.MessageEncoder chatEncoder;
   final ServerTextFilter.IgnoreStrategy chatIgnoreStrategy;
   final ExecutorService workerPool;

   protected static ExecutorService createWorkerPool(int p_365438_) {
      return Executors.newFixedThreadPool(p_365438_, THREAD_FACTORY);
   }

   protected ServerTextFilter(URL p_365184_, ServerTextFilter.MessageEncoder p_364370_, ServerTextFilter.IgnoreStrategy p_362863_, ExecutorService p_361317_) {
      this.chatIgnoreStrategy = p_362863_;
      this.workerPool = p_361317_;
      this.chatEndpoint = p_365184_;
      this.chatEncoder = p_364370_;
   }

   protected static URL getEndpoint(URI p_362537_, @Nullable JsonObject p_363488_, String p_362892_, String p_363642_) throws MalformedURLException {
      String s = getEndpointFromConfig(p_363488_, p_362892_, p_363642_);
      return p_362537_.resolve("/" + s).toURL();
   }

   protected static String getEndpointFromConfig(@Nullable JsonObject p_364584_, String p_360874_, String p_364672_) {
      return p_364584_ != null ? GsonHelper.getAsString(p_364584_, p_360874_, p_364672_) : p_364672_;
   }

   @Nullable
   public static ServerTextFilter createFromConfig(DedicatedServerProperties p_360477_) {
      String s = p_360477_.textFilteringConfig;
      if (StringUtil.isBlank(s)) {
         return null;
      } else {
         return switch (p_360477_.textFilteringVersion) {
            case 0 -> LegacyTextFilter.createTextFilterFromConfig(s);
            case 1 -> PlayerSafetyServiceTextFilter.createTextFilterFromConfig(s);
            default -> {
               LOGGER.warn("Could not create text filter - unsupported text filtering version used");
               yield null;
            }
         };
      }
   }

   protected CompletableFuture<FilteredText> requestMessageProcessing(
      GameProfile p_362002_, String p_365325_, ServerTextFilter.IgnoreStrategy p_363966_, Executor p_363262_
   ) {
      return p_365325_.isEmpty() ? CompletableFuture.completedFuture(FilteredText.EMPTY) : CompletableFuture.supplyAsync(() -> {
         JsonObject jsonobject = this.chatEncoder.encode(p_362002_, p_365325_);

         try {
            JsonObject jsonobject1 = this.processRequestResponse(jsonobject, this.chatEndpoint);
            return this.filterText(p_365325_, p_363966_, jsonobject1);
         } catch (Exception var6) {
            LOGGER.warn("Failed to validate message '{}'", p_365325_, var6);
            return FilteredText.fullyFiltered(p_365325_);
         }
      }, p_363262_);
   }

   protected abstract FilteredText filterText(String var1, ServerTextFilter.IgnoreStrategy var2, JsonObject var3);

   protected FilterMask parseMask(String p_360987_, JsonArray p_362850_, ServerTextFilter.IgnoreStrategy p_361890_) {
      if (p_362850_.isEmpty()) {
         return FilterMask.PASS_THROUGH;
      } else if (p_361890_.shouldIgnore(p_360987_, p_362850_.size())) {
         return FilterMask.FULLY_FILTERED;
      } else {
         FilterMask filtermask = new FilterMask(p_360987_.length());

         for (int i = 0; i < p_362850_.size(); i++) {
            filtermask.setFiltered(p_362850_.get(i).getAsInt());
         }

         return filtermask;
      }
   }

   @Override
   public void close() {
      this.workerPool.shutdownNow();
   }

   protected void drainStream(InputStream p_364540_) throws IOException {
      byte[] abyte = new byte[1024];

      while (p_364540_.read(abyte) != -1) {
      }
   }

   private JsonObject processRequestResponse(JsonObject p_360751_, URL p_364299_) throws IOException {
      HttpURLConnection httpurlconnection = this.makeRequest(p_360751_, p_364299_);

      JsonObject jsonobject;
      try (InputStream inputstream = httpurlconnection.getInputStream()) {
         if (httpurlconnection.getResponseCode() == 204) {
            return new JsonObject();
         }

         try {
            jsonobject = LenientJsonParser.parse(new InputStreamReader(inputstream, StandardCharsets.UTF_8)).getAsJsonObject();
         } finally {
            this.drainStream(inputstream);
         }
      }

      return jsonobject;
   }

   protected HttpURLConnection makeRequest(JsonObject p_365284_, URL p_360402_) throws IOException {
      HttpURLConnection httpurlconnection = this.getURLConnection(p_360402_);
      this.setAuthorizationProperty(httpurlconnection);
      OutputStreamWriter outputstreamwriter = new OutputStreamWriter(httpurlconnection.getOutputStream(), StandardCharsets.UTF_8);

      try {
         JsonWriter jsonwriter = new JsonWriter(outputstreamwriter);

         try {
            Streams.write(p_365284_, jsonwriter);
         } catch (Throwable var10) {
            try {
               jsonwriter.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         jsonwriter.close();
      } catch (Throwable var11) {
         try {
            outputstreamwriter.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      outputstreamwriter.close();
      int i = httpurlconnection.getResponseCode();
      if (i >= 200 && i < 300) {
         return httpurlconnection;
      } else {
         throw new ServerTextFilter.RequestFailedException(i + " " + httpurlconnection.getResponseMessage());
      }
   }

   protected abstract void setAuthorizationProperty(HttpURLConnection var1);

   protected int connectionReadTimeout() {
      return 2000;
   }

   protected HttpURLConnection getURLConnection(URL p_364237_) throws IOException {
      HttpURLConnection httpurlconnection = (HttpURLConnection)p_364237_.openConnection();
      httpurlconnection.setConnectTimeout(15000);
      httpurlconnection.setReadTimeout(this.connectionReadTimeout());
      httpurlconnection.setUseCaches(false);
      httpurlconnection.setDoOutput(true);
      httpurlconnection.setDoInput(true);
      httpurlconnection.setRequestMethod("POST");
      httpurlconnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      httpurlconnection.setRequestProperty("Accept", "application/json");
      httpurlconnection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().name());
      return httpurlconnection;
   }

   public TextFilter createContext(GameProfile p_364881_) {
      return new ServerTextFilter.PlayerContext(p_364881_);
   }

   @FunctionalInterface
   public interface IgnoreStrategy {
      ServerTextFilter.IgnoreStrategy NEVER_IGNORE = (p_361637_, p_365001_) -> false;
      ServerTextFilter.IgnoreStrategy IGNORE_FULLY_FILTERED = (p_361951_, p_362976_) -> p_361951_.length() == p_362976_;

      static ServerTextFilter.IgnoreStrategy ignoreOverThreshold(int p_365213_) {
         return (p_363255_, p_363924_) -> p_363924_ >= p_365213_;
      }

      static ServerTextFilter.IgnoreStrategy select(int p_362175_) {
         return switch (p_362175_) {
            case -1 -> NEVER_IGNORE;
            case 0 -> IGNORE_FULLY_FILTERED;
            default -> ignoreOverThreshold(p_362175_);
         };
      }

      boolean shouldIgnore(String var1, int var2);
   }

   @FunctionalInterface
   protected interface MessageEncoder {
      JsonObject encode(GameProfile var1, String var2);
   }

   protected class PlayerContext implements TextFilter {
      protected final GameProfile profile;
      protected final Executor streamExecutor;

      protected PlayerContext(GameProfile p_360371_) {
         this.profile = p_360371_;
         ConsecutiveExecutor consecutiveexecutor = new ConsecutiveExecutor(ServerTextFilter.this.workerPool, "chat stream for " + p_360371_.name());
         this.streamExecutor = consecutiveexecutor::schedule;
      }

      @Override
      public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> p_360687_) {
         List<CompletableFuture<FilteredText>> list = p_360687_.stream()
            .map(
               p_361744_ -> ServerTextFilter.this.requestMessageProcessing(
                  this.profile, p_361744_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor
               )
            )
            .collect(ImmutableList.toImmutableList());
         return Util.sequenceFailFast(list).exceptionally(p_365427_ -> ImmutableList.of());
      }

      @Override
      public CompletableFuture<FilteredText> processStreamMessage(String p_362855_) {
         return ServerTextFilter.this.requestMessageProcessing(this.profile, p_362855_, ServerTextFilter.this.chatIgnoreStrategy, this.streamExecutor);
      }
   }

   protected static class RequestFailedException extends RuntimeException {
      protected RequestFailedException(String p_360393_) {
         super(p_360393_);
      }
   }
}
