package io.github.pirut.ccagent.neoforge;

import io.github.pirut.ccagent.CCAgent;
import io.github.pirut.ccagent.Platform;
import io.github.pirut.ccagent.Relay;
import io.github.pirut.ccagent.RelayPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
            public void sendToPlayer(ServerPlayer player, RelayPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }

            @Override
            public boolean hasRelay(ServerPlayer player) {
                return player.connection.hasChannel(RelayPayload.TYPE);
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
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional()
            .playBidirectional(RelayPayload.TYPE, RelayPayload.CODEC, (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) Relay.serverReceive(player, payload);
                else Relay.clientReceive(payload);
            }));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) Relay.playerJoined(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) Relay.playerLeft(player);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) NeoForgeClient.init();
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> CCAgent.serverStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> CCAgent.serverStopping());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> CCAgent.registerCommands(e.getDispatcher()));
    }
}
