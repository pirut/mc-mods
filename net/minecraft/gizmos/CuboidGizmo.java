package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CuboidGizmo(AABB aabb, GizmoStyle style, boolean coloredCornerStroke) implements Gizmo {
   @Override
   public void emit(GizmoPrimitives primitives, float opacity) {
      double d = this.aabb.minX;
      double e = this.aabb.minY;
      double f = this.aabb.minZ;
      double g = this.aabb.maxX;
      double h = this.aabb.maxY;
      double i = this.aabb.maxZ;
      if (this.style.hasFill()) {
         int j = this.style.multipliedFill(opacity);
         primitives.addQuad(new Vec3(g, e, f), new Vec3(g, h, f), new Vec3(g, h, i), new Vec3(g, e, i), j);
         primitives.addQuad(new Vec3(d, e, f), new Vec3(d, e, i), new Vec3(d, h, i), new Vec3(d, h, f), j);
         primitives.addQuad(new Vec3(d, e, f), new Vec3(d, h, f), new Vec3(g, h, f), new Vec3(g, e, f), j);
         primitives.addQuad(new Vec3(d, e, i), new Vec3(g, e, i), new Vec3(g, h, i), new Vec3(d, h, i), j);
         primitives.addQuad(new Vec3(d, h, f), new Vec3(d, h, i), new Vec3(g, h, i), new Vec3(g, h, f), j);
         primitives.addQuad(new Vec3(d, e, f), new Vec3(g, e, f), new Vec3(g, e, i), new Vec3(d, e, i), j);
      }

      if (this.style.hasStroke()) {
         int j = this.style.multipliedStroke(opacity);
         primitives.addLine(new Vec3(d, e, f), new Vec3(g, e, f), this.coloredCornerStroke ? ARGB.multiply(j, -34953) : j, this.style.strokeWidth());
         primitives.addLine(new Vec3(d, e, f), new Vec3(d, h, f), this.coloredCornerStroke ? ARGB.multiply(j, -8913033) : j, this.style.strokeWidth());
         primitives.addLine(new Vec3(d, e, f), new Vec3(d, e, i), this.coloredCornerStroke ? ARGB.multiply(j, -8947713) : j, this.style.strokeWidth());
         primitives.addLine(new Vec3(g, e, f), new Vec3(g, h, f), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(g, h, f), new Vec3(d, h, f), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(d, h, f), new Vec3(d, h, i), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(d, h, i), new Vec3(d, e, i), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(d, e, i), new Vec3(g, e, i), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(g, e, i), new Vec3(g, e, f), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(d, h, i), new Vec3(g, h, i), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(g, e, i), new Vec3(g, h, i), j, this.style.strokeWidth());
         primitives.addLine(new Vec3(g, h, f), new Vec3(g, h, i), j, this.style.strokeWidth());
      }
   }
}
