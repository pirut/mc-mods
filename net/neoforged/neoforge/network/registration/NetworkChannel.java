package net.neoforged.neoforge.network.registration;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record NetworkChannel(ResourceLocation id, String chosenVersion) {
   public static final StreamCodec<FriendlyByteBuf, NetworkChannel> STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC, NetworkChannel::id, ByteBufCodecs.STRING_UTF8, NetworkChannel::chosenVersion, NetworkChannel::new
   );
}
