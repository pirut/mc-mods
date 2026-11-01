package net.neoforged.neoforge.client.event.lifecycle;

import net.minecraft.client.Minecraft;

public class ClientStoppedEvent extends ClientLifecycleEvent {
   public ClientStoppedEvent(Minecraft client) {
      super(client);
   }
}
