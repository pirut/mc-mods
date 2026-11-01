package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import org.jspecify.annotations.Nullable;

public interface IncomingRpcMethod<Params, Result> {
   MethodInfo<Params, Result> info();

   IncomingRpcMethod.Attributes attributes();

   JsonElement apply(MinecraftApi var1, @Nullable JsonElement var2, ClientInfo var3);

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> p_458267_) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(p_458267_);
   }

   static <Params, Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> method(IncomingRpcMethod.RpcMethodFunction<Params, Result> p_443259_) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(p_443259_);
   }

   static <Result> IncomingRpcMethod.IncomingRpcMethodBuilder<Void, Result> method(Function<MinecraftApi, Result> p_457857_) {
      return new IncomingRpcMethod.IncomingRpcMethodBuilder<>(p_457857_);
   }

   public record Attributes(boolean runOnMainThread, boolean discoverable) {
   }

   public static class IncomingRpcMethodBuilder<Params, Result> {
      private String description = "";
      @Nullable
      private ParamInfo<Params> paramInfo;
      @Nullable
      private ResultInfo<Result> resultInfo;
      private boolean discoverable = true;
      private boolean runOnMainThread = true;
      @Nullable
      private IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> parameterlessFunction;
      @Nullable
      private IncomingRpcMethod.RpcMethodFunction<Params, Result> parameterFunction;

      public IncomingRpcMethodBuilder(IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> p_458310_) {
         this.parameterlessFunction = p_458310_;
      }

      public IncomingRpcMethodBuilder(IncomingRpcMethod.RpcMethodFunction<Params, Result> p_457915_) {
         this.parameterFunction = p_457915_;
      }

      public IncomingRpcMethodBuilder(Function<MinecraftApi, Result> p_458187_) {
         this.parameterlessFunction = (p_457689_, p_457693_) -> p_458187_.apply(p_457689_);
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> description(String p_442936_) {
         this.description = p_442936_;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> response(String p_458312_, Schema<Result> p_457776_) {
         this.resultInfo = new ResultInfo<>(p_458312_, p_457776_.info());
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> param(String p_457854_, Schema<Params> p_457918_) {
         this.paramInfo = new ParamInfo<>(p_457854_, p_457918_.info());
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> undiscoverable() {
         this.discoverable = false;
         return this;
      }

      public IncomingRpcMethod.IncomingRpcMethodBuilder<Params, Result> notOnMainThread() {
         this.runOnMainThread = false;
         return this;
      }

      public IncomingRpcMethod<Params, Result> build() {
         if (this.resultInfo == null) {
            throw new IllegalStateException("No response defined");
         } else {
            IncomingRpcMethod.Attributes incomingrpcmethod$attributes = new IncomingRpcMethod.Attributes(this.runOnMainThread, this.discoverable);
            MethodInfo<Params, Result> methodinfo = new MethodInfo<>(this.description, this.paramInfo, this.resultInfo);
            if (this.parameterlessFunction != null) {
               return new IncomingRpcMethod.ParameterlessMethod<>(methodinfo, incomingrpcmethod$attributes, this.parameterlessFunction);
            } else if (this.parameterFunction != null) {
               if (this.paramInfo == null) {
                  throw new IllegalStateException("No param schema defined");
               } else {
                  return new IncomingRpcMethod.Method<>(methodinfo, incomingrpcmethod$attributes, this.parameterFunction);
               }
            } else {
               throw new IllegalStateException("No method defined");
            }
         }
      }

      public IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> p_451139_, String p_451409_) {
         return this.register(p_451139_, Identifier.withDefaultNamespace(p_451409_));
      }

      private IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> p_443333_, Identifier p_468356_) {
         return Registry.register(p_443333_, p_468356_, this.build());
      }
   }

   public record Method<Params, Result>(
      MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.RpcMethodFunction<Params, Result> function
   ) implements IncomingRpcMethod<Params, Result> {
      @Override
      public JsonElement apply(MinecraftApi p_442932_, @Nullable JsonElement p_443044_, ClientInfo p_442918_) {
         if (p_443044_ != null && (p_443044_.isJsonArray() || p_443044_.isJsonObject())) {
            if (this.info.params().isEmpty()) {
               throw new IllegalArgumentException("Method defined as having parameters without describing them");
            } else {
               JsonElement jsonelement;
               if (p_443044_.isJsonObject()) {
                  String s = this.info.params().get().name();
                  JsonElement jsonelement1 = p_443044_.getAsJsonObject().get(s);
                  if (jsonelement1 == null) {
                     throw new InvalidParameterJsonRpcException(String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", s));
                  }

                  jsonelement = jsonelement1;
               } else {
                  JsonArray jsonarray = p_443044_.getAsJsonArray();
                  if (jsonarray.isEmpty() || jsonarray.size() > 1) {
                     throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
                  }

                  jsonelement = jsonarray.get(0);
               }

               Params params = (Params)this.info
                  .params()
                  .get()
                  .schema()
                  .codec()
                  .parse(JsonOps.INSTANCE, jsonelement)
                  .getOrThrow(InvalidParameterJsonRpcException::new);
               Result result = this.function.apply(p_442932_, params, p_442918_);
               if (this.info.result().isEmpty()) {
                  throw new IllegalStateException("No result codec defined");
               } else {
                  return (JsonElement)this.info.result().get().schema().codec().encodeStart(JsonOps.INSTANCE, result).getOrThrow(EncodeJsonRpcException::new);
               }
            }
         } else {
            throw new InvalidParameterJsonRpcException("Expected params as array or named");
         }
      }
   }

   public record ParameterlessMethod<Params, Result>(
      MethodInfo<Params, Result> info, IncomingRpcMethod.Attributes attributes, IncomingRpcMethod.ParameterlessRpcMethodFunction<Result> supplier
   ) implements IncomingRpcMethod<Params, Result> {
      @Override
      public JsonElement apply(MinecraftApi p_443197_, @Nullable JsonElement p_442828_, ClientInfo p_443323_) {
         if (p_442828_ == null || p_442828_.isJsonArray() && p_442828_.getAsJsonArray().isEmpty()) {
            if (this.info.params().isPresent()) {
               throw new IllegalArgumentException("Parameterless method unexpectedly has parameter description");
            } else {
               Result result = this.supplier.apply(p_443197_, p_443323_);
               if (this.info.result().isEmpty()) {
                  throw new IllegalStateException("No result codec defined");
               } else {
                  return (JsonElement)this.info
                     .result()
                     .get()
                     .schema()
                     .codec()
                     .encodeStart(JsonOps.INSTANCE, result)
                     .getOrThrow(InvalidParameterJsonRpcException::new);
               }
            }
         } else {
            throw new InvalidParameterJsonRpcException("Expected no params, or an empty array");
         }
      }
   }

   @FunctionalInterface
   public interface ParameterlessRpcMethodFunction<Result> {
      Result apply(MinecraftApi var1, ClientInfo var2);
   }

   @FunctionalInterface
   public interface RpcMethodFunction<Params, Result> {
      Result apply(MinecraftApi var1, Params var2, ClientInfo var3);
   }
}
