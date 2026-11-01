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

   public Connection(Channel channel, ManagementServer managementServer, MinecraftApi minecraftApi, JsonRpcLogger jsonRpcLogger) {
      this.clientInfo = ClientInfo.of(CONNECTION_ID_COUNTER.incrementAndGet());
      this.managementServer = managementServer;
      this.minecraftApi = minecraftApi;
      this.channel = channel;
      this.jsonRpcLogger = jsonRpcLogger;
   }

   public void tick() {
      long l = Util.getMillis();
      this.pendingRequests
         .int2ObjectEntrySet()
         .removeIf(
            entry -> {
               boolean bl = ((PendingRpcRequest)entry.getValue()).timedOut(l);
               if (bl) {
                  ((PendingRpcRequest)entry.getValue())
                     .resultFuture()
                     .completeExceptionally(
                        new ReadTimeoutException(
                           "RPC method " + ((PendingRpcRequest)entry.getValue()).method().key().identifier() + " timed out waiting for response"
                        )
                     );
               }

               return bl;
            }
         );
   }

   public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
      this.jsonRpcLogger.log(this.clientInfo, "Management connection opened for {}", this.channel.remoteAddress());
      super.channelActive(channelHandlerContext);
      this.managementServer.onConnected(this);
   }

   public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
      this.jsonRpcLogger.log(this.clientInfo, "Management connection closed for {}", this.channel.remoteAddress());
      super.channelInactive(channelHandlerContext);
      this.managementServer.onDisconnected(this);
   }

   public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
      if (throwable.getCause() instanceof JsonParseException) {
         this.channel.writeAndFlush(JsonRPCErrors.PARSE_ERROR.createWithUnknownId(throwable.getMessage()));
      } else {
         super.exceptionCaught(channelHandlerContext, throwable);
         this.channel.close().awaitUninterruptibly();
      }
   }

   protected void channelRead0(ChannelHandlerContext context, JsonElement json) {
      if (json.isJsonObject()) {
         JsonObject jsonObject = this.handleJsonObject(json.getAsJsonObject());
         if (jsonObject != null) {
            this.channel.writeAndFlush(jsonObject);
         }
      } else if (json.isJsonArray()) {
         this.channel.writeAndFlush(this.handleBatchRequest(json.getAsJsonArray().asList()));
      } else {
         this.channel.writeAndFlush(JsonRPCErrors.INVALID_REQUEST.createWithUnknownId(null));
      }
   }

   private JsonArray handleBatchRequest(List<JsonElement> requests) {
      JsonArray jsonArray = new JsonArray();
      requests.stream().map(jsonElement -> this.handleJsonObject(jsonElement.getAsJsonObject())).filter(Objects::nonNull).forEach(jsonArray::add);
      return jsonArray;
   }

   public void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
      this.sendRequest(method, null, false);
   }

   public <Params> void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, Params params) {
      this.sendRequest(method, params, false);
   }

   public <Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Void, Result>> method) {
      return this.sendRequest(method, null, true);
   }

   public <Params, Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Params, Result>> method, Params params) {
      return this.sendRequest(method, params, true);
   }

   @Contract("_,_,false->null;_,_,true->!null")
   @Nullable
   private <Params, Result> CompletableFuture<Result> sendRequest(
      Holder.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> method, @Nullable Params params, boolean expectResponse
   ) {
      List<JsonElement> list = params != null ? List.of(Objects.requireNonNull(method.value().encodeParams(params))) : List.of();
      if (expectResponse) {
         CompletableFuture<Result> completableFuture = new CompletableFuture<>();
         int i = this.transactionId.incrementAndGet();
         long l = Util.timeSource.get(TimeUnit.MILLISECONDS);
         this.pendingRequests.put(i, new PendingRpcRequest<>(method, completableFuture, l + 5000L));
         this.channel.writeAndFlush(JsonRPCUtils.createRequest(i, method.key().identifier(), list));
         return completableFuture;
      } else {
         this.channel.writeAndFlush(JsonRPCUtils.createRequest(null, method.key().identifier(), list));
         return null;
      }
   }

   @VisibleForTesting
   @Nullable
   JsonObject handleJsonObject(JsonObject json) {
      try {
         JsonElement jsonElement = JsonRPCUtils.getRequestId(json);
         String string = JsonRPCUtils.getMethodName(json);
         JsonElement jsonElement2 = JsonRPCUtils.getResult(json);
         JsonElement jsonElement3 = JsonRPCUtils.getParams(json);
         JsonObject jsonObject = JsonRPCUtils.getError(json);
         if (string != null && jsonElement2 == null && jsonObject == null) {
            return jsonElement != null && !isValidRequestId(jsonElement)
               ? JsonRPCErrors.INVALID_REQUEST.createWithUnknownId("Invalid request id - only String, Number and NULL supported")
               : this.handleIncomingRequest(jsonElement, string, jsonElement3);
         } else if (string == null && jsonElement2 != null && jsonObject == null && jsonElement != null) {
            if (isValidResponseId(jsonElement)) {
               this.handleRequestResponse(jsonElement.getAsInt(), jsonElement2);
            } else {
               LOGGER.warn("Received respose {} with id {} we did not request", jsonElement2, jsonElement);
            }

            return null;
         } else {
            return string == null && jsonElement2 == null && jsonObject != null
               ? this.handleError(jsonElement, jsonObject)
               : JsonRPCErrors.INVALID_REQUEST.createWithoutData(Objects.requireNonNullElse(jsonElement, JsonNull.INSTANCE));
         }
      } catch (Exception var7) {
         LOGGER.error("Error while handling rpc request", var7);
         return JsonRPCErrors.INTERNAL_ERROR.createWithUnknownId("Unknown error handling request - check server logs for stack trace");
      }
   }

   private static boolean isValidRequestId(JsonElement json) {
      return json.isJsonNull() || GsonHelper.isNumberValue(json) || GsonHelper.isStringValue(json);
   }

   private static boolean isValidResponseId(JsonElement json) {
      return GsonHelper.isNumberValue(json);
   }

   @Nullable
   private JsonObject handleIncomingRequest(@Nullable JsonElement requestId, String methodName, @Nullable JsonElement params) {
      boolean bl = requestId != null;

      try {
         JsonElement jsonElement = this.dispatchIncomingRequest(methodName, params);
         return jsonElement != null && bl ? JsonRPCUtils.createSuccessResult(requestId, jsonElement) : null;
      } catch (InvalidParameterJsonRpcException var6) {
         LOGGER.debug("Invalid parameter invocation {}: {}, {}", new Object[]{methodName, params, var6.getMessage()});
         return bl ? JsonRPCErrors.INVALID_PARAMS.create(requestId, var6.getMessage()) : null;
      } catch (EncodeJsonRpcException var7) {
         LOGGER.error("Failed to encode json rpc response {}: {}", methodName, var7.getMessage());
         return bl ? JsonRPCErrors.INTERNAL_ERROR.create(requestId, var7.getMessage()) : null;
      } catch (InvalidRequestJsonRpcException var8) {
         return bl ? JsonRPCErrors.INVALID_REQUEST.create(requestId, var8.getMessage()) : null;
      } catch (MethodNotFoundJsonRpcException var9) {
         return bl ? JsonRPCErrors.METHOD_NOT_FOUND.create(requestId, var9.getMessage()) : null;
      } catch (Exception var10) {
         LOGGER.error("Error while dispatching rpc method {}", methodName, var10);
         return bl ? JsonRPCErrors.INTERNAL_ERROR.createWithoutData(requestId) : null;
      }
   }

   @Nullable
   public JsonElement dispatchIncomingRequest(String methodName, @Nullable JsonElement params) {
      Identifier identifier = Identifier.tryParse(methodName);
      if (identifier == null) {
         throw new InvalidRequestJsonRpcException("Failed to parse method value: " + methodName);
      } else {
         Optional<IncomingRpcMethod<?, ?>> optional = BuiltInRegistries.INCOMING_RPC_METHOD.getOptional(identifier);
         if (optional.isEmpty()) {
            throw new MethodNotFoundJsonRpcException("Method not found: " + methodName);
         } else if (optional.get().attributes().runOnMainThread()) {
            try {
               return this.minecraftApi.<JsonElement>submit(() -> optional.get().apply(this.minecraftApi, params, this.clientInfo)).join();
            } catch (CompletionException var8) {
               if (var8.getCause() instanceof RuntimeException runtimeException) {
                  throw runtimeException;
               } else {
                  throw var8;
               }
            }
         } else {
            return optional.get().apply(this.minecraftApi, params, this.clientInfo);
         }
      }
   }

   private void handleRequestResponse(int requestId, JsonElement result) {
      PendingRpcRequest<?> pendingRpcRequest = (PendingRpcRequest<?>)this.pendingRequests.remove(requestId);
      if (pendingRpcRequest == null) {
         LOGGER.warn("Received unknown response (id: {}): {}", requestId, result);
      } else {
         pendingRpcRequest.accept(result);
      }
   }

   @Nullable
   private JsonObject handleError(@Nullable JsonElement requestId, JsonObject error) {
      if (requestId != null && isValidResponseId(requestId)) {
         PendingRpcRequest<?> pendingRpcRequest = (PendingRpcRequest<?>)this.pendingRequests.remove(requestId.getAsInt());
         if (pendingRpcRequest != null) {
            pendingRpcRequest.resultFuture().completeExceptionally(new RemoteRpcErrorException(requestId, error));
         }
      }

      LOGGER.error("Received error (id: {}): {}", requestId, error);
      return null;
   }
}
