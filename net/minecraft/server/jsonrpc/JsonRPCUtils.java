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

   public static JsonObject createSuccessResult(JsonElement p_450922_, JsonElement p_451430_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("jsonrpc", "2.0");
      jsonobject.add("id", p_450922_);
      jsonobject.add("result", p_451430_);
      return jsonobject;
   }

   public static JsonObject createRequest(@Nullable Integer p_443114_, Identifier p_467426_, List<JsonElement> p_442808_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("jsonrpc", "2.0");
      if (p_443114_ != null) {
         jsonobject.addProperty("id", p_443114_);
      }

      jsonobject.addProperty("method", p_467426_.toString());
      if (!p_442808_.isEmpty()) {
         JsonArray jsonarray = new JsonArray(p_442808_.size());

         for (JsonElement jsonelement : p_442808_) {
            jsonarray.add(jsonelement);
         }

         jsonobject.add("params", jsonarray);
      }

      return jsonobject;
   }

   public static JsonObject createError(JsonElement p_449806_, String p_442652_, int p_443031_, @Nullable String p_443185_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("jsonrpc", "2.0");
      jsonobject.add("id", p_449806_);
      JsonObject jsonobject1 = new JsonObject();
      jsonobject1.addProperty("code", p_443031_);
      jsonobject1.addProperty("message", p_442652_);
      if (p_443185_ != null && !p_443185_.isBlank()) {
         jsonobject1.addProperty("data", p_443185_);
      }

      jsonobject.add("error", jsonobject1);
      return jsonobject;
   }

   @Nullable
   public static JsonElement getRequestId(JsonObject p_443482_) {
      return p_443482_.get("id");
   }

   @Nullable
   public static String getMethodName(JsonObject p_443085_) {
      return GsonHelper.getAsString(p_443085_, "method", null);
   }

   @Nullable
   public static JsonElement getParams(JsonObject p_443168_) {
      return p_443168_.get("params");
   }

   @Nullable
   public static JsonElement getResult(JsonObject p_443425_) {
      return p_443425_.get("result");
   }

   @Nullable
   public static JsonObject getError(JsonObject p_443455_) {
      return GsonHelper.getAsJsonObject(p_443455_, "error", null);
   }
}
