package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import org.jspecify.annotations.Nullable;

public interface OutgoingRpcMethod<Params, Result> {
   String NOTIFICATION_PREFIX = "notification/";

   MethodInfo<Params, Result> info();

   OutgoingRpcMethod.Attributes attributes();

   @Nullable
   default JsonElement encodeParams(Params p_442939_) {
      return null;
   }

   @Nullable
   default Result decodeResult(JsonElement p_443086_) {
      return null;
   }

   static OutgoingRpcMethod.OutgoingRpcMethodBuilder<Void, Void> notification() {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.ParmeterlessNotification::new);
   }

   static <Params> OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Void> notificationWithParams() {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.Notification::new);
   }

   static <Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<Void, Result> request() {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.ParameterlessMethod::new);
   }

   static <Params, Result> OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> requestWithParams() {
      return new OutgoingRpcMethod.OutgoingRpcMethodBuilder<>(OutgoingRpcMethod.Method::new);
   }

   public record Attributes(boolean discoverable) {
   }

   @FunctionalInterface
   public interface Factory<Params, Result> {
      OutgoingRpcMethod<Params, Result> create(MethodInfo<Params, Result> var1, OutgoingRpcMethod.Attributes var2);
   }

   public record Method<Params, Result>(MethodInfo<Params, Result> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Params, Result> {
      @Nullable
      @Override
      public JsonElement encodeParams(Params p_442798_) {
         if (this.info.params().isEmpty()) {
            throw new IllegalStateException("Method defined as having no parameters");
         } else {
            return (JsonElement)this.info.params().get().schema().codec().encodeStart(JsonOps.INSTANCE, p_442798_).getOrThrow();
         }
      }

      @Override
      public Result decodeResult(JsonElement p_442832_) {
         if (this.info.result().isEmpty()) {
            throw new IllegalStateException("Method defined as having no result");
         } else {
            return (Result)this.info.result().get().schema().codec().parse(JsonOps.INSTANCE, p_442832_).getOrThrow();
         }
      }
   }

   public record Notification<Params>(MethodInfo<Params, Void> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Params, Void> {
      @Nullable
      @Override
      public JsonElement encodeParams(Params p_443326_) {
         if (this.info.params().isEmpty()) {
            throw new IllegalStateException("Method defined as having no parameters");
         } else {
            return (JsonElement)this.info.params().get().schema().codec().encodeStart(JsonOps.INSTANCE, p_443326_).getOrThrow();
         }
      }
   }

   public static class OutgoingRpcMethodBuilder<Params, Result> {
      public static final OutgoingRpcMethod.Attributes DEFAULT_ATTRIBUTES = new OutgoingRpcMethod.Attributes(true);
      private final OutgoingRpcMethod.Factory<Params, Result> method;
      private String description = "";
      @Nullable
      private ParamInfo<Params> paramInfo;
      @Nullable
      private ResultInfo<Result> resultInfo;

      public OutgoingRpcMethodBuilder(OutgoingRpcMethod.Factory<Params, Result> p_451460_) {
         this.method = p_451460_;
      }

      public OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> description(String p_443626_) {
         this.description = p_443626_;
         return this;
      }

      public OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> response(String p_457833_, Schema<Result> p_457722_) {
         this.resultInfo = new ResultInfo<>(p_457833_, p_457722_);
         return this;
      }

      public OutgoingRpcMethod.OutgoingRpcMethodBuilder<Params, Result> param(String p_457556_, Schema<Params> p_457686_) {
         this.paramInfo = new ParamInfo<>(p_457556_, p_457686_);
         return this;
      }

      private OutgoingRpcMethod<Params, Result> build() {
         MethodInfo<Params, Result> methodinfo = new MethodInfo<>(this.description, this.paramInfo, this.resultInfo);
         return this.method.create(methodinfo, DEFAULT_ATTRIBUTES);
      }

      public Holder.Reference<OutgoingRpcMethod<Params, Result>> register(String p_451031_) {
         return this.register(Identifier.withDefaultNamespace("notification/" + p_451031_));
      }

      private Holder.Reference<OutgoingRpcMethod<Params, Result>> register(Identifier p_468768_) {
         return Registry.registerForHolder(BuiltInRegistries.OUTGOING_RPC_METHOD, p_468768_, this.build());
      }
   }

   public record ParameterlessMethod<Result>(MethodInfo<Void, Result> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Result> {
      @Override
      public Result decodeResult(JsonElement p_442885_) {
         if (this.info.result().isEmpty()) {
            throw new IllegalStateException("Method defined as having no result");
         } else {
            return (Result)this.info.result().get().schema().codec().parse(JsonOps.INSTANCE, p_442885_).getOrThrow();
         }
      }
   }

   public record ParmeterlessNotification(MethodInfo<Void, Void> info, OutgoingRpcMethod.Attributes attributes) implements OutgoingRpcMethod<Void, Void> {
   }
}
