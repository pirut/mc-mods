package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;

public record PendingRpcRequest<Result>(
   Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method, CompletableFuture<Result> resultFuture, long timeoutTime
) {
   public void accept(JsonElement p_442619_) {
      try {
         Result result = (Result)this.method.value().decodeResult(p_442619_);
         this.resultFuture.complete(Objects.requireNonNull(result));
      } catch (Exception var3) {
         this.resultFuture.completeExceptionally(var3);
      }
   }

   public boolean timedOut(long p_442861_) {
      return p_442861_ > this.timeoutTime;
   }
}
