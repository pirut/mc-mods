package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public interface BoundingBoxRenderable {
   BoundingBoxRenderable.Mode renderMode();

   BoundingBoxRenderable.RenderableBox getRenderableBox();

   public static enum Mode {
      NONE,
      BOX,
      BOX_AND_INVISIBLE_BLOCKS;
   }

   public record RenderableBox(BlockPos localPos, Vec3i size) {
      public static BoundingBoxRenderable.RenderableBox fromCorners(int p_397940_, int p_397363_, int p_397619_, int p_397665_, int p_397058_, int p_397720_) {
         int i = Math.min(p_397940_, p_397665_);
         int j = Math.min(p_397363_, p_397058_);
         int k = Math.min(p_397619_, p_397720_);
         return new BoundingBoxRenderable.RenderableBox(
            new BlockPos(i, j, k), new Vec3i(Math.max(p_397940_, p_397665_) - i, Math.max(p_397363_, p_397058_) - j, Math.max(p_397619_, p_397720_) - k)
         );
      }
   }
}
