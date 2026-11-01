package net.neoforged.neoforge.network.payload;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record RegistryDataMapSyncPayload<T>(ResourceKey<? extends Registry<T>> registryKey, Map<Identifier, Map<ResourceKey<T>, ?>> dataMaps)
   implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<RegistryDataMapSyncPayload<?>> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "registry_data_map_sync")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RegistryDataMapSyncPayload<?>> STREAM_CODEC = StreamCodec.ofMember(
      RegistryDataMapSyncPayload::write, RegistryDataMapSyncPayload::decode
   );
   private static final Gson GSON = new Gson();

   public static <T> RegistryDataMapSyncPayload<T> decode(RegistryFriendlyByteBuf buf) {
      ResourceKey<Registry<T>> registryKey = (ResourceKey<Registry<T>>)buf.readRegistryKey();
      Map<Identifier, Map<ResourceKey<T>, ?>> attach = buf.readMap(
         FriendlyByteBuf::readIdentifier,
         (b1, key) -> {
            DataMapType<T, ?> dataMap = RegistryManager.getDataMap(registryKey, key);
            return b1.readMap(
               bf -> bf.readResourceKey(registryKey), bf -> readJsonWithRegistryCodec((RegistryFriendlyByteBuf)bf, (Codec<T>)dataMap.networkCodec())
            );
         }
      );
      return new RegistryDataMapSyncPayload<>(registryKey, attach);
   }

   public void write(RegistryFriendlyByteBuf buf) {
      buf.writeResourceKey(this.registryKey);
      buf.writeMap(
         this.dataMaps,
         FriendlyByteBuf::writeIdentifier,
         (b1, key, attach) -> {
            DataMapType<T, ?> dataMap = RegistryManager.getDataMap(this.registryKey, key);
            b1.writeMap(
               attach,
               FriendlyByteBuf::writeResourceKey,
               (bf, value) -> writeJsonWithRegistryCodec((RegistryFriendlyByteBuf)bf, (Codec<T>)dataMap.networkCodec(), (T)value)
            );
         }
      );
   }

   @Override
   public CustomPacketPayload.Type<RegistryDataMapSyncPayload<?>> type() {
      return TYPE;
   }

   private static <T> T readJsonWithRegistryCodec(RegistryFriendlyByteBuf buf, Codec<T> codec) {
      JsonElement jsonelement = GsonHelper.fromJson(GSON, buf.readUtf(), JsonElement.class);
      DataResult<T> dataresult = codec.parse(buf.registryAccess().createSerializationContext(JsonOps.INSTANCE), jsonelement);
      return (T)dataresult.getOrThrow(name -> new DecoderException("Failed to decode json: " + name));
   }

   private static <T> void writeJsonWithRegistryCodec(RegistryFriendlyByteBuf buf, Codec<T> codec, T value) {
      DataResult<JsonElement> dataresult = codec.encodeStart(buf.registryAccess().createSerializationContext(JsonOps.INSTANCE), value);
      buf.writeUtf(GSON.toJson((JsonElement)dataresult.getOrThrow(message -> new EncoderException("Failed to encode: " + message + " " + value))));
   }
}
