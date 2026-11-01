package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistrySnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record FrozenRegistryPayload(ResourceLocation registryName, RegistrySnapshot snapshot) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<FrozenRegistryPayload> TYPE = new CustomPacketPayload.Type<>(
      ResourceLocation.fromNamespaceAndPath("neoforge", "frozen_registry")
   );
   public static final StreamCodec<FriendlyByteBuf, FrozenRegistryPayload> STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC,
      FrozenRegistryPayload::registryName,
      RegistrySnapshot.STREAM_CODEC,
      FrozenRegistryPayload::snapshot,
      FrozenRegistryPayload::new
   );

   @Override
   public CustomPacketPayload.Type<FrozenRegistryPayload> type() {
      return TYPE;
   }
}
