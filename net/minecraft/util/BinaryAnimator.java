package net.minecraft.util;

public class BinaryAnimator {
   private final int animationLength;
   private final EasingType easing;
   private int ticks;
   private int ticksOld;

   public BinaryAnimator(int p_363393_, EasingType p_467608_) {
      this.animationLength = p_363393_;
      this.easing = p_467608_;
   }

   public BinaryAnimator(int p_361095_) {
      this(p_361095_, EasingType.LINEAR);
   }

   public void tick(boolean p_363738_) {
      this.ticksOld = this.ticks;
      if (p_363738_) {
         if (this.ticks < this.animationLength) {
            this.ticks++;
         }
      } else if (this.ticks > 0) {
         this.ticks--;
      }
   }

   public float getFactor(float p_360599_) {
      float f = Mth.lerp(p_360599_, (float)this.ticksOld, (float)this.ticks) / this.animationLength;
      return this.easing.apply(f);
   }
}
