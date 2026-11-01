package net.neoforged.neoforge.client.network;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientPacketDistributor {
   private ClientPacketDistributor() {
   }

   public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
      ClientPacketListener listener = Objects.requireNonNull(Minecraft.getInstance().getConnection());
      Objects.requireNonNull(payload, "Cannot send null payload");
      listener.send(payload);

      for (CustomPacketPayload otherPayload : payloads) {
         Objects.requireNonNull(otherPayload, "Cannot send null payload");
         listener.send(otherPayload);
      }
   }
}
