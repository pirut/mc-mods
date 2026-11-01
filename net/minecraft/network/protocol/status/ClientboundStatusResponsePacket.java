package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jetbrains.annotations.Nullable;

public record ClientboundStatusResponsePacket(ServerStatus status, @Nullable String cachedStatus) implements Packet<ClientStatusPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundStatusResponsePacket> STREAM_CODEC = Packet.codec(
      ClientboundStatusResponsePacket::write, ClientboundStatusResponsePacket::new
   );

   public ClientboundStatusResponsePacket(ServerStatus status) {
      this(status, null);
   }

   private ClientboundStatusResponsePacket(FriendlyByteBuf p_179834_) {
      this(p_179834_.readJsonWithCodec(ServerStatus.CODEC));
   }

   private void write(FriendlyByteBuf p_134899_) {
      if (this.cachedStatus != null) {
         p_134899_.writeUtf(this.cachedStatus);
      } else {
         p_134899_.writeJsonWithCodec(ServerStatus.CODEC, this.status);
      }
   }

   @Override
   public PacketType<ClientboundStatusResponsePacket> type() {
      return StatusPacketTypes.CLIENTBOUND_STATUS_RESPONSE;
   }

   public void handle(ClientStatusPacketListener p_134896_) {
      p_134896_.handleStatusResponse(this);
   }
}
