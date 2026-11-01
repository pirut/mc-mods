package net.neoforged.neoforge.network.payload;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record MinecraftUnregisterPayload(Set<ResourceLocation> forgottenChannels) implements CustomPacketPayload {
   public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("unregister");
   public static final CustomPacketPayload.Type<MinecraftUnregisterPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static final StreamCodec<FriendlyByteBuf, MinecraftUnregisterPayload> STREAM_CODEC = DinnerboneProtocolUtils.CHANNELS_CODEC
      .map(MinecraftUnregisterPayload::new, MinecraftUnregisterPayload::forgottenChannels);

   @Override
   public CustomPacketPayload.Type<MinecraftUnregisterPayload> type() {
      return TYPE;
   }
}
