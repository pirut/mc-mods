package net.neoforged.neoforge.network.negotiation;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record NegotiationResult(List<NegotiatedNetworkComponent> components, boolean success, Map<ResourceLocation, Component> failureReasons) {
}
