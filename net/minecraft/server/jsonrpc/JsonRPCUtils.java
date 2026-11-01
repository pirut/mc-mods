package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class JsonRPCUtils {
   public static final String JSON_RPC_VERSION = "2.0";
   public static final String OPEN_RPC_VERSION = "1.3.2";

   public static JsonObject createSuccessResult(JsonElement requestId, JsonElement result) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("jsonrpc", "2.0");
      jsonObject.add("id", requestId);
      jsonObject.add("result", result);
      return jsonObject;
   }

   public static JsonObject createRequest(@Nullable Integer requestId, Identifier methodName, List<JsonElement> params) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("jsonrpc", "2.0");
      if (requestId != null) {
         jsonObject.addProperty("id", requestId);
      }

      jsonObject.addProperty("method", methodName.toString());
      if (!params.isEmpty()) {
         JsonArray jsonArray = new JsonArray(params.size());

         for (JsonElement jsonElement : params) {
            jsonArray.add(jsonElement);
         }

         jsonObject.add("params", jsonArray);
      }

      return jsonObject;
   }

   public static JsonObject createError(JsonElement requestId, String message, int code, @Nullable String data) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("jsonrpc", "2.0");
      jsonObject.add("id", requestId);
      JsonObject jsonObject2 = new JsonObject();
      jsonObject2.addProperty("code", code);
      jsonObject2.addProperty("message", message);
      if (data != null && !data.isBlank()) {
         jsonObject2.addProperty("data", data);
      }

      jsonObject.add("error", jsonObject2);
      return jsonObject;
   }

   @Nullable
   public static JsonElement getRequestId(JsonObject json) {
      return json.get("id");
   }

   @Nullable
   public static String getMethodName(JsonObject json) {
      return GsonHelper.getAsString(json, "method", null);
   }

   @Nullable
   public static JsonElement getParams(JsonObject json) {
      return json.get("params");
   }

   @Nullable
   public static JsonElement getResult(JsonObject json) {
      return json.get("result");
   }

   @Nullable
   public static JsonObject getError(JsonObject json) {
      return GsonHelper.getAsJsonObject(json, "error", null);
   }
}
