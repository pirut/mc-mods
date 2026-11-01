package net.neoforged.neoforge.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppedEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ClientLifecycleHooks {
   public static void handleClientStarted(Minecraft minecraft) {
      NeoForge.EVENT_BUS.post(new ClientStartedEvent(minecraft));
   }

   public static void handleClientStopping(Minecraft minecraft) {
      NeoForge.EVENT_BUS.post(new ClientStoppingEvent(minecraft));
   }

   public static void handleClientStopped(Minecraft minecraft) {
      NeoForge.EVENT_BUS.post(new ClientStoppedEvent(minecraft));
   }
}
