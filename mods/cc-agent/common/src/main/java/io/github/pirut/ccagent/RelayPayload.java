package io.github.pirut.ccagent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One piece of a relay message (JSON, split to fit Minecraft's packet size limits). */
public record RelayPayload(int message, int part, int parts, byte[] data) implements CustomPacketPayload {
    public static final Type<RelayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CCAgent.MOD_ID, "relay"));

    public static final StreamCodec<FriendlyByteBuf, RelayPayload> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeVarInt(p.message);
            buf.writeVarInt(p.part);
            buf.writeVarInt(p.parts);
            buf.writeByteArray(p.data);
        },
        buf -> new RelayPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(Relay.CHUNK)));

    @Override
    public Type<RelayPayload> type() {
        return TYPE;
    }
}
