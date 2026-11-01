package net.neoforged.neoforge.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IEntityWithComplexSpawn {
   void writeSpawnData(RegistryFriendlyByteBuf var1);

   void readSpawnData(RegistryFriendlyByteBuf var1);
}
