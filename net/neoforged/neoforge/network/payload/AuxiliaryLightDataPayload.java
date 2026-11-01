package net.neoforged.neoforge.network.payload;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record AuxiliaryLightDataPayload(ChunkPos pos, Map<BlockPos, Byte> entries) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<AuxiliaryLightDataPayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "auxiliary_light_data")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, AuxiliaryLightDataPayload> STREAM_CODEC = StreamCodec.composite(
      NeoForgeStreamCodecs.CHUNK_POS,
      AuxiliaryLightDataPayload::pos,
      ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, ByteBufCodecs.BYTE),
      AuxiliaryLightDataPayload::entries,
      AuxiliaryLightDataPayload::new
   );

   @Override
   public CustomPacketPayload.Type<AuxiliaryLightDataPayload> type() {
      return TYPE;
   }
}
