package net.minecraft.world.entity.variant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

public record SpawnContext(BlockPos pos, ServerLevelAccessor level, EnvironmentAttributeReader environmentAttributes, Holder<Biome> biome) {
   public static SpawnContext create(ServerLevelAccessor p_400282_, BlockPos p_399599_) {
      Holder<Biome> holder = p_400282_.getBiome(p_399599_);
      return new SpawnContext(p_399599_, p_400282_, p_400282_.environmentAttributes(), holder);
   }
}
