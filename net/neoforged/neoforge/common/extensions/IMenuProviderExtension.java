package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface IMenuProviderExtension {
   default boolean shouldTriggerClientSideContainerClosingOnOpen() {
      return true;
   }

   default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
   }
}
