package com.mojang.blaze3d.resource;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ResourceDescriptor<T> {
   T allocate();

   default void prepare(T target) {
   }

   void free(T target);

   default boolean canUsePhysicalResource(ResourceDescriptor<?> descriptor) {
      return this.equals(descriptor);
   }
}
