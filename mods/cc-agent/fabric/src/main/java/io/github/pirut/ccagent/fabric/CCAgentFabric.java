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
            public Path gameDir() {
                return FabricLoader.getInstance().getGameDir();
            }

            @Override
            public String loader() {
                return "fabric";
            }

            @Override
            public ModMeta mod(String modId) {
                return FabricLoader.getInstance().getModContainer(modId).map(container -> {
                    var meta = container.getMetadata();
                    var contact = meta.getContact();
                    return new ModMeta(modId, meta.getName(), meta.getVersion().getFriendlyString(),
                        contact.get("homepage").orElse(null), contact.get("sources").orElse(null), contact.get("issues").orElse(null));
                }).orElse(null);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(CCAgent::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CCAgent.serverStopping());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> CCAgent.registerCommands(dispatcher));
    }
}
