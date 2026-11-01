package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedNetworkPayload(NetworkPayloadSetup setup) implements CustomPacketPayload {
   public static final Identifier ID = Identifier.fromNamespaceAndPath("neoforge", "network");
   public static final CustomPacketPayload.Type<ModdedNetworkPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static final StreamCodec<FriendlyByteBuf, ModdedNetworkPayload> STREAM_CODEC = StreamCodec.composite(
      NetworkPayloadSetup.STREAM_CODEC, ModdedNetworkPayload::setup, ModdedNetworkPayload::new
   );

   @Override
   public CustomPacketPayload.Type<ModdedNetworkPayload> type() {
      return TYPE;
   }
}
