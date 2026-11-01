package net.minecraft.world.level;

public interface ColorMapColorUtil {
   static int get(double p_406263_, double p_406274_, int[] p_406208_, int p_406366_) {
      p_406274_ *= p_406263_;
      int i = (int)((1.0 - p_406263_) * 255.0);
      int j = (int)((1.0 - p_406274_) * 255.0);
      int k = j << 8 | i;
      return k >= p_406208_.length ? p_406366_ : p_406208_[k];
   }
}
