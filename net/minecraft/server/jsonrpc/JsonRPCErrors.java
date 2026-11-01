package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

public enum JsonRPCErrors {
   PARSE_ERROR(-32700, "Parse error"),
   INVALID_REQUEST(-32600, "Invalid Request"),
   METHOD_NOT_FOUND(-32601, "Method not found"),
   INVALID_PARAMS(-32602, "Invalid params"),
   INTERNAL_ERROR(-32603, "Internal error");

   private final int errorCode;
   private final String message;

   private JsonRPCErrors(int p_442677_, String p_443042_) {
      this.errorCode = p_442677_;
      this.message = p_443042_;
   }

   public JsonObject createWithUnknownId(@Nullable String p_450968_) {
      return JsonRPCUtils.createError(JsonNull.INSTANCE, this.message, this.errorCode, p_450968_);
   }

   public JsonObject createWithoutData(JsonElement p_451359_) {
      return JsonRPCUtils.createError(p_451359_, this.message, this.errorCode, null);
   }

   public JsonObject create(JsonElement p_449670_, String p_449451_) {
      return JsonRPCUtils.createError(p_449670_, this.message, this.errorCode, p_449451_);
   }
}
