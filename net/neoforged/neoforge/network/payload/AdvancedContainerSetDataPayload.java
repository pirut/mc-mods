package net.neoforged.neoforge.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record AdvancedContainerSetDataPayload(byte containerId, short dataId, int value) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<AdvancedContainerSetDataPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "advanced_container_set_data")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedContainerSetDataPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BYTE,
      AdvancedContainerSetDataPayload::containerId,
      ByteBufCodecs.SHORT,
      AdvancedContainerSetDataPayload::dataId,
      ByteBufCodecs.VAR_INT,
      AdvancedContainerSetDataPayload::value,
      AdvancedContainerSetDataPayload::new
   );

   @Override
   public CustomPacketPayload.Type<AdvancedContainerSetDataPayload> type() {
      return TYPE;
   }

   public ClientboundContainerSetDataPacket toVanillaPacket() {
      return new ClientboundContainerSetDataPacket(this.containerId, this.dataId, this.value);
   }
}
