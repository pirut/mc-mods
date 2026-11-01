package com.mojang.blaze3d.resource;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrossFrameResourcePool implements GraphicsResourceAllocator, AutoCloseable {
   private final int framesToKeepResource;
   private final Deque<CrossFrameResourcePool.ResourceEntry<?>> pool = new ArrayDeque<>();

   public CrossFrameResourcePool(int p_360291_) {
      this.framesToKeepResource = p_360291_;
   }

   public void endFrame() {
      Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

      while (iterator.hasNext()) {
         CrossFrameResourcePool.ResourceEntry<?> resourceentry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
         if (resourceentry.framesToLive-- == 0) {
            resourceentry.close();
            iterator.remove();
         }
      }
   }

   @Override
   public <T> T acquire(ResourceDescriptor<T> p_365509_) {
      T t = this.acquireWithoutPreparing(p_365509_);
      p_365509_.prepare(t);
      return t;
   }

   private <T> T acquireWithoutPreparing(ResourceDescriptor<T> p_394528_) {
      Iterator<? extends CrossFrameResourcePool.ResourceEntry<?>> iterator = this.pool.iterator();

      while (iterator.hasNext()) {
         CrossFrameResourcePool.ResourceEntry<?> resourceentry = (CrossFrameResourcePool.ResourceEntry<?>)iterator.next();
         if (p_394528_.canUsePhysicalResource(resourceentry.descriptor)) {
            iterator.remove();
            return (T)resourceentry.value;
         }
      }

      return p_394528_.allocate();
   }

   @Override
   public <T> void release(ResourceDescriptor<T> p_360929_, T p_364526_) {
      this.pool.addFirst(new CrossFrameResourcePool.ResourceEntry<>(p_360929_, p_364526_, this.framesToKeepResource));
   }

   public void clear() {
      this.pool.forEach(CrossFrameResourcePool.ResourceEntry::close);
      this.pool.clear();
   }

   @Override
   public void close() {
      this.clear();
   }

   @VisibleForTesting
   protected Collection<CrossFrameResourcePool.ResourceEntry<?>> entries() {
      return this.pool;
   }

   @OnlyIn(Dist.CLIENT)
   @VisibleForTesting
   protected static final class ResourceEntry<T> implements AutoCloseable {
      final ResourceDescriptor<T> descriptor;
      final T value;
      int framesToLive;

      ResourceEntry(ResourceDescriptor<T> p_362044_, T p_364798_, int p_362012_) {
         this.descriptor = p_362044_;
         this.value = p_364798_;
         this.framesToLive = p_362012_;
      }

      @Override
      public void close() {
         this.descriptor.free(this.value);
      }
   }
}
