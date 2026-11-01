package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record PointGizmo(Vec3 pos, int color, float size) implements Gizmo {
   @Override
   public void emit(GizmoPrimitives primitives, float opacity) {
      primitives.addPoint(this.pos, ARGB.multiplyAlpha(this.color, opacity), this.size);
   }
}
