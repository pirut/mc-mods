package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ConfigFilePayload(String fileName, byte[] contents) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<ConfigFilePayload> TYPE = new CustomPacketPayload.Type<>(
      Identifier.fromNamespaceAndPath("neoforge", "config_file")
   );
   public static final StreamCodec<FriendlyByteBuf, ConfigFilePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, ConfigFilePayload::fileName, NeoForgeStreamCodecs.UNBOUNDED_BYTE_ARRAY, ConfigFilePayload::contents, ConfigFilePayload::new
   );

   @Override
   public CustomPacketPayload.Type<ConfigFilePayload> type() {
      return TYPE;
   }
}
