package io.github.pirut.ccagent.fabric;

import io.github.pirut.ccagent.CCAgent;
import io.github.pirut.ccagent.Platform;
import io.github.pirut.ccagent.Relay;
import io.github.pirut.ccagent.RelayPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
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
            public void sendToPlayer(ServerPlayer player, RelayPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }

            @Override
            public boolean hasRelay(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, RelayPayload.TYPE);
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
        Payloads.register();
        ServerPlayNetworking.registerGlobalReceiver(RelayPayload.TYPE, (payload, context) -> Relay.serverReceive(context.player(), payload));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Relay.playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Relay.playerLeft(handler.player));
        ServerLifecycleEvents.SERVER_STARTED.register(CCAgent::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CCAgent.serverStopping());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> CCAgent.registerCommands(dispatcher));
    }
}
