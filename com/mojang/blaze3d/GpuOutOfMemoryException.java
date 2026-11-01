package com.mojang.blaze3d;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GpuOutOfMemoryException extends RuntimeException {
   public GpuOutOfMemoryException(String p_405843_) {
      super(p_405843_);
   }
}
