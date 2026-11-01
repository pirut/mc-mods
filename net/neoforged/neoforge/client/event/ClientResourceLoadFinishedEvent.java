package net.neoforged.neoforge.client.event;

import net.neoforged.bus.api.Event;

public class ClientResourceLoadFinishedEvent extends Event {
   private final boolean initial;

   public ClientResourceLoadFinishedEvent(boolean initial) {
      this.initial = initial;
   }

   public boolean isInitial() {
      return this.initial;
   }
}
