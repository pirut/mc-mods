package net.neoforged.neoforge.network.event;

import java.util.LinkedList;
import java.util.Queue;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterConfigurationTasksEvent extends Event implements IModBusEvent {
   private final ServerConfigurationPacketListener listener;
   private final Queue<ConfigurationTask> configurationTasks = new LinkedList<>();

   @Internal
   public RegisterConfigurationTasksEvent(ServerConfigurationPacketListener listener) {
      this.listener = listener;
   }

   public void register(ConfigurationTask task) {
      this.configurationTasks.add(task);
   }

   @Internal
   public Queue<ConfigurationTask> getConfigurationTasks() {
      return new LinkedList<>(this.configurationTasks);
   }

   public ServerConfigurationPacketListener getListener() {
      return this.listener;
   }
}
