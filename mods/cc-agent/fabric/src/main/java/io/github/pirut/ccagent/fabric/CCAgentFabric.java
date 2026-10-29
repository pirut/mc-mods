package io.github.pirut.ccagent.fabric;

import io.github.pirut.ccagent.CCAgent;
import io.github.pirut.ccagent.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class CCAgentFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CCAgent.init(new Platform() {
            @Override
            public Path configDir() {
                return FabricLoader.getInstance().getConfigDir();
            }

            @Override
            public String loader() {
                return "fabric";
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(CCAgent::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CCAgent.serverStopping());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> CCAgent.registerCommands(dispatcher));
    }
}
