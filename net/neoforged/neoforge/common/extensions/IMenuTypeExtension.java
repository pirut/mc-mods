package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;

public interface IMenuTypeExtension<T> {
   static <T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory) {
      return new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS);
   }

   T create(int var1, Inventory var2, RegistryFriendlyByteBuf var3);
}
