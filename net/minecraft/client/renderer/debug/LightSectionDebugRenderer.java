package net.minecraft.client.renderer.debug;

import java.time.Duration;
import java.time.Instant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LightSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
   private static final int RADIUS = 10;
   private static final int LIGHT_AND_BLOCKS_COLOR = ARGB.colorFromFloat(0.25F, 1.0F, 1.0F, 0.0F);
   private static final int LIGHT_ONLY_COLOR = ARGB.colorFromFloat(0.125F, 0.25F, 0.125F, 0.0F);
   private final Minecraft minecraft;
   private final LightLayer lightLayer;
   private Instant lastUpdateTime = Instant.now();
   @Nullable
   private LightSectionDebugRenderer.SectionData data;

   public LightSectionDebugRenderer(Minecraft minecraft, LightLayer lightLayer) {
      this.minecraft = minecraft;
      this.lightLayer = lightLayer;
   }

   @Override
   public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
      Instant instant = Instant.now();
      if (this.data == null || Duration.between(this.lastUpdateTime, instant).compareTo(REFRESH_INTERVAL) > 0) {
         this.lastUpdateTime = instant;
         this.data = new LightSectionDebugRenderer.SectionData(
            this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer
         );
      }

      renderEdges(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
      renderEdges(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
      renderFaces(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
      renderFaces(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
   }

   private static void renderFaces(DiscreteVoxelShape shape, SectionPos pos, int color) {
      shape.forAllFaces((direction, j, k, l) -> {
         int m = j + pos.getX();
         int n = k + pos.getY();
         int o = l + pos.getZ();
         renderFace(direction, m, n, o, color);
      });
   }

   private static void renderEdges(DiscreteVoxelShape shape, SectionPos pos, int color) {
      shape.forAllEdges((j, k, l, m, n, o) -> {
         int p = j + pos.getX();
         int q = k + pos.getY();
         int r = l + pos.getZ();
         int s = m + pos.getX();
         int t = n + pos.getY();
         int u = o + pos.getZ();
         renderEdge(p, q, r, s, t, u, color);
      }, true);
   }

   private static void renderFace(Direction direction, int x, int y, int z, int color) {
      Vec3 vec3 = new Vec3(SectionPos.sectionToBlockCoord(x), SectionPos.sectionToBlockCoord(y), SectionPos.sectionToBlockCoord(z));
      Vec3 vec32 = vec3.add(16.0, 16.0, 16.0);
      Gizmos.rect(vec3, vec32, direction, GizmoStyle.fill(color));
   }

   private static void renderEdge(int x1, int y1, int z1, int x2, int y2, int z2, int color) {
      double d = SectionPos.sectionToBlockCoord(x1);
      double e = SectionPos.sectionToBlockCoord(y1);
      double f = SectionPos.sectionToBlockCoord(z1);
      double g = SectionPos.sectionToBlockCoord(x2);
      double h = SectionPos.sectionToBlockCoord(y2);
      double i = SectionPos.sectionToBlockCoord(z2);
      int j = ARGB.opaque(color);
      Gizmos.line(new Vec3(d, e, f), new Vec3(g, h, i), j);
   }

   @Environment(EnvType.CLIENT)
   static final class SectionData {
      final DiscreteVoxelShape lightAndBlocksShape;
      final DiscreteVoxelShape lightShape;
      final SectionPos minPos;

      SectionData(LevelLightEngine levelLightEngine, SectionPos pos, int radius, LightLayer lightLayer) {
         int i = radius * 2 + 1;
         this.lightAndBlocksShape = new BitSetDiscreteVoxelShape(i, i, i);
         this.lightShape = new BitSetDiscreteVoxelShape(i, i, i);

         for (int j = 0; j < i; j++) {
            for (int k = 0; k < i; k++) {
               for (int l = 0; l < i; l++) {
                  SectionPos sectionPos = SectionPos.of(pos.x() + l - radius, pos.y() + k - radius, pos.z() + j - radius);
                  LayerLightSectionStorage.SectionType sectionType = levelLightEngine.getDebugSectionType(lightLayer, sectionPos);
                  if (sectionType == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                     this.lightAndBlocksShape.fill(l, k, j);
                     this.lightShape.fill(l, k, j);
                  } else if (sectionType == LayerLightSectionStorage.SectionType.LIGHT_ONLY) {
                     this.lightShape.fill(l, k, j);
                  }
               }
            }
         }

         this.minPos = SectionPos.of(pos.x() - radius, pos.y() - radius, pos.z() - radius);
      }
   }
}
