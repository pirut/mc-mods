package net.neoforged.neoforge.common.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public interface ICondition {
   Codec<ICondition> CODEC = NeoForgeRegistries.CONDITION_SERIALIZERS.byNameCodec().dispatch(ICondition::codec, Function.identity());
   Codec<List<ICondition>> LIST_CODEC = CODEC.listOf();

   static <V, T> Optional<T> getConditionally(Codec<T> codec, DynamicOps<V> ops, V element) {
      return getWithConditionalCodec(ConditionalOps.createConditionalCodec(codec), ops, element);
   }

   static <V, T> Optional<T> getWithConditionalCodec(Codec<Optional<T>> codec, DynamicOps<V> ops, V element) {
      return (Optional<T>)codec.parse(ops, element).getOrThrow(JsonParseException::new);
   }

   static <V, T> Optional<T> getWithWithConditionsCodec(Codec<Optional<WithConditions<T>>> codec, DynamicOps<V> ops, V elements) {
      return ((Optional)codec.parse(ops, elements).promotePartial(m -> {}).getOrThrow(JsonParseException::new)).map(WithConditions::carrier);
   }

   static <V> boolean conditionsMatched(DynamicOps<V> ops, V element) {
      Codec<Unit> codec = MapCodec.unitCodec(Unit.INSTANCE);
      return getConditionally(codec, ops, element).isPresent();
   }

   static void writeConditions(HolderLookup.Provider registries, JsonObject jsonObject, ICondition... conditions) {
      writeConditions(registries, jsonObject, List.of(conditions));
   }

   static void writeConditions(HolderLookup.Provider registries, JsonObject jsonObject, List<ICondition> conditions) {
      writeConditions(RegistryOps.create(JsonOps.INSTANCE, registries), jsonObject, conditions);
   }

   static void writeConditions(DynamicOps<JsonElement> jsonOps, JsonObject jsonObject, List<ICondition> conditions) {
      if (!conditions.isEmpty()) {
         DataResult<JsonElement> result = LIST_CODEC.encodeStart(jsonOps, conditions);
         JsonElement serializedConditions = (JsonElement)result.result().orElseThrow(() -> new RuntimeException("Failed to serialize conditions"));
         jsonObject.add("neoforge:conditions", serializedConditions);
      }
   }

   boolean test(ICondition.IContext var1);

   MapCodec<? extends ICondition> codec();

   public interface IContext {
      ICondition.IContext EMPTY = new ICondition.IContext() {
         @Override
         public <T> boolean isTagLoaded(TagKey<T> key) {
            return false;
         }
      };
      ICondition.IContext TAGS_INVALID = new ICondition.IContext() {
         @Override
         public <T> boolean isTagLoaded(TagKey<T> key) {
            throw new UnsupportedOperationException("Usage of tag-based conditions is not permitted in this context!");
         }
      };

      <T> boolean isTagLoaded(TagKey<T> var1);

      default RegistryAccess registryAccess() {
         return RegistryAccess.EMPTY;
      }

      default FeatureFlagSet enabledFeatures() {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         return server == null ? FeatureFlags.VANILLA_SET : server.getWorldData().enabledFeatures();
      }
   }
}
