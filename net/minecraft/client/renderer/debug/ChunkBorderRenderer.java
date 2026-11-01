package net.minecraft.client.renderer.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class ChunkBorderRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float THICK_WIDTH = 4.0F;
   private static final float THIN_WIDTH = 1.0F;
   private final Minecraft minecraft;
   private static final int CELL_BORDER = ARGB.color(255, 0, 155, 155);
   private static final int YELLOW = ARGB.color(255, 255, 255, 0);
   private static final int MAJOR_LINES = ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 1.0F);

   public ChunkBorderRenderer(Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
      Entity entity = this.minecraft.gameRenderer.getMainCamera().entity();
      float f = this.minecraft.level.getMinY();
      float g = this.minecraft.level.getMaxY() + 1;
      SectionPos sectionPos = SectionPos.of(entity.blockPosition());
      double d = sectionPos.minBlockX();
      double e = sectionPos.minBlockZ();

      for (int i = -16; i <= 32; i += 16) {
         for (int j = -16; j <= 32; j += 16) {
            Gizmos.line(new Vec3(d + i, f, e + j), new Vec3(d + i, g, e + j), ARGB.colorFromFloat(0.5F, 1.0F, 0.0F, 0.0F), 4.0F);
         }
      }

      for (int i = 2; i < 16; i += 2) {
         int j = i % 4 == 0 ? CELL_BORDER : YELLOW;
         Gizmos.line(new Vec3(d + i, f, e), new Vec3(d + i, g, e), j, 1.0F);
         Gizmos.line(new Vec3(d + i, f, e + 16.0), new Vec3(d + i, g, e + 16.0), j, 1.0F);
      }

      for (int i = 2; i < 16; i += 2) {
         int j = i % 4 == 0 ? CELL_BORDER : YELLOW;
         Gizmos.line(new Vec3(d, f, e + i), new Vec3(d, g, e + i), j, 1.0F);
         Gizmos.line(new Vec3(d + 16.0, f, e + i), new Vec3(d + 16.0, g, e + i), j, 1.0F);
      }

      for (int i = this.minecraft.level.getMinY(); i <= this.minecraft.level.getMaxY() + 1; i += 2) {
         float h = i;
         int k = i % 8 == 0 ? CELL_BORDER : YELLOW;
         Gizmos.line(new Vec3(d, h, e), new Vec3(d, h, e + 16.0), k, 1.0F);
         Gizmos.line(new Vec3(d, h, e + 16.0), new Vec3(d + 16.0, h, e + 16.0), k, 1.0F);
         Gizmos.line(new Vec3(d + 16.0, h, e + 16.0), new Vec3(d + 16.0, h, e), k, 1.0F);
         Gizmos.line(new Vec3(d + 16.0, h, e), new Vec3(d, h, e), k, 1.0F);
      }

      for (int i = 0; i <= 16; i += 16) {
         for (int j = 0; j <= 16; j += 16) {
            Gizmos.line(new Vec3(d + i, f, e + j), new Vec3(d + i, g, e + j), MAJOR_LINES, 4.0F);
         }
      }

      Gizmos.cuboid(
            new AABB(
               sectionPos.minBlockX(),
               sectionPos.minBlockY(),
               sectionPos.minBlockZ(),
               sectionPos.maxBlockX() + 1,
               sectionPos.maxBlockY() + 1,
               sectionPos.maxBlockZ() + 1
            ),
            GizmoStyle.stroke(MAJOR_LINES, 1.0F)
         )
         .setAlwaysOnTop();

      for (int i = this.minecraft.level.getMinY(); i <= this.minecraft.level.getMaxY() + 1; i += 16) {
         Gizmos.line(new Vec3(d, i, e), new Vec3(d, i, e + 16.0), MAJOR_LINES, 4.0F);
         Gizmos.line(new Vec3(d, i, e + 16.0), new Vec3(d + 16.0, i, e + 16.0), MAJOR_LINES, 4.0F);
         Gizmos.line(new Vec3(d + 16.0, i, e + 16.0), new Vec3(d + 16.0, i, e), MAJOR_LINES, 4.0F);
         Gizmos.line(new Vec3(d + 16.0, i, e), new Vec3(d, i, e), MAJOR_LINES, 4.0F);
      }
   }
}
