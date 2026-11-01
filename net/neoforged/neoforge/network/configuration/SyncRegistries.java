package net.neoforged.neoforge.network.configuration;

import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.payload.FrozenRegistrySyncCompletedPayload;
import net.neoforged.neoforge.network.payload.FrozenRegistrySyncStartPayload;
import net.neoforged.neoforge.registries.RegistryManager;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record SyncRegistries() implements ICustomConfigurationTask {
   private static final Identifier ID = Identifier.fromNamespaceAndPath("neoforge", "sync_registries");
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ID);

   @Override
   public void run(Consumer<CustomPacketPayload> sender) {
      sender.accept(new FrozenRegistrySyncStartPayload(RegistryManager.getRegistryNamesForSyncToClient()));
      RegistryManager.generateRegistryPackets(false).forEach(sender);
      sender.accept(FrozenRegistrySyncCompletedPayload.INSTANCE);
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
