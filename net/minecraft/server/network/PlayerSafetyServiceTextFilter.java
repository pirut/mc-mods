package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCertificate;
import com.microsoft.aad.msal4j.ConfidentialClientApplication.Builder;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class PlayerSafetyServiceTextFilter extends ServerTextFilter {
   private final ConfidentialClientApplication client;
   private final ClientCredentialParameters clientParameters;
   private final Set<String> fullyFilteredEvents;
   private final int connectionReadTimeoutMs;

   private PlayerSafetyServiceTextFilter(
      URL chatEndpoint,
      ServerTextFilter.MessageEncoder chatEncoder,
      ServerTextFilter.IgnoreStrategy chatIgnoreStrategy,
      ExecutorService workerPool,
      ConfidentialClientApplication client,
      ClientCredentialParameters clientParameters,
      Set<String> fullyFilteredEvents,
      int connectionReadTimeoutMs
   ) {
      super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
      this.client = client;
      this.clientParameters = clientParameters;
      this.fullyFilteredEvents = fullyFilteredEvents;
      this.connectionReadTimeoutMs = connectionReadTimeoutMs;
   }

   @Nullable
   public static ServerTextFilter createTextFilterFromConfig(String config) {
      JsonObject jsonObject = GsonHelper.parse(config);
      URI uRI = URI.create(GsonHelper.getAsString(jsonObject, "apiServer"));
      String string = GsonHelper.getAsString(jsonObject, "apiPath");
      String string2 = GsonHelper.getAsString(jsonObject, "scope");
      String string3 = GsonHelper.getAsString(jsonObject, "serverId", "");
      String string4 = GsonHelper.getAsString(jsonObject, "applicationId");
      String string5 = GsonHelper.getAsString(jsonObject, "tenantId");
      String string6 = GsonHelper.getAsString(jsonObject, "roomId", "Java:Chat");
      String string7 = GsonHelper.getAsString(jsonObject, "certificatePath");
      String string8 = GsonHelper.getAsString(jsonObject, "certificatePassword", "");
      int i = GsonHelper.getAsInt(jsonObject, "hashesToDrop", -1);
      int j = GsonHelper.getAsInt(jsonObject, "maxConcurrentRequests", 7);
      JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "fullyFilteredEvents");
      Set<String> set = new HashSet<>();
      jsonArray.forEach(jsonElement -> set.add(GsonHelper.convertToString(jsonElement, "filteredEvent")));
      int k = GsonHelper.getAsInt(jsonObject, "connectionReadTimeoutMs", 2000);

      URL uRL;
      try {
         uRL = uRI.resolve(string).toURL();
      } catch (MalformedURLException var26) {
         throw new RuntimeException(var26);
      }

      ServerTextFilter.MessageEncoder messageEncoder = (gameProfile, string3x) -> {
         JsonObject jsonObjectx = new JsonObject();
         jsonObjectx.addProperty("userId", gameProfile.id().toString());
         jsonObjectx.addProperty("userDisplayName", gameProfile.name());
         jsonObjectx.addProperty("server", string3);
         jsonObjectx.addProperty("room", string6);
         jsonObjectx.addProperty("area", "JavaChatRealms");
         jsonObjectx.addProperty("data", string3x);
         jsonObjectx.addProperty("language", "*");
         return jsonObjectx;
      };
      ServerTextFilter.IgnoreStrategy ignoreStrategy = ServerTextFilter.IgnoreStrategy.select(i);
      ExecutorService executorService = createWorkerPool(j);

      IClientCertificate iClientCertificate;
      try (InputStream inputStream = Files.newInputStream(Path.of(string7))) {
         iClientCertificate = ClientCredentialFactory.createFromCertificate(inputStream, string8);
      } catch (Exception var28) {
         LOGGER.warn("Failed to open certificate file");
         return null;
      }

      ConfidentialClientApplication confidentialClientApplication;
      try {
         confidentialClientApplication = ((Builder)((Builder)ConfidentialClientApplication.builder(string4, iClientCertificate)
                  .sendX5c(true)
                  .executorService(executorService))
               .authority(String.format(Locale.ROOT, "https://login.microsoftonline.com/%s/", string5)))
            .build();
      } catch (Exception var25) {
         LOGGER.warn("Failed to create confidential client application");
         return null;
      }

      ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(Set.of(string2)).build();
      return new PlayerSafetyServiceTextFilter(
         uRL, messageEncoder, ignoreStrategy, executorService, confidentialClientApplication, clientCredentialParameters, set, k
      );
   }

   private IAuthenticationResult aquireIAuthenticationResult() {
      return (IAuthenticationResult)this.client.acquireToken(this.clientParameters).join();
   }

   @Override
   protected void setAuthorizationProperty(HttpURLConnection connection) {
      IAuthenticationResult iAuthenticationResult = this.aquireIAuthenticationResult();
      connection.setRequestProperty("Authorization", "Bearer " + iAuthenticationResult.accessToken());
   }

   @Override
   protected FilteredText filterText(String text, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject response) {
      JsonObject jsonObject = GsonHelper.getAsJsonObject(response, "result", null);
      if (jsonObject == null) {
         return FilteredText.fullyFiltered(text);
      } else {
         boolean bl = GsonHelper.getAsBoolean(jsonObject, "filtered", true);
         if (!bl) {
            return FilteredText.passThrough(text);
         } else {
            for (JsonElement jsonElement : GsonHelper.getAsJsonArray(jsonObject, "events", new JsonArray())) {
               JsonObject jsonObject2 = jsonElement.getAsJsonObject();
               String string = GsonHelper.getAsString(jsonObject2, "id", "");
               if (this.fullyFilteredEvents.contains(string)) {
                  return FilteredText.fullyFiltered(text);
               }
            }

            JsonArray jsonArray2 = GsonHelper.getAsJsonArray(jsonObject, "redactedTextIndex", new JsonArray());
            return new FilteredText(text, this.parseMask(text, jsonArray2, ignoreStrategy));
         }
      }
   }

   @Override
   protected int connectionReadTimeout() {
      return this.connectionReadTimeoutMs;
   }
}
