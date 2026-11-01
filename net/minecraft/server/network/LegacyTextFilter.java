package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class LegacyTextFilter extends ServerTextFilter {
   private static final String ENDPOINT = "v1/chat";
   final URL joinEndpoint;
   final LegacyTextFilter.JoinOrLeaveEncoder joinEncoder;
   final URL leaveEndpoint;
   final LegacyTextFilter.JoinOrLeaveEncoder leaveEncoder;
   private final String authKey;

   private LegacyTextFilter(
      URL chatEndpoint,
      ServerTextFilter.MessageEncoder chatEncoder,
      URL joinEndpoint,
      LegacyTextFilter.JoinOrLeaveEncoder joinEncoder,
      URL leaveEndpoint,
      LegacyTextFilter.JoinOrLeaveEncoder leaveEncoder,
      String authKey,
      ServerTextFilter.IgnoreStrategy chatIgnoreStrategy,
      ExecutorService workerPool
   ) {
      super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
      this.joinEndpoint = joinEndpoint;
      this.joinEncoder = joinEncoder;
      this.leaveEndpoint = leaveEndpoint;
      this.leaveEncoder = leaveEncoder;
      this.authKey = authKey;
   }

   @Nullable
   public static ServerTextFilter createTextFilterFromConfig(String config) {
      try {
         JsonObject jsonObject = GsonHelper.parse(config);
         URI uRI = new URI(GsonHelper.getAsString(jsonObject, "apiServer"));
         String string = GsonHelper.getAsString(jsonObject, "apiKey");
         if (string.isEmpty()) {
            throw new IllegalArgumentException("Missing API key");
         } else {
            int i = GsonHelper.getAsInt(jsonObject, "ruleId", 1);
            String string2 = GsonHelper.getAsString(jsonObject, "serverId", "");
            String string3 = GsonHelper.getAsString(jsonObject, "roomId", "Java:Chat");
            int j = GsonHelper.getAsInt(jsonObject, "hashesToDrop", -1);
            int k = GsonHelper.getAsInt(jsonObject, "maxConcurrentRequests", 7);
            JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "endpoints", null);
            String string4 = getEndpointFromConfig(jsonObject2, "chat", "v1/chat");
            boolean bl = string4.equals("v1/chat");
            URL uRL = uRI.resolve("/" + string4).toURL();
            URL uRL2 = getEndpoint(uRI, jsonObject2, "join", "v1/join");
            URL uRL3 = getEndpoint(uRI, jsonObject2, "leave", "v1/leave");
            LegacyTextFilter.JoinOrLeaveEncoder joinOrLeaveEncoder = gameProfile -> {
               JsonObject jsonObjectx = new JsonObject();
               jsonObjectx.addProperty("server", string2);
               jsonObjectx.addProperty("room", string3);
               jsonObjectx.addProperty("user_id", gameProfile.id().toString());
               jsonObjectx.addProperty("user_display_name", gameProfile.name());
               return jsonObjectx;
            };
            ServerTextFilter.MessageEncoder messageEncoder;
            if (bl) {
               messageEncoder = (gameProfile, string3x) -> {
                  JsonObject jsonObjectx = new JsonObject();
                  jsonObjectx.addProperty("rule", i);
                  jsonObjectx.addProperty("server", string2);
                  jsonObjectx.addProperty("room", string3);
                  jsonObjectx.addProperty("player", gameProfile.id().toString());
                  jsonObjectx.addProperty("player_display_name", gameProfile.name());
                  jsonObjectx.addProperty("text", string3x);
                  jsonObjectx.addProperty("language", "*");
                  return jsonObjectx;
               };
            } else {
               String string5 = String.valueOf(i);
               messageEncoder = (gameProfile, string4x) -> {
                  JsonObject jsonObjectx = new JsonObject();
                  jsonObjectx.addProperty("rule_id", string5);
                  jsonObjectx.addProperty("category", string2);
                  jsonObjectx.addProperty("subcategory", string3);
                  jsonObjectx.addProperty("user_id", gameProfile.id().toString());
                  jsonObjectx.addProperty("user_display_name", gameProfile.name());
                  jsonObjectx.addProperty("text", string4x);
                  jsonObjectx.addProperty("language", "*");
                  return jsonObjectx;
               };
            }

            ServerTextFilter.IgnoreStrategy ignoreStrategy = ServerTextFilter.IgnoreStrategy.select(j);
            ExecutorService executorService = createWorkerPool(k);
            String string6 = Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.US_ASCII));
            return new LegacyTextFilter(uRL, messageEncoder, uRL2, joinOrLeaveEncoder, uRL3, joinOrLeaveEncoder, string6, ignoreStrategy, executorService);
         }
      } catch (Exception var20) {
         LOGGER.warn("Failed to parse chat filter config {}", config, var20);
         return null;
      }
   }

   @Override
   public TextFilter createContext(GameProfile profile) {
      return new ServerTextFilter.PlayerContext(profile) {
         @Override
         public void join() {
            LegacyTextFilter.this.processJoinOrLeave(this.profile, LegacyTextFilter.this.joinEndpoint, LegacyTextFilter.this.joinEncoder, this.streamExecutor);
         }

         @Override
         public void leave() {
            LegacyTextFilter.this.processJoinOrLeave(this.profile, LegacyTextFilter.this.leaveEndpoint, LegacyTextFilter.this.leaveEncoder, this.streamExecutor);
         }
      };
   }

   void processJoinOrLeave(GameProfile profile, URL endpoint, LegacyTextFilter.JoinOrLeaveEncoder encoder, Executor streamExecutor) {
      streamExecutor.execute(() -> {
         JsonObject jsonObject = encoder.encode(profile);

         try {
            this.processRequest(jsonObject, endpoint);
         } catch (Exception var6) {
            LOGGER.warn("Failed to send join/leave packet to {} for player {}", new Object[]{endpoint, profile, var6});
         }
      });
   }

   private void processRequest(JsonObject request, URL endpoint) throws IOException {
      HttpURLConnection httpURLConnection = this.makeRequest(request, endpoint);

      try (InputStream inputStream = httpURLConnection.getInputStream()) {
         this.drainStream(inputStream);
      }
   }

   @Override
   protected void setAuthorizationProperty(HttpURLConnection connection) {
      connection.setRequestProperty("Authorization", "Basic " + this.authKey);
   }

   @Override
   protected FilteredText filterText(String text, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject response) {
      boolean bl = GsonHelper.getAsBoolean(response, "response", false);
      if (bl) {
         return FilteredText.passThrough(text);
      } else {
         String string = GsonHelper.getAsString(response, "hashed", null);
         if (string == null) {
            return FilteredText.fullyFiltered(text);
         } else {
            JsonArray jsonArray = GsonHelper.getAsJsonArray(response, "hashes");
            FilterMask filterMask = this.parseMask(text, jsonArray, ignoreStrategy);
            return new FilteredText(text, filterMask);
         }
      }
   }

   @FunctionalInterface
   interface JoinOrLeaveEncoder {
      JsonObject encode(GameProfile gameProfile);
   }
}
