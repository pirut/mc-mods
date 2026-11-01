package net.neoforged.neoforge.network.negotiation;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record NegotiatedNetworkComponent(ResourceLocation id, String version) {
}
