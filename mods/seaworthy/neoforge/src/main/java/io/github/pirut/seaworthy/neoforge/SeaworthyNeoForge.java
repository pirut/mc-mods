package io.github.pirut.seaworthy.neoforge;

import io.github.pirut.seaworthy.Platform;
import io.github.pirut.seaworthy.Seaworthy;
import io.github.pirut.seaworthy.net.ClientFlood;
import io.github.pirut.seaworthy.net.FloodSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.nio.file.Path;

@Mod(Seaworthy.MOD_ID)
public final class SeaworthyNeoForge {
    public SeaworthyNeoForge(IEventBus modBus) {
        Seaworthy.init(new Platform() {
            @Override
            public String loader() {
                return "neoforge";
            }

            @Override
            public Path configDir() {
                return FMLPaths.CONFIGDIR.get();
            }

            @Override
            public void sendFlood(ServerPlayer player, FloodSyncPayload payload) {
                if (player.connection.hasChannel(FloodSyncPayload.TYPE)) PacketDistributor.sendToPlayer(player, payload);
            }
        });
        // Optional: a player without the mod can still join, and just sees Sable's water as before.
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional()
                .playToClient(FloodSyncPayload.TYPE, FloodSyncPayload.CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientFlood.handle(payload))));
    }
}
