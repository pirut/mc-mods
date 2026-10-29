package io.github.pirut.ccagent.neoforge;

import io.github.pirut.ccagent.CCAgent;
import io.github.pirut.ccagent.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;

@Mod(CCAgent.MOD_ID)
public final class CCAgentNeoForge {
    public CCAgentNeoForge(IEventBus modBus) {
        CCAgent.init(new Platform() {
            @Override
            public Path configDir() {
                return FMLPaths.CONFIGDIR.get();
            }

            @Override
            public Path gameDir() {
                return FMLPaths.GAMEDIR.get();
            }

            @Override
            public String loader() {
                return "neoforge";
            }

            @Override
            public ModMeta mod(String modId) {
                return ModList.get().getModContainerById(modId).map(container -> {
                    var info = container.getModInfo();
                    var homepage = info.getModURL().map(Object::toString).orElse(null);
                    var issues = info.getOwningFile().getConfig().<String>getConfigElement("issueTrackerURL").orElse(null);
                    return new ModMeta(modId, info.getDisplayName(), info.getVersion().toString(), homepage, null, issues);
                }).orElse(null);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> CCAgent.serverStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> CCAgent.serverStopping());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> CCAgent.registerCommands(e.getDispatcher()));
    }
}
