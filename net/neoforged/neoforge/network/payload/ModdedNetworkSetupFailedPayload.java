package net.neoforged.neoforge.network.payload;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedNetworkSetupFailedPayload(Map<ResourceLocation, Component> failureReasons) implements CustomPacketPayload {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("neoforge", "modded_network_setup_failed");
   public static final CustomPacketPayload.Type<ModdedNetworkSetupFailedPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static final StreamCodec<FriendlyByteBuf, ModdedNetworkSetupFailedPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC),
      ModdedNetworkSetupFailedPayload::failureReasons,
      ModdedNetworkSetupFailedPayload::new
   );

   @Override
   public CustomPacketPayload.Type<ModdedNetworkSetupFailedPayload> type() {
      return TYPE;
   }
}
