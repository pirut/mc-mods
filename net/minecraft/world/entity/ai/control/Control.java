package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;

public interface Control {
   default float rotateTowards(float p_371698_, float p_371703_, float p_371518_) {
      float f = Mth.degreesDifference(p_371698_, p_371703_);
      float f1 = Mth.clamp(f, -p_371518_, p_371518_);
      return p_371698_ + f1;
   }
}
