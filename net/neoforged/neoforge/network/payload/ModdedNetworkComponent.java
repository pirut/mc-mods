package net.neoforged.neoforge.network.payload;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedNetworkComponent(ResourceLocation id, Optional<String> version) {
   public static final StreamCodec<FriendlyByteBuf, ModdedNetworkComponent> STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC,
      ModdedNetworkComponent::id,
      ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
      ModdedNetworkComponent::version,
      ModdedNetworkComponent::new
   );
}
