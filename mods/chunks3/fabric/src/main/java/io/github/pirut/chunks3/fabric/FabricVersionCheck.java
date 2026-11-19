package io.github.pirut.chunks3.fabric;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.net.CubePayloads;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;

/**
 * On Fabric, the check NeoForge makes of {@link CubePayloads#VERSION} for its registered payloads: while a player
 * joins, a server with Chunks3 sends its version to a client that has Chunks3 too, the client answers with its own,
 * and the server refuses a client whose version differs, saying why, rather than let far columns and cubes come in a
 * form the other side reads wrongly. A client or server without Chunks3 is not asked and joins as it always has.
 */
final class FabricVersionCheck {
    private FabricVersionCheck() {}

    record Version(String version) implements CustomPacketPayload {
        static final Type<Version> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Cubic.MOD_ID, "version"));
        static final StreamCodec<FriendlyByteBuf, Version> CODEC = ByteBufCodecs.STRING_UTF8.<FriendlyByteBuf>cast().map(Version::new, Version::version);
        @Override public Type<Version> type() { return TYPE; }
    }

    private static final ConfigurationTask.Type TASK = new ConfigurationTask.Type(Cubic.MOD_ID + ":version");

    /** Both sides, from the main entry point. */
    static void register() {
        PayloadTypeRegistry.configurationS2C().register(Version.TYPE, Version.CODEC);
        PayloadTypeRegistry.configurationC2S().register(Version.TYPE, Version.CODEC);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, Version.TYPE)) return;
            handler.addTask(new ConfigurationTask() {
                @Override public void start(Consumer<Packet<?>> send) { send.accept(ServerConfigurationNetworking.createS2CPacket(new Version(CubePayloads.VERSION))); }
                @Override public Type type() { return TASK; }
            });
        });
        ServerConfigurationNetworking.registerGlobalReceiver(Version.TYPE, (payload, context) -> {
            if (CubePayloads.VERSION.equals(payload.version())) context.networkHandler().completeTask(TASK);
            else context.networkHandler().disconnect(mismatch(CubePayloads.VERSION, payload.version()));
        });
    }

    /** The client's side, from the client entry point: it answers with its own version. */
    static void registerClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(Version.TYPE, (payload, context) -> {
            if (!CubePayloads.VERSION.equals(payload.version()))
                Cubic.LOGGER.warn("This server's Chunks3 speaks {}, this game's {}; the server will refuse the join", payload.version(), CubePayloads.VERSION);
            context.responseSender().sendPacket(new Version(CubePayloads.VERSION));
        });
    }

    static Component mismatch(String server, String client) {
        return Component.literal("This server's Chunks3 and yours differ (network version " + server + " here, " + client
                + " in your game). Install the same Chunks3 build as the server.");
    }
}
