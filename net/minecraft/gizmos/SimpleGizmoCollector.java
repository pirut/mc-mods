package net.minecraft.gizmos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class SimpleGizmoCollector implements GizmoCollector {
   private final List<SimpleGizmoCollector.GizmoInstance> gizmos = new ArrayList<>();
   private final List<SimpleGizmoCollector.GizmoInstance> temporaryGizmos = new ArrayList<>();

   @Override
   public GizmoProperties add(Gizmo gizmo) {
      SimpleGizmoCollector.GizmoInstance gizmoInstance = new SimpleGizmoCollector.GizmoInstance(gizmo);
      this.gizmos.add(gizmoInstance);
      return gizmoInstance;
   }

   public List<SimpleGizmoCollector.GizmoInstance> drainGizmos() {
      ArrayList<SimpleGizmoCollector.GizmoInstance> arrayList = new ArrayList<>(this.gizmos);
      arrayList.addAll(this.temporaryGizmos);
      long l = Util.getMillis();
      this.gizmos.removeIf(gizmoInstance -> gizmoInstance.getExpireTimeMillis() < l);
      this.temporaryGizmos.clear();
      return arrayList;
   }

   public List<SimpleGizmoCollector.GizmoInstance> getGizmos() {
      return this.gizmos;
   }

   public void addTemporaryGizmos(Collection<SimpleGizmoCollector.GizmoInstance> gizmos) {
      this.temporaryGizmos.addAll(gizmos);
   }

   public static class GizmoInstance implements GizmoProperties {
      private final Gizmo gizmo;
      private boolean isAlwaysOnTop;
      private long startTimeMillis;
      private long expireTimeMillis;
      private boolean shouldFadeOut;

      GizmoInstance(Gizmo gizmo) {
         this.gizmo = gizmo;
      }

      @Override
      public GizmoProperties setAlwaysOnTop() {
         this.isAlwaysOnTop = true;
         return this;
      }

      @Override
      public GizmoProperties persistForMillis(int millis) {
         this.startTimeMillis = Util.getMillis();
         this.expireTimeMillis = this.startTimeMillis + millis;
         return this;
      }

      @Override
      public GizmoProperties fadeOut() {
         this.shouldFadeOut = true;
         return this;
      }

      public float getAlphaMultiplier(long currentTime) {
         if (this.shouldFadeOut) {
            long l = this.expireTimeMillis - this.startTimeMillis;
            long m = currentTime - this.startTimeMillis;
            return 1.0F - Mth.clamp((float)m / (float)l, 0.0F, 1.0F);
         } else {
            return 1.0F;
         }
      }

      public boolean isAlwaysOnTop() {
         return this.isAlwaysOnTop;
      }

      public long getExpireTimeMillis() {
         return this.expireTimeMillis;
      }

      public Gizmo gizmo() {
         return this.gizmo;
      }
   }
}
