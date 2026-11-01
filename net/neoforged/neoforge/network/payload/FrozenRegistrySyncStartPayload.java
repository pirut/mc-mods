package net.neoforged.neoforge.network.payload;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record FrozenRegistrySyncStartPayload(List<ResourceLocation> toAccess) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<FrozenRegistrySyncStartPayload> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath("neoforge", "frozen_registry_sync_start")
   );
   public static final StreamCodec<FriendlyByteBuf, FrozenRegistrySyncStartPayload> STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), FrozenRegistrySyncStartPayload::toAccess, FrozenRegistrySyncStartPayload::new
   );

   @Override
   public CustomPacketPayload.Type<FrozenRegistrySyncStartPayload> type() {
      return TYPE;
   }
}
