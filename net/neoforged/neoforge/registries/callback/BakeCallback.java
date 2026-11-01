package net.neoforged.neoforge.registries.callback;

import net.minecraft.core.Registry;

@FunctionalInterface
public non-sealed interface BakeCallback<T> extends RegistryCallback<T> {
   void onBake(Registry<T> var1);
}
