package net.neoforged.neoforge.client.event.lifecycle;

import net.minecraft.client.Minecraft;

public class ClientStartedEvent extends ClientLifecycleEvent {
   public ClientStartedEvent(Minecraft client) {
      super(client);
   }
}
