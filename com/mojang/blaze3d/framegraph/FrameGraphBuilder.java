package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FrameGraphBuilder {
   private final List<FrameGraphBuilder.InternalVirtualResource<?>> internalResources = new ArrayList<>();
   private final List<FrameGraphBuilder.ExternalResource<?>> externalResources = new ArrayList<>();
   private final List<FrameGraphBuilder.Pass> passes = new ArrayList<>();

   public FramePass addPass(String p_362000_) {
      FrameGraphBuilder.Pass framegraphbuilder$pass = new FrameGraphBuilder.Pass(this.passes.size(), p_362000_);
      this.passes.add(framegraphbuilder$pass);
      return framegraphbuilder$pass;
   }

   public <T> ResourceHandle<T> importExternal(String p_363653_, T p_363574_) {
      FrameGraphBuilder.ExternalResource<T> externalresource = new FrameGraphBuilder.ExternalResource<>(p_363653_, null, p_363574_);
      this.externalResources.add(externalresource);
      return externalresource.handle;
   }

   public <T> ResourceHandle<T> createInternal(String p_364379_, ResourceDescriptor<T> p_360666_) {
      return this.createInternalResource(p_364379_, p_360666_, null).handle;
   }

   <T> FrameGraphBuilder.InternalVirtualResource<T> createInternalResource(String p_362189_, ResourceDescriptor<T> p_361484_, FrameGraphBuilder.Pass p_363038_) {
      int i = this.internalResources.size();
      FrameGraphBuilder.InternalVirtualResource<T> internalvirtualresource = new FrameGraphBuilder.InternalVirtualResource<>(i, p_362189_, p_363038_, p_361484_);
      this.internalResources.add(internalvirtualresource);
      return internalvirtualresource;
   }

   public void execute(GraphicsResourceAllocator p_361151_) {
      this.execute(p_361151_, FrameGraphBuilder.Inspector.NONE);
   }

   public void execute(GraphicsResourceAllocator p_362990_, FrameGraphBuilder.Inspector p_363758_) {
      BitSet bitset = this.identifyPassesToKeep();
      List<FrameGraphBuilder.Pass> list = new ArrayList<>(bitset.cardinality());
      BitSet bitset1 = new BitSet(this.passes.size());

      for (FrameGraphBuilder.Pass framegraphbuilder$pass : this.passes) {
         this.resolvePassOrder(framegraphbuilder$pass, bitset, bitset1, list);
      }

      this.assignResourceLifetimes(list);

      for (FrameGraphBuilder.Pass framegraphbuilder$pass1 : list) {
         for (FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource : framegraphbuilder$pass1.resourcesToAcquire) {
            p_363758_.acquireResource(internalvirtualresource.name);
            internalvirtualresource.acquire(p_362990_);
         }

         p_363758_.beforeExecutePass(framegraphbuilder$pass1.name);
         framegraphbuilder$pass1.task.run();
         p_363758_.afterExecutePass(framegraphbuilder$pass1.name);

         for (int i = framegraphbuilder$pass1.resourcesToRelease.nextSetBit(0); i >= 0; i = framegraphbuilder$pass1.resourcesToRelease.nextSetBit(i + 1)) {
            FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource1 = this.internalResources.get(i);
            p_363758_.releaseResource(internalvirtualresource1.name);
            internalvirtualresource1.release(p_362990_);
         }
      }
   }

   private BitSet identifyPassesToKeep() {
      Deque<FrameGraphBuilder.Pass> deque = new ArrayDeque<>(this.passes.size());
      BitSet bitset = new BitSet(this.passes.size());

      for (FrameGraphBuilder.VirtualResource<?> virtualresource : this.externalResources) {
         FrameGraphBuilder.Pass framegraphbuilder$pass = virtualresource.handle.createdBy;
         if (framegraphbuilder$pass != null) {
            this.discoverAllRequiredPasses(framegraphbuilder$pass, bitset, deque);
         }
      }

      for (FrameGraphBuilder.Pass framegraphbuilder$pass1 : this.passes) {
         if (framegraphbuilder$pass1.disableCulling) {
            this.discoverAllRequiredPasses(framegraphbuilder$pass1, bitset, deque);
         }
      }

      return bitset;
   }

   private void discoverAllRequiredPasses(FrameGraphBuilder.Pass p_362135_, BitSet p_361672_, Deque<FrameGraphBuilder.Pass> p_363875_) {
      p_363875_.add(p_362135_);

      while (!p_363875_.isEmpty()) {
         FrameGraphBuilder.Pass framegraphbuilder$pass = p_363875_.poll();
         if (!p_361672_.get(framegraphbuilder$pass.id)) {
            p_361672_.set(framegraphbuilder$pass.id);

            for (int i = framegraphbuilder$pass.requiredPassIds.nextSetBit(0); i >= 0; i = framegraphbuilder$pass.requiredPassIds.nextSetBit(i + 1)) {
               p_363875_.add(this.passes.get(i));
            }
         }
      }
   }

   private void resolvePassOrder(FrameGraphBuilder.Pass p_361359_, BitSet p_362755_, BitSet p_362866_, List<FrameGraphBuilder.Pass> p_365482_) {
      if (p_362866_.get(p_361359_.id)) {
         String s = p_362866_.stream().mapToObj(p_361748_ -> this.passes.get(p_361748_).name).collect(Collectors.joining(", "));
         throw new IllegalStateException("Frame graph cycle detected between " + s);
      } else {
         if (p_362755_.get(p_361359_.id)) {
            p_362866_.set(p_361359_.id);
            p_362755_.clear(p_361359_.id);

            for (int i = p_361359_.requiredPassIds.nextSetBit(0); i >= 0; i = p_361359_.requiredPassIds.nextSetBit(i + 1)) {
               this.resolvePassOrder(this.passes.get(i), p_362755_, p_362866_, p_365482_);
            }

            for (FrameGraphBuilder.Handle<?> handle : p_361359_.writesFrom) {
               for (int j = handle.readBy.nextSetBit(0); j >= 0; j = handle.readBy.nextSetBit(j + 1)) {
                  if (j != p_361359_.id) {
                     this.resolvePassOrder(this.passes.get(j), p_362755_, p_362866_, p_365482_);
                  }
               }
            }

            p_365482_.add(p_361359_);
            p_362866_.clear(p_361359_.id);
         }
      }
   }

   private void assignResourceLifetimes(Collection<FrameGraphBuilder.Pass> p_365083_) {
      FrameGraphBuilder.Pass[] aframegraphbuilder$pass = new FrameGraphBuilder.Pass[this.internalResources.size()];

      for (FrameGraphBuilder.Pass framegraphbuilder$pass : p_365083_) {
         for (int i = framegraphbuilder$pass.requiredResourceIds.nextSetBit(0); i >= 0; i = framegraphbuilder$pass.requiredResourceIds.nextSetBit(i + 1)) {
            FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource = this.internalResources.get(i);
            FrameGraphBuilder.Pass framegraphbuilder$pass1 = aframegraphbuilder$pass[i];
            aframegraphbuilder$pass[i] = framegraphbuilder$pass;
            if (framegraphbuilder$pass1 == null) {
               framegraphbuilder$pass.resourcesToAcquire.add(internalvirtualresource);
            } else {
               framegraphbuilder$pass1.resourcesToRelease.clear(i);
            }

            framegraphbuilder$pass.resourcesToRelease.set(i);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ExternalResource<T> extends FrameGraphBuilder.VirtualResource<T> {
      private final T resource;

      public ExternalResource(String p_362541_, FrameGraphBuilder.Pass p_363193_, T p_360991_) {
         super(p_362541_, p_363193_);
         this.resource = p_360991_;
      }

      @Override
      public T get() {
         return this.resource;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Handle<T> implements ResourceHandle<T> {
      final FrameGraphBuilder.VirtualResource<T> holder;
      private final int version;
      final FrameGraphBuilder.Pass createdBy;
      final BitSet readBy = new BitSet();
      @Nullable
      private FrameGraphBuilder.Handle<T> aliasedBy;

      Handle(FrameGraphBuilder.VirtualResource<T> p_361124_, int p_362902_, FrameGraphBuilder.Pass p_361683_) {
         this.holder = p_361124_;
         this.version = p_362902_;
         this.createdBy = p_361683_;
      }

      @Override
      public T get() {
         return this.holder.get();
      }

      FrameGraphBuilder.Handle<T> writeAndAlias(FrameGraphBuilder.Pass p_365456_) {
         if (this.holder.handle != this) {
            throw new IllegalStateException("Handle " + this + " is no longer valid, as its contents were moved into " + this.aliasedBy);
         } else {
            FrameGraphBuilder.Handle<T> handle = new FrameGraphBuilder.Handle<>(this.holder, this.version + 1, p_365456_);
            this.holder.handle = handle;
            this.aliasedBy = handle;
            return handle;
         }
      }

      @Override
      public String toString() {
         return this.createdBy != null ? this.holder + "#" + this.version + " (from " + this.createdBy + ")" : this.holder + "#" + this.version;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Inspector {
      FrameGraphBuilder.Inspector NONE = new FrameGraphBuilder.Inspector() {};

      default void acquireResource(String p_361310_) {
      }

      default void releaseResource(String p_361496_) {
      }

      default void beforeExecutePass(String p_362930_) {
      }

      default void afterExecutePass(String p_363153_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class InternalVirtualResource<T> extends FrameGraphBuilder.VirtualResource<T> {
      final int id;
      private final ResourceDescriptor<T> descriptor;
      @Nullable
      private T physicalResource;

      public InternalVirtualResource(int p_364397_, String p_361829_, FrameGraphBuilder.Pass p_364005_, ResourceDescriptor<T> p_364356_) {
         super(p_361829_, p_364005_);
         this.id = p_364397_;
         this.descriptor = p_364356_;
      }

      @Override
      public T get() {
         return Objects.requireNonNull(this.physicalResource, "Resource is not currently available");
      }

      public void acquire(GraphicsResourceAllocator p_363728_) {
         if (this.physicalResource != null) {
            throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
         } else {
            this.physicalResource = p_363728_.acquire(this.descriptor);
         }
      }

      public void release(GraphicsResourceAllocator p_362252_) {
         if (this.physicalResource == null) {
            throw new IllegalStateException("Tried to release physical resource that was not allocated");
         } else {
            p_362252_.release(this.descriptor, this.physicalResource);
            this.physicalResource = null;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Pass implements FramePass {
      final int id;
      final String name;
      final List<FrameGraphBuilder.Handle<?>> writesFrom = new ArrayList<>();
      final BitSet requiredResourceIds = new BitSet();
      final BitSet requiredPassIds = new BitSet();
      Runnable task = () -> {};
      final List<FrameGraphBuilder.InternalVirtualResource<?>> resourcesToAcquire = new ArrayList<>();
      final BitSet resourcesToRelease = new BitSet();
      boolean disableCulling;

      public Pass(int p_363679_, String p_362279_) {
         this.id = p_363679_;
         this.name = p_362279_;
      }

      private <T> void markResourceRequired(FrameGraphBuilder.Handle<T> p_362265_) {
         if (p_362265_.holder instanceof FrameGraphBuilder.InternalVirtualResource<?> internalvirtualresource) {
            this.requiredResourceIds.set(internalvirtualresource.id);
         }
      }

      private void markPassRequired(FrameGraphBuilder.Pass p_361707_) {
         this.requiredPassIds.set(p_361707_.id);
      }

      @Override
      public <T> ResourceHandle<T> createsInternal(String p_363278_, ResourceDescriptor<T> p_363531_) {
         FrameGraphBuilder.InternalVirtualResource<T> internalvirtualresource = FrameGraphBuilder.this.createInternalResource(p_363278_, p_363531_, this);
         this.requiredResourceIds.set(internalvirtualresource.id);
         return internalvirtualresource.handle;
      }

      @Override
      public <T> void reads(ResourceHandle<T> p_363844_) {
         this._reads((FrameGraphBuilder.Handle<T>)p_363844_);
      }

      private <T> void _reads(FrameGraphBuilder.Handle<T> p_360431_) {
         this.markResourceRequired(p_360431_);
         if (p_360431_.createdBy != null) {
            this.markPassRequired(p_360431_.createdBy);
         }

         p_360431_.readBy.set(this.id);
      }

      @Override
      public <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> p_363371_) {
         return this._readsAndWrites((FrameGraphBuilder.Handle<T>)p_363371_);
      }

      @Override
      public void requires(FramePass p_365196_) {
         this.requiredPassIds.set(((FrameGraphBuilder.Pass)p_365196_).id);
      }

      @Override
      public void disableCulling() {
         this.disableCulling = true;
      }

      private <T> FrameGraphBuilder.Handle<T> _readsAndWrites(FrameGraphBuilder.Handle<T> p_364070_) {
         this.writesFrom.add(p_364070_);
         this._reads(p_364070_);
         return p_364070_.writeAndAlias(this);
      }

      @Override
      public void executes(Runnable p_360375_) {
         this.task = p_360375_;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract static class VirtualResource<T> {
      public final String name;
      public FrameGraphBuilder.Handle<T> handle;

      public VirtualResource(String p_361601_, FrameGraphBuilder.Pass p_361693_) {
         this.name = p_361601_;
         this.handle = new FrameGraphBuilder.Handle<>(this, 0, p_361693_);
      }

      public abstract T get();

      @Override
      public String toString() {
         return this.name;
      }
   }
}
