package net.minecraft.client.renderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RunningTrimmedMean {
   private final long[] values;
   private int count;
   private int cursor;

   public RunningTrimmedMean(int p_110711_) {
      this.values = new long[p_110711_];
   }

   public long registerValueAndGetMean(long p_110713_) {
      if (this.count < this.values.length) {
         this.count++;
      }

      this.values[this.cursor] = p_110713_;
      this.cursor = (this.cursor + 1) % this.values.length;
      long $$1 = Long.MAX_VALUE;
      long $$2 = Long.MIN_VALUE;
      long $$3 = 0L;

      for (int $$4 = 0; $$4 < this.count; $$4++) {
         long $$5 = this.values[$$4];
         $$3 += $$5;
         $$1 = Math.min($$1, $$5);
         $$2 = Math.max($$2, $$5);
      }

      if (this.count > 2) {
         $$3 -= $$1 + $$2;
         return $$3 / (this.count - 2);
      } else {
         return $$3 > 0L ? this.count / $$3 : 0L;
      }
   }
}
