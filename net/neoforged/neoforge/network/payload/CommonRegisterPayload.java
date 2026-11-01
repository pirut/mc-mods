package net.neoforged.neoforge.network.payload;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record CommonRegisterPayload(int version, ConnectionProtocol protocol, Set<ResourceLocation> channels) implements CustomPacketPayload {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("c", "register");
   public static final CustomPacketPayload.Type<CommonRegisterPayload> TYPE = new CustomPacketPayload.Type<>(ID);
   public static final StreamCodec<FriendlyByteBuf, CommonRegisterPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      CommonRegisterPayload::version,
      ByteBufCodecs.STRING_UTF8.map(CommonRegisterPayload::protocolById, ConnectionProtocol::id),
      CommonRegisterPayload::protocol,
      ByteBufCodecs.collection(HashSet::new, ResourceLocation.STREAM_CODEC),
      CommonRegisterPayload::channels,
      CommonRegisterPayload::new
   );

   @Override
   public CustomPacketPayload.Type<CommonRegisterPayload> type() {
      return TYPE;
   }

   @Nullable
   private static ConnectionProtocol protocolById(String id) {
      for (ConnectionProtocol p : ConnectionProtocol.values()) {
         if (p.id().equals(id)) {
            return p;
         }
      }

      return null;
   }
}
