package net.minecraft.gizmos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Gizmos {
   static final ThreadLocal<GizmoCollector> collector = new ThreadLocal<>();

   private Gizmos() {
   }

   public static Gizmos.TemporaryCollection withCollector(GizmoCollector p_455674_) {
      Gizmos.TemporaryCollection gizmos$temporarycollection = new Gizmos.TemporaryCollection();
      collector.set(p_455674_);
      return gizmos$temporarycollection;
   }

   public static GizmoProperties addGizmo(Gizmo p_455031_) {
      GizmoCollector gizmocollector = collector.get();
      if (gizmocollector == null) {
         throw new IllegalStateException("Gizmos cannot be created here! No GizmoCollector has been registered.");
      } else {
         return gizmocollector.add(p_455031_);
      }
   }

   public static GizmoProperties cuboid(AABB p_456037_, GizmoStyle p_456227_) {
      return cuboid(p_456037_, p_456227_, false);
   }

   public static GizmoProperties cuboid(AABB p_456072_, GizmoStyle p_455892_, boolean p_456186_) {
      return addGizmo(new CuboidGizmo(p_456072_, p_455892_, p_456186_));
   }

   public static GizmoProperties cuboid(BlockPos p_454665_, GizmoStyle p_455400_) {
      return cuboid(new AABB(p_454665_), p_455400_);
   }

   public static GizmoProperties cuboid(BlockPos p_454722_, float p_455226_, GizmoStyle p_454660_) {
      return cuboid(new AABB(p_454722_).inflate(p_455226_), p_454660_);
   }

   public static GizmoProperties circle(Vec3 p_455996_, float p_454719_, GizmoStyle p_455498_) {
      return addGizmo(new CircleGizmo(p_455996_, p_454719_, p_455498_));
   }

   public static GizmoProperties line(Vec3 p_455046_, Vec3 p_456182_, int p_456161_) {
      return addGizmo(new LineGizmo(p_455046_, p_456182_, p_456161_, 3.0F));
   }

   public static GizmoProperties line(Vec3 p_455835_, Vec3 p_455795_, int p_456058_, float p_456176_) {
      return addGizmo(new LineGizmo(p_455835_, p_455795_, p_456058_, p_456176_));
   }

   public static GizmoProperties arrow(Vec3 p_456044_, Vec3 p_455201_, int p_455095_) {
      return addGizmo(new ArrowGizmo(p_456044_, p_455201_, p_455095_, 2.5F));
   }

   public static GizmoProperties arrow(Vec3 p_455606_, Vec3 p_456038_, int p_455726_, float p_455634_) {
      return addGizmo(new ArrowGizmo(p_455606_, p_456038_, p_455726_, p_455634_));
   }

   public static GizmoProperties rect(Vec3 p_456092_, Vec3 p_455375_, Direction p_455798_, GizmoStyle p_454916_) {
      return addGizmo(RectGizmo.fromCuboidFace(p_456092_, p_455375_, p_455798_, p_454916_));
   }

   public static GizmoProperties rect(Vec3 p_454690_, Vec3 p_455866_, Vec3 p_455356_, Vec3 p_456121_, GizmoStyle p_456005_) {
      return addGizmo(new RectGizmo(p_454690_, p_455866_, p_455356_, p_456121_, p_456005_));
   }

   public static GizmoProperties point(Vec3 p_455574_, int p_454835_, float p_456102_) {
      return addGizmo(new PointGizmo(p_455574_, p_454835_, p_456102_));
   }

   public static GizmoProperties billboardTextOverBlock(String p_454700_, BlockPos p_456130_, int p_455514_, int p_455024_, float p_454992_) {
      double d0 = 1.3;
      double d1 = 0.2;
      GizmoProperties gizmoproperties = billboardText(
         p_454700_,
         Vec3.atLowerCornerWithOffset(p_456130_, 0.5, 1.3 + p_455514_ * 0.2, 0.5),
         TextGizmo.Style.forColorAndCentered(p_455024_).withScale(p_454992_)
      );
      gizmoproperties.setAlwaysOnTop();
      return gizmoproperties;
   }

   public static GizmoProperties billboardTextOverMob(Entity p_456287_, int p_454669_, String p_456137_, int p_455680_, float p_454783_) {
      double d0 = 2.4;
      double d1 = 0.25;
      double d2 = p_456287_.getBlockX() + 0.5;
      double d3 = p_456287_.getY() + 2.4 + p_454669_ * 0.25;
      double d4 = p_456287_.getBlockZ() + 0.5;
      float f = 0.5F;
      GizmoProperties gizmoproperties = billboardText(
         p_456137_, new Vec3(d2, d3, d4), TextGizmo.Style.forColor(p_455680_).withScale(p_454783_).withLeftAlignment(0.5F)
      );
      gizmoproperties.setAlwaysOnTop();
      return gizmoproperties;
   }

   public static GizmoProperties billboardText(String p_454970_, Vec3 p_455955_, TextGizmo.Style p_455384_) {
      return addGizmo(new TextGizmo(p_455955_, p_454970_, p_455384_));
   }

   public static class TemporaryCollection implements AutoCloseable {
      @Nullable
      private final GizmoCollector old = Gizmos.collector.get();
      private boolean closed;

      TemporaryCollection() {
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            Gizmos.collector.set(this.old);
         }
      }
   }
}
