package net.minecraft.server.jsonrpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidRequestJsonRpcException;
import net.minecraft.server.jsonrpc.methods.MethodNotFoundJsonRpcException;
import net.minecraft.server.jsonrpc.methods.RemoteRpcErrorException;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Connection extends SimpleChannelInboundHandler<JsonElement> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final AtomicInteger CONNECTION_ID_COUNTER = new AtomicInteger(0);
   private final JsonRpcLogger jsonRpcLogger;
   private final ClientInfo clientInfo;
   private final ManagementServer managementServer;
   private final Channel channel;
   private final MinecraftApi minecraftApi;
   private final AtomicInteger transactionId = new AtomicInteger();
   private final Int2ObjectMap<PendingRpcRequest<?>> pendingRequests = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap());

   public Connection(Channel p_443561_, ManagementServer p_443606_, MinecraftApi p_442841_, JsonRpcLogger p_442479_) {
      this.clientInfo = ClientInfo.of(CONNECTION_ID_COUNTER.incrementAndGet());
      this.managementServer = p_443606_;
      this.minecraftApi = p_442841_;
      this.channel = p_443561_;
      this.jsonRpcLogger = p_442479_;
   }

   public void tick() {
      long i = Util.getMillis();
      this.pendingRequests
         .int2ObjectEntrySet()
         .removeIf(
            p_466327_ -> {
               boolean flag = ((PendingRpcRequest)p_466327_.getValue()).timedOut(i);
               if (flag) {
                  ((PendingRpcRequest)p_466327_.getValue())
                     .resultFuture()
                     .completeExceptionally(
                        new ReadTimeoutException(
                           "RPC method " + ((PendingRpcRequest)p_466327_.getValue()).method().key().identifier() + " timed out waiting for response"
                        )
                     );
               }

               return flag;
            }
         );
   }

   public void channelActive(ChannelHandlerContext p_442887_) throws Exception {
      this.jsonRpcLogger.log(this.clientInfo, "Management connection opened for {}", this.channel.remoteAddress());
      super.channelActive(p_442887_);
      this.managementServer.onConnected(this);
   }

   public void channelInactive(ChannelHandlerContext p_443272_) throws Exception {
      this.jsonRpcLogger.log(this.clientInfo, "Management connection closed for {}", this.channel.remoteAddress());
      super.channelInactive(p_443272_);
      this.managementServer.onDisconnected(this);
   }

   public void exceptionCaught(ChannelHandlerContext p_442751_, Throwable p_442647_) throws Exception {
      if (p_442647_.getCause() instanceof JsonParseException) {
         this.channel.writeAndFlush(JsonRPCErrors.PARSE_ERROR.createWithUnknownId(p_442647_.getMessage()));
      } else {
         super.exceptionCaught(p_442751_, p_442647_);
         this.channel.close().awaitUninterruptibly();
      }
   }

   protected void channelRead0(ChannelHandlerContext p_443211_, JsonElement p_443352_) {
      if (p_443352_.isJsonObject()) {
         JsonObject jsonobject = this.handleJsonObject(p_443352_.getAsJsonObject());
         if (jsonobject != null) {
            this.channel.writeAndFlush(jsonobject);
         }
      } else if (p_443352_.isJsonArray()) {
         this.channel.writeAndFlush(this.handleBatchRequest(p_443352_.getAsJsonArray().asList()));
      } else {
         this.channel.writeAndFlush(JsonRPCErrors.INVALID_REQUEST.createWithUnknownId(null));
      }
   }

   private JsonArray handleBatchRequest(List<JsonElement> p_449202_) {
      JsonArray jsonarray = new JsonArray();
      p_449202_.stream().map(p_448829_ -> this.handleJsonObject(p_448829_.getAsJsonObject())).filter(Objects::nonNull).forEach(jsonarray::add);
      return jsonarray;
   }

   public void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> p_451244_) {
      this.sendRequest(p_451244_, null, false);
   }

   public <Params> void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> p_451405_, Params p_450980_) {
      this.sendRequest(p_451405_, p_450980_, false);
   }

   public <Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Void, Result>> p_451436_) {
      return this.sendRequest(p_451436_, null, true);
   }

   public <Params, Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Params, Result>> p_451372_, Params p_443101_) {
      return this.sendRequest(p_451372_, p_443101_, true);
   }

   @Contract("_,_,false->null;_,_,true->!null")
   @Nullable
   private <Params, Result> CompletableFuture<Result> sendRequest(
      Holder.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> p_451408_, @Nullable Params p_442928_, boolean p_442806_
   ) {
      List<JsonElement> list = p_442928_ != null ? List.of(Objects.requireNonNull(p_451408_.value().encodeParams(p_442928_))) : List.of();
      if (p_442806_) {
         CompletableFuture<Result> completablefuture = new CompletableFuture<>();
         int i = this.transactionId.incrementAndGet();
         long j = Util.timeSource.get(TimeUnit.MILLISECONDS);
         this.pendingRequests.put(i, new PendingRpcRequest<>(p_451408_, completablefuture, j + 5000L));
         this.channel.writeAndFlush(JsonRPCUtils.createRequest(i, p_451408_.key().identifier(), list));
         return completablefuture;
      } else {
         this.channel.writeAndFlush(JsonRPCUtils.createRequest(null, p_451408_.key().identifier(), list));
         return null;
      }
   }

   @VisibleForTesting
   @Nullable
   JsonObject handleJsonObject(JsonObject p_443045_) {
      try {
         JsonElement jsonelement = JsonRPCUtils.getRequestId(p_443045_);
         String s = JsonRPCUtils.getMethodName(p_443045_);
         JsonElement jsonelement1 = JsonRPCUtils.getResult(p_443045_);
         JsonElement jsonelement2 = JsonRPCUtils.getParams(p_443045_);
         JsonObject jsonobject = JsonRPCUtils.getError(p_443045_);
         if (s != null && jsonelement1 == null && jsonobject == null) {
            return jsonelement != null && !isValidRequestId(jsonelement)
               ? JsonRPCErrors.INVALID_REQUEST.createWithUnknownId("Invalid request id - only String, Number and NULL supported")
               : this.handleIncomingRequest(jsonelement, s, jsonelement2);
         } else if (s == null && jsonelement1 != null && jsonobject == null && jsonelement != null) {
            if (isValidResponseId(jsonelement)) {
               this.handleRequestResponse(jsonelement.getAsInt(), jsonelement1);
            } else {
               LOGGER.warn("Received respose {} with id {} we did not request", jsonelement1, jsonelement);
            }

            return null;
         } else {
            return s == null && jsonelement1 == null && jsonobject != null
               ? this.handleError(jsonelement, jsonobject)
               : JsonRPCErrors.INVALID_REQUEST.createWithoutData(Objects.requireNonNullElse(jsonelement, JsonNull.INSTANCE));
         }
      } catch (Exception var7) {
         LOGGER.error("Error while handling rpc request", var7);
         return JsonRPCErrors.INTERNAL_ERROR.createWithUnknownId("Unknown error handling request - check server logs for stack trace");
      }
   }

   private static boolean isValidRequestId(JsonElement p_451473_) {
      return p_451473_.isJsonNull() || GsonHelper.isNumberValue(p_451473_) || GsonHelper.isStringValue(p_451473_);
   }

   private static boolean isValidResponseId(JsonElement p_451123_) {
      return GsonHelper.isNumberValue(p_451123_);
   }

   @Nullable
   private JsonObject handleIncomingRequest(@Nullable JsonElement p_442487_, String p_443189_, @Nullable JsonElement p_449633_) {
      boolean flag = p_442487_ != null;

      try {
         JsonElement jsonelement = this.dispatchIncomingRequest(p_443189_, p_449633_);
         return jsonelement != null && flag ? JsonRPCUtils.createSuccessResult(p_442487_, jsonelement) : null;
      } catch (InvalidParameterJsonRpcException var6) {
         LOGGER.debug("Invalid parameter invocation {}: {}, {}", new Object[]{p_443189_, p_449633_, var6.getMessage()});
         return flag ? JsonRPCErrors.INVALID_PARAMS.create(p_442487_, var6.getMessage()) : null;
      } catch (EncodeJsonRpcException var7) {
         LOGGER.error("Failed to encode json rpc response {}: {}", p_443189_, var7.getMessage());
         return flag ? JsonRPCErrors.INTERNAL_ERROR.create(p_442487_, var7.getMessage()) : null;
      } catch (InvalidRequestJsonRpcException var8) {
         return flag ? JsonRPCErrors.INVALID_REQUEST.create(p_442487_, var8.getMessage()) : null;
      } catch (MethodNotFoundJsonRpcException var9) {
         return flag ? JsonRPCErrors.METHOD_NOT_FOUND.create(p_442487_, var9.getMessage()) : null;
      } catch (Exception var10) {
         LOGGER.error("Error while dispatching rpc method {}", p_443189_, var10);
         return flag ? JsonRPCErrors.INTERNAL_ERROR.createWithoutData(p_442487_) : null;
      }
   }

   @Nullable
   public JsonElement dispatchIncomingRequest(String p_443544_, @Nullable JsonElement p_443365_) {
      Identifier identifier = Identifier.tryParse(p_443544_);
      if (identifier == null) {
         throw new InvalidRequestJsonRpcException("Failed to parse method value: " + p_443544_);
      } else {
         Optional<IncomingRpcMethod<?, ?>> optional = BuiltInRegistries.INCOMING_RPC_METHOD.getOptional(identifier);
         if (optional.isEmpty()) {
            throw new MethodNotFoundJsonRpcException("Method not found: " + p_443544_);
         } else if (optional.get().attributes().runOnMainThread()) {
            try {
               return this.minecraftApi.<JsonElement>submit(() -> optional.get().apply(this.minecraftApi, p_443365_, this.clientInfo)).join();
            } catch (CompletionException var8) {
               if (var8.getCause() instanceof RuntimeException runtimeexception) {
                  throw runtimeexception;
               } else {
                  throw var8;
               }
            }
         } else {
            return optional.get().apply(this.minecraftApi, p_443365_, this.clientInfo);
         }
      }
   }

   private void handleRequestResponse(int p_450921_, JsonElement p_443143_) {
      PendingRpcRequest<?> pendingrpcrequest = (PendingRpcRequest<?>)this.pendingRequests.remove(p_450921_);
      if (pendingrpcrequest == null) {
         LOGGER.warn("Received unknown response (id: {}): {}", p_450921_, p_443143_);
      } else {
         pendingrpcrequest.accept(p_443143_);
      }
   }

   @Nullable
   private JsonObject handleError(@Nullable JsonElement p_449792_, JsonObject p_442625_) {
      if (p_449792_ != null && isValidResponseId(p_449792_)) {
         PendingRpcRequest<?> pendingrpcrequest = (PendingRpcRequest<?>)this.pendingRequests.remove(p_449792_.getAsInt());
         if (pendingrpcrequest != null) {
            pendingrpcrequest.resultFuture().completeExceptionally(new RemoteRpcErrorException(p_449792_, p_442625_));
         }
      }

      LOGGER.error("Received error (id: {}): {}", p_449792_, p_442625_);
      return null;
   }
}
