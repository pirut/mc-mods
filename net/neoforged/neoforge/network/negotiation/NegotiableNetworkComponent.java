package net.neoforged.neoforge.network.negotiation;

import java.util.Optional;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record NegotiableNetworkComponent(Identifier id, String version, Optional<PacketFlow> flow, boolean optional) {
   public NegotiableNetworkComponent(PayloadRegistration<?> reg) {
      this(reg.id(), reg.version(), reg.flow(), reg.optional());
   }

   public NegotiableNetworkComponent(ModdedNetworkQueryComponent query) {
      this(query.id(), query.version(), query.flow(), query.optional());
   }
}
