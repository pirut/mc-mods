package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class FrozenRegistrySyncCompletedPayload implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<FrozenRegistrySyncCompletedPayload> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath("neoforge", "frozen_registry_sync_completed")
   );
   public static final FrozenRegistrySyncCompletedPayload INSTANCE = new FrozenRegistrySyncCompletedPayload();
   public static final StreamCodec<FriendlyByteBuf, FrozenRegistrySyncCompletedPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   private FrozenRegistrySyncCompletedPayload() {
   }

   @Override
   public CustomPacketPayload.Type<FrozenRegistrySyncCompletedPayload> type() {
      return TYPE;
   }
}
