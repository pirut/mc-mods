package net.minecraft.util;

public class TickThrottler {
   private final int incrementStep;
   private final int threshold;
   private int count;

   public TickThrottler(int p_374110_, int p_374074_) {
      this.incrementStep = p_374110_;
      this.threshold = p_374074_;
   }

   public void increment() {
      this.count = this.count + this.incrementStep;
   }

   public void tick() {
      if (this.count > 0) {
         this.count--;
      }
   }

   public boolean isUnderThreshold() {
      return this.count < this.threshold;
   }
}
