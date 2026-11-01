package net.neoforged.neoforge.network.registration;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ModdedPlayPayloadRegistration<T extends CustomPacketPayload>(
   Identifier id, Class<T> type, IPayloadHandler<T> handler, StreamCodec<RegistryFriendlyByteBuf, T> reader
) {
}
