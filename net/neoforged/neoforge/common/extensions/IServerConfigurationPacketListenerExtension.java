package net.neoforged.neoforge.common.extensions;

import net.minecraft.server.network.ConfigurationTask;

public interface IServerConfigurationPacketListenerExtension extends IServerCommonPacketListenerExtension {
   void finishCurrentTask(ConfigurationTask.Type var1);
}
