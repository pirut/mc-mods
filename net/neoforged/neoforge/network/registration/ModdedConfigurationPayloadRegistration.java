package net.neoforged.neoforge.network.registration;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedConfigurationPayloadRegistration<T extends CustomPacketPayload>(
   Identifier id, Class<T> type, IPayloadHandler<T> handler, StreamCodec<FriendlyByteBuf, T> reader
) {
}
