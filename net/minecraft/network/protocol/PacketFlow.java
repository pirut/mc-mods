package net.minecraft.network.protocol;

import net.neoforged.neoforge.common.extensions.IPacketFlowExtension;

public enum PacketFlow implements IPacketFlowExtension {
   SERVERBOUND("serverbound"),
   CLIENTBOUND("clientbound");

   private final String id;

   private PacketFlow(String p_320377_) {
      this.id = p_320377_;
   }

   public PacketFlow getOpposite() {
      return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
   }

   public String id() {
      return this.id;
   }
}
