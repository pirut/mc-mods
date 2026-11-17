package io.github.pirut.lib.neoforge;

import io.github.pirut.lib.Net;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** {@link Net} on NeoForge. Channels register in {@code RegisterPayloadHandlersEvent}. */
final class NeoForgeNet implements Net.Bridge {
    @Override
    public void register(Net.Channel channel) {
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type);
    }

    @Override
    public void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
