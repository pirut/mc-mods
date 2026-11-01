package com.mojang.blaze3d.resource;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface GraphicsResourceAllocator {
   GraphicsResourceAllocator UNPOOLED = new GraphicsResourceAllocator() {
      @Override
      public <T> T acquire(ResourceDescriptor<T> descriptor) {
         T object = descriptor.allocate();
         descriptor.prepare(object);
         return object;
      }

      @Override
      public <T> void release(ResourceDescriptor<T> descriptor, T value) {
         descriptor.free(value);
      }
   };

   <T> T acquire(ResourceDescriptor<T> descriptor);

   <T> void release(ResourceDescriptor<T> descriptor, T value);
}
