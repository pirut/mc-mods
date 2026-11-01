package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;

public record ClientboundEntityPositionSyncPacket(int id, PositionMoveRotation values, boolean onGround) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundEntityPositionSyncPacket::id,
      PositionMoveRotation.STREAM_CODEC,
      ClientboundEntityPositionSyncPacket::values,
      ByteBufCodecs.BOOL,
      ClientboundEntityPositionSyncPacket::onGround,
      ClientboundEntityPositionSyncPacket::new
   );

   public static ClientboundEntityPositionSyncPacket of(Entity p_379588_) {
      return new ClientboundEntityPositionSyncPacket(
         p_379588_.getId(),
         new PositionMoveRotation(p_379588_.trackingPosition(), p_379588_.getDeltaMovement(), p_379588_.getYRot(), p_379588_.getXRot()),
         p_379588_.onGround()
      );
   }

   @Override
   public PacketType<ClientboundEntityPositionSyncPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ENTITY_POSITION_SYNC;
   }

   public void handle(ClientGamePacketListener p_379993_) {
      p_379993_.handleEntityPositionSync(this);
   }
}
