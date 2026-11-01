package net.neoforged.neoforge.network.configuration;

import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.ConfigSync;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record SyncConfig(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
   private static final Identifier ID = Identifier.fromNamespaceAndPath("neoforge", "sync_config");
   public static ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ID);

   @Override
   public void run(Consumer<CustomPacketPayload> sender) {
      ConfigSync.syncAllConfigs(this.listener);
      this.listener().finishCurrentTask(this.type());
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
