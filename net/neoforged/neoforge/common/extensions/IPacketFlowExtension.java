package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.fml.LogicalSide;

public interface IPacketFlowExtension {
   default PacketFlow self() {
      return (PacketFlow)this;
   }

   default boolean isClientbound() {
      return this.self() == PacketFlow.CLIENTBOUND;
   }

   default boolean isServerbound() {
      return this.self() == PacketFlow.SERVERBOUND;
   }

   default LogicalSide getReceptionSide() {
      return this.isServerbound() ? LogicalSide.SERVER : LogicalSide.CLIENT;
   }
}
