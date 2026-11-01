package net.neoforged.neoforge.network.configuration;

import java.util.function.Consumer;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.payload.CommonRegisterPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record CommonRegisterTask() implements ICustomConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath("neoforge", "common_register"));

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }

   @Override
   public void run(Consumer<CustomPacketPayload> sender) {
      sender.accept(new CommonRegisterPayload(1, ConnectionProtocol.PLAY, NetworkRegistry.getCommonPlayChannels(PacketFlow.SERVERBOUND)));
   }
}
