package io.github.pirut.lib;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Small messages between server and client, each a {@link CompoundTag} on a named channel. Channels are optional:
 * players without the mod can still join, and {@link #canSend} says whether a player's client listens on one.
 *
 * <p>Register channels while the mod starts up. Client handlers run on the client's game thread; keep client-only
 * classes inside the handler's lambda body so a dedicated server never loads them.
 */
public final class Net {
    private Net() {
    }

    public record Message(CustomPacketPayload.Type<Message> type, CompoundTag tag) implements CustomPacketPayload {
    }

    public record Channel(ResourceLocation id, CustomPacketPayload.Type<Message> type,
                          StreamCodec<RegistryFriendlyByteBuf, Message> codec, boolean toClient,
                          Consumer<CompoundTag> client, BiConsumer<ServerPlayer, CompoundTag> server) {
    }

    /** What the loader glue provides. */
    public interface Bridge {
        void register(Channel channel);

        boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

        void send(ServerPlayer player, CustomPacketPayload payload);

        void sendToServer(CustomPacketPayload payload);
    }

    private static final List<Channel> CHANNELS = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, Channel> BY_ID = new ConcurrentHashMap<>();
    private static Bridge bridge;
    private static boolean eager;

    /** A channel from server to client. */
    public static void toClient(ResourceLocation id, Consumer<CompoundTag> clientHandler) {
        add(channel(id, true, clientHandler, null));
    }

    /** A channel from client to server. */
    public static void toServer(ResourceLocation id, BiConsumer<ServerPlayer, CompoundTag> serverHandler) {
        add(channel(id, false, null, serverHandler));
    }

    public static boolean canSend(ServerPlayer player, ResourceLocation id) {
        Channel c = BY_ID.get(id);
        return c != null && bridge != null && bridge.canSend(player, c.type());
    }

    /** Sends to a player whose client listens on the channel; does nothing otherwise. */
    public static void send(ServerPlayer player, ResourceLocation id, CompoundTag tag) {
        Channel c = BY_ID.get(id);
        if (c != null && canSend(player, id)) bridge.send(player, new Message(c.type(), tag));
    }

    /** From the client, to the server. */
    public static void sendToServer(ResourceLocation id, CompoundTag tag) {
        Channel c = BY_ID.get(id);
        if (c != null && bridge != null) bridge.sendToServer(new Message(c.type(), tag));
    }

    public static List<Channel> channels() {
        return CHANNELS;
    }

    /** For the loader glue. Channels added before and after are handed to it. */
    public static synchronized void bridge(Bridge b) {
        bridge = b;
        eager = true;
        for (Channel c : CHANNELS) b.register(c);
    }

    /** For loader glue that registers channels at a set time instead of as they come. */
    public static synchronized void bridgeLater(Bridge b) {
        bridge = b;
    }

    private static synchronized void add(Channel c) {
        if (BY_ID.putIfAbsent(c.id(), c) != null) throw new IllegalStateException("Channel " + c.id() + " is taken");
        CHANNELS.add(c);
        if (bridge != null && eager) bridge.register(c);
    }

    private static Channel channel(ResourceLocation id, boolean toClient, Consumer<CompoundTag> client,
                                   BiConsumer<ServerPlayer, CompoundTag> server) {
        CustomPacketPayload.Type<Message> type = new CustomPacketPayload.Type<>(id);
        StreamCodec<RegistryFriendlyByteBuf, Message> codec = StreamCodec.of(
                (buf, m) -> buf.writeNbt(m.tag()),
                buf -> {
                    CompoundTag t = buf.readNbt();
                    return new Message(type, t == null ? new CompoundTag() : t);
                });
        return new Channel(id, type, codec, toClient, client, server);
    }
}
