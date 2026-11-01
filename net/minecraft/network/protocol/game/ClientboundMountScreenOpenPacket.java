package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundMountScreenOpenPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundMountScreenOpenPacket> STREAM_CODEC = Packet.codec(
      ClientboundMountScreenOpenPacket::write, ClientboundMountScreenOpenPacket::new
   );
   private final int containerId;
   private final int inventoryColumns;
   private final int entityId;

   public ClientboundMountScreenOpenPacket(int p_470596_, int p_470609_, int p_470844_) {
      this.containerId = p_470596_;
      this.inventoryColumns = p_470609_;
      this.entityId = p_470844_;
   }

   private ClientboundMountScreenOpenPacket(FriendlyByteBuf p_470688_) {
      this.containerId = p_470688_.readContainerId();
      this.inventoryColumns = p_470688_.readVarInt();
      this.entityId = p_470688_.readInt();
   }

   private void write(FriendlyByteBuf p_470768_) {
      p_470768_.writeContainerId(this.containerId);
      p_470768_.writeVarInt(this.inventoryColumns);
      p_470768_.writeInt(this.entityId);
   }

   @Override
   public PacketType<ClientboundMountScreenOpenPacket> type() {
      return GamePacketTypes.CLIENTBOUND_MOUNT_SCREEN_OPEN;
   }

   public void handle(ClientGamePacketListener p_470599_) {
      p_470599_.handleMountScreenOpen(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getInventoryColumns() {
      return this.inventoryColumns;
   }

   public int getEntityId() {
      return this.entityId;
   }
}
