package net.minecraft.gizmos;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record RectGizmo(Vec3 a, Vec3 b, Vec3 c, Vec3 d, GizmoStyle style) implements Gizmo {
   public static RectGizmo fromCuboidFace(Vec3 p_455016_, Vec3 p_455074_, Direction p_455214_, GizmoStyle p_455964_) {
      return switch (p_455214_) {
         case DOWN -> new RectGizmo(
            new Vec3(p_455016_.x, p_455016_.y, p_455016_.z),
            new Vec3(p_455074_.x, p_455016_.y, p_455016_.z),
            new Vec3(p_455074_.x, p_455016_.y, p_455074_.z),
            new Vec3(p_455016_.x, p_455016_.y, p_455074_.z),
            p_455964_
         );
         case UP -> new RectGizmo(
            new Vec3(p_455016_.x, p_455074_.y, p_455016_.z),
            new Vec3(p_455016_.x, p_455074_.y, p_455074_.z),
            new Vec3(p_455074_.x, p_455074_.y, p_455074_.z),
            new Vec3(p_455074_.x, p_455074_.y, p_455016_.z),
            p_455964_
         );
         case NORTH -> new RectGizmo(
            new Vec3(p_455016_.x, p_455016_.y, p_455016_.z),
            new Vec3(p_455016_.x, p_455074_.y, p_455016_.z),
            new Vec3(p_455074_.x, p_455074_.y, p_455016_.z),
            new Vec3(p_455074_.x, p_455016_.y, p_455016_.z),
            p_455964_
         );
         case SOUTH -> new RectGizmo(
            new Vec3(p_455016_.x, p_455016_.y, p_455074_.z),
            new Vec3(p_455074_.x, p_455016_.y, p_455074_.z),
            new Vec3(p_455074_.x, p_455074_.y, p_455074_.z),
            new Vec3(p_455016_.x, p_455074_.y, p_455074_.z),
            p_455964_
         );
         case WEST -> new RectGizmo(
            new Vec3(p_455016_.x, p_455016_.y, p_455016_.z),
            new Vec3(p_455016_.x, p_455016_.y, p_455074_.z),
            new Vec3(p_455016_.x, p_455074_.y, p_455074_.z),
            new Vec3(p_455016_.x, p_455074_.y, p_455016_.z),
            p_455964_
         );
         case EAST -> new RectGizmo(
            new Vec3(p_455074_.x, p_455016_.y, p_455016_.z),
            new Vec3(p_455074_.x, p_455074_.y, p_455016_.z),
            new Vec3(p_455074_.x, p_455074_.y, p_455074_.z),
            new Vec3(p_455074_.x, p_455016_.y, p_455074_.z),
            p_455964_
         );
      };
   }

   @Override
   public void emit(GizmoPrimitives p_455410_, float p_458133_) {
      if (this.style.hasFill()) {
         int i = this.style.multipliedFill(p_458133_);
         p_455410_.addQuad(this.a, this.b, this.c, this.d, i);
      }

      if (this.style.hasStroke()) {
         int j = this.style.multipliedStroke(p_458133_);
         p_455410_.addLine(this.a, this.b, j, this.style.strokeWidth());
         p_455410_.addLine(this.b, this.c, j, this.style.strokeWidth());
         p_455410_.addLine(this.c, this.d, j, this.style.strokeWidth());
         p_455410_.addLine(this.d, this.a, j, this.style.strokeWidth());
      }
   }
}
