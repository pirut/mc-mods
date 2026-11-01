package net.neoforged.neoforge.network.negotiation;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record NegotiatedNetworkComponent(Identifier id, String version) {
}
