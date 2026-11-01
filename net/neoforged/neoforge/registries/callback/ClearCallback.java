package net.neoforged.neoforge.registries.callback;

import net.minecraft.core.Registry;

@FunctionalInterface
public non-sealed interface ClearCallback<T> extends RegistryCallback<T> {
   void onClear(Registry<T> var1, boolean var2);
}
