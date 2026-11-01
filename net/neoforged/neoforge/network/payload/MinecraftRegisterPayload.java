package net.neoforged.neoforge.network.payload;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record MinecraftRegisterPayload(Set<ResourceLocation> newChannels) implements CustomPacketPayload {
   public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("register");
   public static final CustomPacketPayload.Type<MinecraftRegisterPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static final StreamCodec<FriendlyByteBuf, MinecraftRegisterPayload> STREAM_CODEC = DinnerboneProtocolUtils.CHANNELS_CODEC
      .map(MinecraftRegisterPayload::new, MinecraftRegisterPayload::newChannels);

   @Override
   public CustomPacketPayload.Type<MinecraftRegisterPayload> type() {
      return TYPE;
   }
}
