package net.minecraft.client.model;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelUtils {
   public static float rotlerpRad(float p_103126_, float p_103127_, float p_103128_) {
      float $$3 = p_103127_ - p_103126_;

      while ($$3 < (float) -Math.PI) {
         $$3 += (float) (Math.PI * 2);
      }

      while ($$3 >= (float) Math.PI) {
         $$3 -= (float) (Math.PI * 2);
      }

      return p_103126_ + p_103128_ * $$3;
   }
}
