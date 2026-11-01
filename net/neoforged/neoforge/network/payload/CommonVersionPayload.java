package net.neoforged.neoforge.network.payload;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record CommonVersionPayload(List<Integer> versions) implements CustomPacketPayload {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("c", "version");
   public static final CustomPacketPayload.Type<CommonVersionPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static final StreamCodec<FriendlyByteBuf, CommonVersionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CommonVersionPayload::versions, CommonVersionPayload::new
   );

   public CommonVersionPayload() {
      this(NetworkRegistry.SUPPORTED_COMMON_NETWORKING_VERSIONS);
   }

   @Override
   public CustomPacketPayload.Type<CommonVersionPayload> type() {
      return TYPE;
   }
}
