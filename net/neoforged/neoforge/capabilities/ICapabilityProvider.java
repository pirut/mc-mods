package net.neoforged.neoforge.capabilities;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ICapabilityProvider<O, C, T> {
   @Nullable
   T getCapability(O var1, C var2);
}
