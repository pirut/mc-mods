package net.minecraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class ScrollWheelHandler {
   private double accumulatedScrollX;
   private double accumulatedScrollY;

   public Vector2i onMouseScroll(double p_361026_, double p_363216_) {
      if (this.accumulatedScrollX != 0.0 && Math.signum(p_361026_) != Math.signum(this.accumulatedScrollX)) {
         this.accumulatedScrollX = 0.0;
      }

      if (this.accumulatedScrollY != 0.0 && Math.signum(p_363216_) != Math.signum(this.accumulatedScrollY)) {
         this.accumulatedScrollY = 0.0;
      }

      this.accumulatedScrollX += p_361026_;
      this.accumulatedScrollY += p_363216_;
      int i = (int)this.accumulatedScrollX;
      int j = (int)this.accumulatedScrollY;
      if (i == 0 && j == 0) {
         return new Vector2i(0, 0);
      } else {
         this.accumulatedScrollX -= i;
         this.accumulatedScrollY -= j;
         return new Vector2i(i, j);
      }
   }

   public static int getNextScrollWheelSelection(double p_360640_, int p_363247_, int p_363136_) {
      int i = (int)Math.signum(p_360640_);
      p_363247_ -= i;
      p_363247_ = Math.max(-1, p_363247_);

      while (p_363247_ < 0) {
         p_363247_ += p_363136_;
      }

      while (p_363247_ >= p_363136_) {
         p_363247_ -= p_363136_;
      }

      return p_363247_;
   }
}
