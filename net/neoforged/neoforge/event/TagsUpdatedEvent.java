package net.neoforged.neoforge.event;

import net.minecraft.core.HolderLookup;
import net.neoforged.bus.api.Event;

public class TagsUpdatedEvent extends Event {
   private final HolderLookup.Provider lookupProvider;
   private final TagsUpdatedEvent.UpdateCause updateCause;
   private final boolean integratedServer;

   public TagsUpdatedEvent(HolderLookup.Provider lookupProvider, boolean fromClientPacket, boolean isIntegratedServerConnection) {
      this.lookupProvider = lookupProvider;
      this.updateCause = fromClientPacket ? TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED : TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD;
      this.integratedServer = isIntegratedServerConnection;
   }

   public HolderLookup.Provider getLookupProvider() {
      return this.lookupProvider;
   }

   public TagsUpdatedEvent.UpdateCause getUpdateCause() {
      return this.updateCause;
   }

   public boolean shouldUpdateStaticData() {
      return this.updateCause == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD || !this.integratedServer;
   }

   public static enum UpdateCause {
      SERVER_DATA_LOAD,
      CLIENT_PACKET_RECEIVED;
   }
}
