package net.neoforged.neoforge.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class FeatureFlagAcknowledgePayload implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<FeatureFlagAcknowledgePayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "feature_flags_ack")
   );
   public static final FeatureFlagAcknowledgePayload INSTANCE = new FeatureFlagAcknowledgePayload();
   public static final StreamCodec<ByteBuf, FeatureFlagAcknowledgePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   private FeatureFlagAcknowledgePayload() {
   }

   @Override
   public CustomPacketPayload.Type<FeatureFlagAcknowledgePayload> type() {
      return TYPE;
   }
}
