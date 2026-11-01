package net.minecraft.gizmos;

import net.minecraft.util.ARGB;

public record GizmoStyle(int stroke, float strokeWidth, int fill) {
   private static final float DEFAULT_WIDTH = 2.5F;

   public static GizmoStyle stroke(int p_455419_) {
      return new GizmoStyle(p_455419_, 2.5F, 0);
   }

   public static GizmoStyle stroke(int p_454872_, float p_455302_) {
      return new GizmoStyle(p_454872_, p_455302_, 0);
   }

   public static GizmoStyle fill(int p_456202_) {
      return new GizmoStyle(0, 0.0F, p_456202_);
   }

   public static GizmoStyle strokeAndFill(int p_455554_, float p_455482_, int p_455787_) {
      return new GizmoStyle(p_455554_, p_455482_, p_455787_);
   }

   public boolean hasFill() {
      return this.fill != 0;
   }

   public boolean hasStroke() {
      return this.stroke != 0 && this.strokeWidth > 0.0F;
   }

   public int multipliedStroke(float p_457876_) {
      return ARGB.multiplyAlpha(this.stroke, p_457876_);
   }

   public int multipliedFill(float p_458279_) {
      return ARGB.multiplyAlpha(this.fill, p_458279_);
   }
}
