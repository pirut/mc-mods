package io.github.pirut.seaworthy.fabric;

import io.github.pirut.seaworthy.Platform;
import io.github.pirut.seaworthy.Seaworthy;
import io.github.pirut.seaworthy.net.FloodSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class SeaworthyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(FloodSyncPayload.TYPE, FloodSyncPayload.CODEC);
        Seaworthy.init(new Platform() {
            @Override
            public String loader() {
                return "fabric";
            }

            @Override
            public Path configDir() {
                return FabricLoader.getInstance().getConfigDir();
            }

            @Override
            public void sendFlood(ServerPlayer player, FloodSyncPayload payload) {
                if (ServerPlayNetworking.canSend(player, FloodSyncPayload.TYPE)) ServerPlayNetworking.send(player, payload);
            }
        });
    }
}
