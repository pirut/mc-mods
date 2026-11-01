package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record SplitPacketPayload(byte[] payload) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<SplitPacketPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("neoforge", "split"));
   public static final StreamCodec<FriendlyByteBuf, SplitPacketPayload> STREAM_CODEC = StreamCodec.composite(
      NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY, SplitPacketPayload::payload, SplitPacketPayload::new
   );

   @Override
   public CustomPacketPayload.Type<SplitPacketPayload> type() {
      return TYPE;
   }
}
