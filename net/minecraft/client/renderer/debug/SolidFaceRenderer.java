package net.minecraft.client.renderer.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
      BlockGetter blockGetter = this.minecraft.player.level();
      BlockPos blockPos = BlockPos.containing(camX, camY, camZ);

      for (BlockPos blockPos2 : BlockPos.betweenClosed(blockPos.offset(-6, -6, -6), blockPos.offset(6, 6, 6))) {
         BlockState blockState = blockGetter.getBlockState(blockPos2);
         if (!blockState.is(Blocks.AIR)) {
            VoxelShape voxelShape = blockState.getShape(blockGetter, blockPos2);

            for (AABB aABB : voxelShape.toAabbs()) {
               AABB aABB2 = aABB.move(blockPos2).inflate(0.002);
               int i = -2130771968;
               Vec3 vec3 = aABB2.getMinPosition();
               Vec3 vec32 = aABB2.getMaxPosition();
               addFaceIfSturdy(blockPos2, blockState, blockGetter, Direction.WEST, vec3, vec32, -2130771968);
               addFaceIfSturdy(blockPos2, blockState, blockGetter, Direction.SOUTH, vec3, vec32, -2130771968);
               addFaceIfSturdy(blockPos2, blockState, blockGetter, Direction.EAST, vec3, vec32, -2130771968);
               addFaceIfSturdy(blockPos2, blockState, blockGetter, Direction.NORTH, vec3, vec32, -2130771968);
               addFaceIfSturdy(blockPos2, blockState, blockGetter, Direction.DOWN, vec3, vec32, -2130771968);
               addFaceIfSturdy(blockPos2, blockState, blockGetter, Direction.UP, vec3, vec32, -2130771968);
            }
         }
      }
   }

   private static void addFaceIfSturdy(BlockPos pos, BlockState state, BlockGetter level, Direction direction, Vec3 minPos, Vec3 maxPos, int color) {
      if (state.isFaceSturdy(level, pos, direction)) {
         Gizmos.rect(minPos, maxPos, direction, GizmoStyle.fill(color));
      }
   }
}
