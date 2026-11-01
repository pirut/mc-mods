package net.minecraft.client.renderer.state;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockOutlineRenderState(
   BlockPos pos,
   boolean isTranslucent,
   boolean highContrast,
   VoxelShape shape,
   @Nullable VoxelShape collisionShape,
   @Nullable VoxelShape occlusionShape,
   @Nullable VoxelShape interactionShape,
   List<CustomBlockOutlineRenderer> customRenderers
) {
   @Deprecated
   public BlockOutlineRenderState(
      BlockPos pos,
      boolean isTranslucent,
      boolean highContrast,
      VoxelShape shape,
      @Nullable VoxelShape collisionShape,
      @Nullable VoxelShape occlusionShape,
      @Nullable VoxelShape interactionShape
   ) {
      this(pos, isTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, List.of());
   }

   @Deprecated
   public BlockOutlineRenderState(BlockPos p_451260_, boolean p_451414_, boolean p_451550_, VoxelShape p_451218_) {
      this(p_451260_, p_451414_, p_451550_, p_451218_, List.of());
   }

   public BlockOutlineRenderState(
      BlockPos p_451260_, boolean p_451414_, boolean p_451550_, VoxelShape p_451218_, List<CustomBlockOutlineRenderer> customRenderers
   ) {
      this(p_451260_, p_451414_, p_451550_, p_451218_, null, null, null, customRenderers);
   }
}
