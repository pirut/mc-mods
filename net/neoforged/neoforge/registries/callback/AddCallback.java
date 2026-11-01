package net.neoforged.neoforge.registries.callback;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

@FunctionalInterface
public non-sealed interface AddCallback<T> extends RegistryCallback<T> {
   void onAdd(Registry<T> var1, int var2, ResourceKey<T> var3, T var4);
}
