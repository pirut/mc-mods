package net.minecraft.world.entity;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class InterpolationHandler {
   public static final int DEFAULT_INTERPOLATION_STEPS = 3;
   private final Entity entity;
   private int interpolationSteps;
   private final InterpolationHandler.InterpolationData interpolationData = new InterpolationHandler.InterpolationData(0, Vec3.ZERO, 0.0F, 0.0F);
   @Nullable
   private Vec3 previousTickPosition;
   @Nullable
   private Vec2 previousTickRot;
   @Nullable
   private final Consumer<InterpolationHandler> onInterpolationStart;

   public InterpolationHandler(Entity p_394439_) {
      this(p_394439_, 3, null);
   }

   public InterpolationHandler(Entity p_394433_, int p_394544_) {
      this(p_394433_, p_394544_, null);
   }

   public InterpolationHandler(Entity p_394101_, @Nullable Consumer<InterpolationHandler> p_394503_) {
      this(p_394101_, 3, p_394503_);
   }

   public InterpolationHandler(Entity p_393992_, int p_394386_, @Nullable Consumer<InterpolationHandler> p_393601_) {
      this.interpolationSteps = p_394386_;
      this.entity = p_393992_;
      this.onInterpolationStart = p_393601_;
   }

   public Vec3 position() {
      return this.interpolationData.steps > 0 ? this.interpolationData.position : this.entity.position();
   }

   public float yRot() {
      return this.interpolationData.steps > 0 ? this.interpolationData.yRot : this.entity.getYRot();
   }

   public float xRot() {
      return this.interpolationData.steps > 0 ? this.interpolationData.xRot : this.entity.getXRot();
   }

   public void interpolateTo(Vec3 p_393836_, float p_394255_, float p_393907_) {
      if (this.interpolationSteps == 0) {
         this.entity.snapTo(p_393836_, p_394255_, p_393907_);
         this.cancel();
      } else if (!this.hasActiveInterpolation()
         || !Objects.equals(this.yRot(), p_394255_)
         || !Objects.equals(this.xRot(), p_393907_)
         || !Objects.equals(this.position(), p_393836_)) {
         this.interpolationData.steps = this.interpolationSteps;
         this.interpolationData.position = p_393836_;
         this.interpolationData.yRot = p_394255_;
         this.interpolationData.xRot = p_393907_;
         this.previousTickPosition = this.entity.position();
         this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
         if (this.onInterpolationStart != null) {
            this.onInterpolationStart.accept(this);
         }
      }
   }

   public boolean hasActiveInterpolation() {
      return this.interpolationData.steps > 0;
   }

   public void setInterpolationLength(int p_393476_) {
      this.interpolationSteps = p_393476_;
   }

   public void interpolate() {
      if (!this.hasActiveInterpolation()) {
         this.cancel();
      } else {
         double d0 = 1.0 / this.interpolationData.steps;
         if (this.previousTickPosition != null) {
            Vec3 vec3 = this.entity.position().subtract(this.previousTickPosition);
            if (this.entity.level().noCollision(this.entity, this.entity.makeBoundingBox(this.interpolationData.position.add(vec3)))) {
               this.interpolationData.addDelta(vec3);
            }
         }

         if (this.previousTickRot != null) {
            float f3 = this.entity.getYRot() - this.previousTickRot.y;
            float f = this.entity.getXRot() - this.previousTickRot.x;
            this.interpolationData.addRotation(f3, f);
         }

         double d3 = Mth.lerp(d0, this.entity.getX(), this.interpolationData.position.x);
         double d1 = Mth.lerp(d0, this.entity.getY(), this.interpolationData.position.y);
         double d2 = Mth.lerp(d0, this.entity.getZ(), this.interpolationData.position.z);
         Vec3 vec31 = new Vec3(d3, d1, d2);
         float f1 = (float)Mth.rotLerp(d0, (double)this.entity.getYRot(), (double)this.interpolationData.yRot);
         float f2 = (float)Mth.lerp(d0, (double)this.entity.getXRot(), (double)this.interpolationData.xRot);
         this.entity.setPos(vec31);
         this.entity.setRot(f1, f2);
         this.interpolationData.decrease();
         this.previousTickPosition = vec31;
         this.previousTickRot = new Vec2(this.entity.getXRot(), this.entity.getYRot());
      }
   }

   public void cancel() {
      this.interpolationData.steps = 0;
      this.previousTickPosition = null;
      this.previousTickRot = null;
   }

   static class InterpolationData {
      protected int steps;
      Vec3 position;
      float yRot;
      float xRot;

      InterpolationData(int p_394434_, Vec3 p_393933_, float p_393655_, float p_393861_) {
         this.steps = p_394434_;
         this.position = p_393933_;
         this.yRot = p_393655_;
         this.xRot = p_393861_;
      }

      public void decrease() {
         this.steps--;
      }

      public void addDelta(Vec3 p_394401_) {
         this.position = this.position.add(p_394401_);
      }

      public void addRotation(float p_393925_, float p_393734_) {
         this.yRot += p_393925_;
         this.xRot += p_393734_;
      }
   }
}
