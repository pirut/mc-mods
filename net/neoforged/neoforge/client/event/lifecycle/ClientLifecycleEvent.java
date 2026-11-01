package net.neoforged.neoforge.client.event.lifecycle;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.Event;

public abstract class ClientLifecycleEvent extends Event {
   protected final Minecraft client;

   public ClientLifecycleEvent(Minecraft client) {
      this.client = client;
   }

   public Minecraft getClient() {
      return this.client;
   }
}
