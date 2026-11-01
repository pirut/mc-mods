package net.neoforged.neoforge.client.event.lifecycle;

import net.minecraft.client.Minecraft;

public class ClientStoppingEvent extends ClientLifecycleEvent {
   public ClientStoppingEvent(Minecraft client) {
      super(client);
   }
}
