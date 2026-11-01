package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ClientboundSelectAdvancementsTabPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSelectAdvancementsTabPacket> STREAM_CODEC = Packet.codec(
      ClientboundSelectAdvancementsTabPacket::write, ClientboundSelectAdvancementsTabPacket::new
   );
   @Nullable
   private final Identifier tab;

   public ClientboundSelectAdvancementsTabPacket(@Nullable Identifier p_468452_) {
      this.tab = p_468452_;
   }

   private ClientboundSelectAdvancementsTabPacket(FriendlyByteBuf p_179198_) {
      this.tab = p_179198_.readNullable(FriendlyByteBuf::readIdentifier);
   }

   private void write(FriendlyByteBuf p_133015_) {
      p_133015_.writeNullable(this.tab, FriendlyByteBuf::writeIdentifier);
   }

   @Override
   public PacketType<ClientboundSelectAdvancementsTabPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SELECT_ADVANCEMENTS_TAB;
   }

   public void handle(ClientGamePacketListener p_133012_) {
      p_133012_.handleSelectAdvancementsTab(this);
   }

   @Nullable
   public Identifier getTab() {
      return this.tab;
   }
}
