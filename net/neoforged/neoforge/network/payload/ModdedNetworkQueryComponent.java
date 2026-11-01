package net.neoforged.neoforge.network.payload;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedNetworkQueryComponent(ResourceLocation id, String version, Optional<PacketFlow> flow, boolean optional) {
   public static final StreamCodec<FriendlyByteBuf, ModdedNetworkQueryComponent> STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC,
      ModdedNetworkQueryComponent::id,
      ByteBufCodecs.STRING_UTF8,
      ModdedNetworkQueryComponent::version,
      ByteBufCodecs.optional(NeoForgeStreamCodecs.enumCodec(PacketFlow.class)),
      ModdedNetworkQueryComponent::flow,
      ByteBufCodecs.BOOL,
      ModdedNetworkQueryComponent::optional,
      ModdedNetworkQueryComponent::new
   );

   public ModdedNetworkQueryComponent(PayloadRegistration<?> reg) {
      this(reg.id(), reg.version(), reg.flow(), reg.optional());
   }
}
