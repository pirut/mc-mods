package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameTestBlockHighlightRenderer {
   private static final int SHOW_POS_DURATION_MS = 10000;
   private static final float PADDING = 0.02F;
   private final Map<BlockPos, GameTestBlockHighlightRenderer.Marker> markers = Maps.newHashMap();

   public void highlightPos(BlockPos p_450992_, BlockPos p_451181_) {
      String s = p_451181_.toShortString();
      this.markers.put(p_450992_, new GameTestBlockHighlightRenderer.Marker(1610678016, s, Util.getMillis() + 10000L));
   }

   public void clear() {
      this.markers.clear();
   }

   public void emitGizmos() {
      long i = Util.getMillis();
      this.markers.entrySet().removeIf(p_451551_ -> i > p_451551_.getValue().removeAtTime);
      this.markers.forEach((p_454301_, p_454302_) -> this.renderMarker(p_454301_, p_454302_));
   }

   private void renderMarker(BlockPos p_451482_, GameTestBlockHighlightRenderer.Marker p_451040_) {
      Gizmos.cuboid(p_451482_, 0.02F, GizmoStyle.fill(p_451040_.color()));
      if (!p_451040_.text.isEmpty()) {
         Gizmos.billboardText(p_451040_.text, Vec3.atLowerCornerWithOffset(p_451482_, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F))
            .setAlwaysOnTop();
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Marker(int color, String text, long removeAtTime) {
   }
}
