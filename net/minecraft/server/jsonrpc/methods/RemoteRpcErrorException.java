package net.minecraft.server.jsonrpc.methods;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RemoteRpcErrorException extends RuntimeException {
   private final JsonElement id;
   private final JsonObject error;

   public RemoteRpcErrorException(JsonElement p_449972_, JsonObject p_443500_) {
      this.id = p_449972_;
      this.error = p_443500_;
   }

   private JsonObject getError() {
      return this.error;
   }

   private JsonElement getId() {
      return this.id;
   }
}
