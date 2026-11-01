package net.neoforged.neoforge.network.registration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record NetworkPayloadSetup(Map<ConnectionProtocol, Map<ResourceLocation, NetworkChannel>> channels) {
   public static StreamCodec<FriendlyByteBuf, NetworkPayloadSetup> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(
         IdentityHashMap::new,
         ByteBufCodecs.idMapper(b -> ConnectionProtocol.values()[b], Enum::ordinal),
         ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, NetworkChannel.STREAM_CODEC)
      ),
      NetworkPayloadSetup::channels,
      NetworkPayloadSetup::new
   );

   public Map<ResourceLocation, NetworkChannel> getChannels(ConnectionProtocol protocol) {
      return this.channels().getOrDefault(protocol, Collections.emptyMap());
   }

   @Nullable
   public NetworkChannel getChannel(ConnectionProtocol protocol, ResourceLocation id) {
      return this.getChannels(protocol).get(id);
   }

   public static NetworkPayloadSetup empty() {
      return new NetworkPayloadSetup(Collections.emptyMap());
   }

   public static NetworkPayloadSetup from(Map<ConnectionProtocol, NegotiationResult> results) {
      Builder<ConnectionProtocol, Map<ResourceLocation, NetworkChannel>> channels = ImmutableMap.builder();

      for (Entry<ConnectionProtocol, NegotiationResult> result : results.entrySet()) {
         Builder<ResourceLocation, NetworkChannel> protocolChannels = ImmutableMap.builder();
         result.getValue().components().forEach(component -> protocolChannels.put(component.id(), new NetworkChannel(component.id(), component.version())));
         channels.put(result.getKey(), protocolChannels.build());
      }

      return new NetworkPayloadSetup(channels.build());
   }
}
