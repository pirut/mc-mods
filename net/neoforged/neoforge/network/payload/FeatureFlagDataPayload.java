package net.neoforged.neoforge.network.payload;

import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record FeatureFlagDataPayload(Set<Identifier> moddedFlags) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<FeatureFlagDataPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "feature_flags")
   );
   public static final StreamCodec<ByteBuf, FeatureFlagDataPayload> STREAM_CODEC = Identifier.STREAM_CODEC
      .apply(ByteBufCodecs.collection(HashSet::new))
      .map(FeatureFlagDataPayload::new, FeatureFlagDataPayload::moddedFlags);

   @Override
   public CustomPacketPayload.Type<FeatureFlagDataPayload> type() {
      return TYPE;
   }
}
