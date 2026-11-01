package com.mojang.blaze3d.resource;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResourceDescriptor<T> {
   T allocate();

   default void prepare(T p_394130_) {
   }

   void free(T var1);

   default boolean canUsePhysicalResource(ResourceDescriptor<?> p_394583_) {
      return this.equals(p_394583_);
   }
}
