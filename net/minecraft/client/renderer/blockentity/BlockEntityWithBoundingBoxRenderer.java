package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BlockEntityWithBoundingBoxRenderer<T extends BlockEntity & BoundingBoxRenderable>
   implements BlockEntityRenderer<T, BlockEntityWithBoundingBoxRenderState> {
   public static final int STRUCTURE_VOIDS_COLOR = ARGB.colorFromFloat(0.2F, 0.75F, 0.75F, 1.0F);

   public BlockEntityWithBoundingBoxRenderState createRenderState() {
      return new BlockEntityWithBoundingBoxRenderState();
   }

   public void extractRenderState(
      T blockEntity,
      BlockEntityWithBoundingBoxRenderState blockEntityWithBoundingBoxRenderState,
      float f,
      Vec3 vec3,
      @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, blockEntityWithBoundingBoxRenderState, f, vec3, crumblingOverlay);
      extract(blockEntity, blockEntityWithBoundingBoxRenderState);
   }

   public static <T extends BlockEntity & BoundingBoxRenderable> void extract(T blockEntity, BlockEntityWithBoundingBoxRenderState renderState) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      renderState.isVisible = localPlayer.canUseGameMasterBlocks() || localPlayer.isSpectator();
      renderState.box = blockEntity.getRenderableBox();
      renderState.mode = blockEntity.renderMode();
      BlockPos blockPos = renderState.box.localPos();
      Vec3i vec3i = renderState.box.size();
      BlockPos blockPos2 = renderState.blockPos;
      BlockPos blockPos3 = blockPos2.offset(blockPos);
      if (renderState.isVisible && blockEntity.getLevel() != null && renderState.mode == BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS) {
         renderState.invisibleBlocks = new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[vec3i.getX() * vec3i.getY() * vec3i.getZ()];

         for (int i = 0; i < vec3i.getX(); i++) {
            for (int j = 0; j < vec3i.getY(); j++) {
               for (int k = 0; k < vec3i.getZ(); k++) {
                  int l = k * vec3i.getX() * vec3i.getY() + j * vec3i.getX() + i;
                  BlockState blockState = blockEntity.getLevel().getBlockState(blockPos3.offset(i, j, k));
                  if (blockState.isAir()) {
                     renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR;
                  } else if (blockState.is(Blocks.STRUCTURE_VOID)) {
                     renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID;
                  } else if (blockState.is(Blocks.BARRIER)) {
                     renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER;
                  } else if (blockState.is(Blocks.LIGHT)) {
                     renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT;
                  }
               }
            }
         }
      } else {
         renderState.invisibleBlocks = null;
      }

      if (renderState.isVisible) {
      }

      renderState.structureVoids = null;
   }

   public void submit(
      BlockEntityWithBoundingBoxRenderState blockEntityWithBoundingBoxRenderState,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      CameraRenderState cameraRenderState
   ) {
      if (blockEntityWithBoundingBoxRenderState.isVisible) {
         BoundingBoxRenderable.Mode mode = blockEntityWithBoundingBoxRenderState.mode;
         if (mode != BoundingBoxRenderable.Mode.NONE) {
            BoundingBoxRenderable.RenderableBox renderableBox = blockEntityWithBoundingBoxRenderState.box;
            BlockPos blockPos = renderableBox.localPos();
            Vec3i vec3i = renderableBox.size();
            if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
               float f = 1.0F;
               float g = 0.9F;
               BlockPos blockPos2 = blockPos.offset(vec3i);
               Gizmos.cuboid(
                  new AABB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos2.getX(), blockPos2.getY(), blockPos2.getZ())
                     .move(blockEntityWithBoundingBoxRenderState.blockPos),
                  GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.9F, 0.9F, 0.9F)),
                  true
               );
               this.renderInvisibleBlocks(blockEntityWithBoundingBoxRenderState, blockPos, vec3i);
            }
         }
      }
   }

   private void renderInvisibleBlocks(BlockEntityWithBoundingBoxRenderState renderState, BlockPos pos, Vec3i size) {
      if (renderState.invisibleBlocks != null) {
         BlockPos blockPos = renderState.blockPos;
         BlockPos blockPos2 = blockPos.offset(pos);

         for (int i = 0; i < size.getX(); i++) {
            for (int j = 0; j < size.getY(); j++) {
               for (int k = 0; k < size.getZ(); k++) {
                  int l = k * size.getX() * size.getY() + j * size.getX() + i;
                  BlockEntityWithBoundingBoxRenderState.InvisibleBlockType invisibleBlockType = renderState.invisibleBlocks[l];
                  if (invisibleBlockType != null) {
                     float f = invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR ? 0.05F : 0.0F;
                     double d = blockPos2.getX() + i + 0.45F - f;
                     double e = blockPos2.getY() + j + 0.45F - f;
                     double g = blockPos2.getZ() + k + 0.45F - f;
                     double h = blockPos2.getX() + i + 0.55F + f;
                     double m = blockPos2.getY() + j + 0.55F + f;
                     double n = blockPos2.getZ() + k + 0.55F + f;
                     AABB aABB = new AABB(d, e, g, h, m, n);
                     if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR) {
                        Gizmos.cuboid(aABB, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 1.0F)));
                     } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID) {
                        Gizmos.cuboid(aABB, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0.75F, 0.75F)));
                     } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                        Gizmos.cuboid(aABB, GizmoStyle.stroke(-65536));
                     } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) {
                        Gizmos.cuboid(aABB, GizmoStyle.stroke(-256));
                     }
                  }
               }
            }
         }
      }
   }

   private void renderStructureVoids(BlockEntityWithBoundingBoxRenderState renderState, BlockPos pos, Vec3i size) {
      if (renderState.structureVoids != null) {
         DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(size.getX(), size.getY(), size.getZ());

         for (int i = 0; i < size.getX(); i++) {
            for (int j = 0; j < size.getY(); j++) {
               for (int k = 0; k < size.getZ(); k++) {
                  int l = k * size.getX() * size.getY() + j * size.getX() + i;
                  if (renderState.structureVoids[l]) {
                     discreteVoxelShape.fill(i, j, k);
                  }
               }
            }
         }

         discreteVoxelShape.forAllFaces((direction, ix, jx, kx) -> {
            float f = 0.48F;
            float g = ix + pos.getX() + 0.5F - 0.48F;
            float h = jx + pos.getY() + 0.5F - 0.48F;
            float lx = kx + pos.getZ() + 0.5F - 0.48F;
            float m = ix + pos.getX() + 0.5F + 0.48F;
            float n = jx + pos.getY() + 0.5F + 0.48F;
            float o = kx + pos.getZ() + 0.5F + 0.48F;
            Gizmos.rect(new Vec3(g, h, lx), new Vec3(m, n, o), direction, GizmoStyle.fill(STRUCTURE_VOIDS_COLOR));
         });
      }
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 96;
   }
}
