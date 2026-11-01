package net.neoforged.neoforge.common.world;

import net.minecraft.core.BlockPos;

public interface AuxiliaryLightManager {
   void setLightAt(BlockPos var1, int var2);

   default void removeLightAt(BlockPos pos) {
      this.setLightAt(pos, 0);
   }

   int getLightAt(BlockPos var1);
}
