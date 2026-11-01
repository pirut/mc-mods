package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;

public interface IBlockAndTintGetterExtension {
   private BlockAndTintGetter self() {
      return (BlockAndTintGetter)this;
   }

   default float getShade(float normalX, float normalY, float normalZ, boolean shade) {
      return this.self().getShade(Direction.getApproximateNearest(normalX, normalY, normalZ), shade);
   }
}
