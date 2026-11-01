package net.minecraft.network.protocol.game;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

public record ClientboundTeleportEntityPacket(int id, PositionMoveRotation change, Set<Relative> relatives, boolean onGround)
   implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundTeleportEntityPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundTeleportEntityPacket::id,
      PositionMoveRotation.STREAM_CODEC,
      ClientboundTeleportEntityPacket::change,
      Relative.SET_STREAM_CODEC,
      ClientboundTeleportEntityPacket::relatives,
      ByteBufCodecs.BOOL,
      ClientboundTeleportEntityPacket::onGround,
      ClientboundTeleportEntityPacket::new
   );

   public static ClientboundTeleportEntityPacket teleport(int p_379814_, PositionMoveRotation p_379503_, Set<Relative> p_379757_, boolean p_380240_) {
      return new ClientboundTeleportEntityPacket(p_379814_, p_379503_, p_379757_, p_380240_);
   }

   @Override
   public PacketType<ClientboundTeleportEntityPacket> type() {
      return GamePacketTypes.CLIENTBOUND_TELEPORT_ENTITY;
   }

   public void handle(ClientGamePacketListener p_133544_) {
      p_133544_.handleTeleportEntity(this);
   }
}
