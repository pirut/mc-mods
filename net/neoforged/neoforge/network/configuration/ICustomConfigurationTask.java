package net.neoforged.neoforge.network.configuration;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface ICustomConfigurationTask extends ConfigurationTask {
   void run(Consumer<CustomPacketPayload> var1);

   @Internal
   @NonExtendable
   @Override
   default void start(Consumer<Packet<?>> sender) {
      this.run(payload -> sender.accept(new ClientboundCustomPayloadPacket(payload)));
   }
}
