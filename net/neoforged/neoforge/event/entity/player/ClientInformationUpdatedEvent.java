package net.neoforged.neoforge.event.entity.player;

import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;

public class ClientInformationUpdatedEvent extends PlayerEvent {
   private final ClientInformation oldInformation;
   private final ClientInformation updatedInformation;

   public ClientInformationUpdatedEvent(ServerPlayer player, ClientInformation oldInfo, ClientInformation newInfo) {
      super(player);
      this.oldInformation = oldInfo;
      this.updatedInformation = newInfo;
   }

   public ServerPlayer getEntity() {
      return (ServerPlayer)super.getEntity();
   }

   public ClientInformation getUpdatedInformation() {
      return this.updatedInformation;
   }

   public ClientInformation getOldInformation() {
      return this.oldInformation;
   }
}
