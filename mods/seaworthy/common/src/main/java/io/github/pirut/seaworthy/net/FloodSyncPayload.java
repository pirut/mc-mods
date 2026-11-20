package io.github.pirut.seaworthy.net;

import io.github.pirut.seaworthy.Seaworthy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * How much water is in each compartment of a ship, from the server. Players' games work out the compartments
 * themselves from the ship's blocks; this keeps their water levels in step with the server's.
 *
 * @param ship    the sub-level
 * @param anchors per compartment, the plot position ({@code BlockPos.asLong}) of one of its cells
 * @param volumes per compartment, its water in blocks
 */
public record FloodSyncPayload(UUID ship, long[] anchors, float[] volumes) implements CustomPacketPayload {
    public static final Type<FloodSyncPayload> TYPE = new Type<>(Seaworthy.id("flood"));
    private static final int MAX_ENTRIES = 4096;

    public static final StreamCodec<FriendlyByteBuf, FloodSyncPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUUID(p.ship);
                int n = Math.min(p.anchors.length, MAX_ENTRIES);
                buf.writeVarInt(n);
                for (int i = 0; i < n; i++) {
                    buf.writeLong(p.anchors[i]);
                    buf.writeFloat(p.volumes[i]);
                }
            },
            buf -> {
                UUID ship = buf.readUUID();
                int n = buf.readVarInt();
                if (n < 0 || n > MAX_ENTRIES) throw new IllegalArgumentException("too many compartments: " + n);
                long[] anchors = new long[n];
                float[] volumes = new float[n];
                for (int i = 0; i < n; i++) {
                    anchors[i] = buf.readLong();
                    volumes[i] = buf.readFloat();
                }
                return new FloodSyncPayload(ship, anchors, volumes);
            });

    @Override
    public Type<FloodSyncPayload> type() {
        return TYPE;
    }
}
