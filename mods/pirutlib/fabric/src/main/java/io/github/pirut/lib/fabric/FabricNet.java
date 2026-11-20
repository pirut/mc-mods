package io.github.pirut.lib.fabric;

import io.github.pirut.lib.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** {@link Net} on Fabric. Client receivers register in {@link PirutLibFabricClient}. */
final class FabricNet implements Net.Bridge {
    @Override
    public void register(Net.Channel c) {
        if (c.toClient()) {
            PayloadTypeRegistry.playS2C().register(c.type(), c.codec());
        } else {
            PayloadTypeRegistry.playC2S().register(c.type(), c.codec());
            ServerPlayNetworking.registerGlobalReceiver(c.type(), (m, context) -> c.server().accept(context.player(), m.tag()));
        }
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }

    @Override
    public void send(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
