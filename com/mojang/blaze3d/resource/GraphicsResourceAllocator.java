package com.mojang.blaze3d.resource;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GraphicsResourceAllocator {
   GraphicsResourceAllocator UNPOOLED = new GraphicsResourceAllocator() {
      @Override
      public <T> T acquire(ResourceDescriptor<T> p_362261_) {
         T t = p_362261_.allocate();
         p_362261_.prepare(t);
         return t;
      }

      @Override
      public <T> void release(ResourceDescriptor<T> p_363699_, T p_364295_) {
         p_363699_.free(p_364295_);
      }
   };

   <T> T acquire(ResourceDescriptor<T> var1);

   <T> void release(ResourceDescriptor<T> var1, T var2);
}
