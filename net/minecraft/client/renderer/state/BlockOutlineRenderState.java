package net.minecraft.client.renderer.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record BlockOutlineRenderState(
   BlockPos pos,
   boolean isTranslucent,
   boolean highContrast,
   VoxelShape shape,
   @Nullable VoxelShape collisionShape,
   @Nullable VoxelShape occlusionShape,
   @Nullable VoxelShape interactionShape
) {
   public BlockOutlineRenderState(BlockPos pos, boolean isTranslucent, boolean highContrast, VoxelShape shape) {
      this(pos, isTranslucent, highContrast, shape, null, null, null);
   }
}
