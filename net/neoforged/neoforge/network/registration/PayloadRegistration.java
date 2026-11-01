package net.neoforged.neoforge.network.registration;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record PayloadRegistration<T extends CustomPacketPayload>(
   CustomPacketPayload.Type<T> type,
   StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
   List<ConnectionProtocol> protocols,
   Optional<PacketFlow> flow,
   String version,
   boolean optional
) {
   public Identifier id() {
      return this.type().id();
   }

   public boolean matchesFlow(PacketFlow flow) {
      return this.flow.isEmpty() || this.flow.get() == flow;
   }
}
