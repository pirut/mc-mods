package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
      double d = Util.getNanos();
      if (d - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = d;
         Entity entity = this.minecraft.gameRenderer.getMainCamera().entity();
         this.surroundEntities = ImmutableList.copyOf(entity.level().getEntities(entity, entity.getBoundingBox().inflate(16.0)));
      }

      Player player = this.minecraft.player;
      if (player != null && player.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(player, () -> 0.0, -65536);
      }

      for (Entity entity2 : this.surroundEntities) {
         if (entity2 != player) {
            this.drawHighlights(entity2, () -> this.getBias(entity2), -16711936);
         }
      }
   }

   private void drawHighlights(Entity entity, DoubleSupplier biasGetter, int color) {
      entity.mainSupportingBlockPos.ifPresent(blockPos -> {
         double d = biasGetter.getAsDouble();
         BlockPos blockPos2 = entity.getOnPos();
         this.highlightPosition(blockPos2, 0.02 + d, color);
         BlockPos blockPos3 = entity.getOnPosLegacy();
         if (!blockPos3.equals(blockPos2)) {
            this.highlightPosition(blockPos3, 0.04 + d, -16711681);
         }
      });
   }

   private double getBias(Entity entity) {
      return 0.02 * (String.valueOf(entity.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
   }

   private void highlightPosition(BlockPos pos, double bias, int color) {
      double d = pos.getX() - 2.0 * bias;
      double e = pos.getY() - 2.0 * bias;
      double f = pos.getZ() - 2.0 * bias;
      double g = d + 1.0 + 4.0 * bias;
      double h = e + 1.0 + 4.0 * bias;
      double i = f + 1.0 + 4.0 * bias;
      Gizmos.cuboid(new AABB(d, e, f, g, h, i), GizmoStyle.stroke(ARGB.color(0.4F, color)));
      VoxelShape voxelShape = this.minecraft.level.getBlockState(pos).getCollisionShape(this.minecraft.level, pos, CollisionContext.empty()).move(pos);
      GizmoStyle gizmoStyle = GizmoStyle.stroke(color);

      for (AABB aABB : voxelShape.toAabbs()) {
         Gizmos.cuboid(aABB, gizmoStyle);
      }
   }
}
