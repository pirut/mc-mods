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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class FrameGraphBuilder {
   private final List<FrameGraphBuilder.InternalVirtualResource<?>> internalResources = new ArrayList<>();
   private final List<FrameGraphBuilder.ExternalResource<?>> externalResources = new ArrayList<>();
   private final List<FrameGraphBuilder.Pass> passes = new ArrayList<>();

   public FramePass addPass(String name) {
      FrameGraphBuilder.Pass pass = new FrameGraphBuilder.Pass(this.passes.size(), name);
      this.passes.add(pass);
      return pass;
   }

   public <T> ResourceHandle<T> importExternal(String name, T resource) {
      FrameGraphBuilder.ExternalResource<T> externalResource = new FrameGraphBuilder.ExternalResource<>(name, null, resource);
      this.externalResources.add(externalResource);
      return externalResource.handle;
   }

   public <T> ResourceHandle<T> createInternal(String name, ResourceDescriptor<T> descriptor) {
      return this.createInternalResource(name, descriptor, null).handle;
   }

   <T> FrameGraphBuilder.InternalVirtualResource<T> createInternalResource(String name, ResourceDescriptor<T> descriptor, FrameGraphBuilder.Pass createdBy) {
      int i = this.internalResources.size();
      FrameGraphBuilder.InternalVirtualResource<T> internalVirtualResource = new FrameGraphBuilder.InternalVirtualResource<>(i, name, createdBy, descriptor);
      this.internalResources.add(internalVirtualResource);
      return internalVirtualResource;
   }

   public void execute(GraphicsResourceAllocator allocator) {
      this.execute(allocator, FrameGraphBuilder.Inspector.NONE);
   }

   public void execute(GraphicsResourceAllocator allocator, FrameGraphBuilder.Inspector inspector) {
      BitSet bitSet = this.identifyPassesToKeep();
      List<FrameGraphBuilder.Pass> list = new ArrayList<>(bitSet.cardinality());
      BitSet bitSet2 = new BitSet(this.passes.size());

      for (FrameGraphBuilder.Pass pass : this.passes) {
         this.resolvePassOrder(pass, bitSet, bitSet2, list);
      }

      this.assignResourceLifetimes(list);

      for (FrameGraphBuilder.Pass pass : list) {
         for (FrameGraphBuilder.InternalVirtualResource<?> internalVirtualResource : pass.resourcesToAcquire) {
            inspector.acquireResource(internalVirtualResource.name);
            internalVirtualResource.acquire(allocator);
         }

         inspector.beforeExecutePass(pass.name);
         pass.task.run();
         inspector.afterExecutePass(pass.name);

         for (int i = pass.resourcesToRelease.nextSetBit(0); i >= 0; i = pass.resourcesToRelease.nextSetBit(i + 1)) {
            FrameGraphBuilder.InternalVirtualResource<?> internalVirtualResource = this.internalResources.get(i);
            inspector.releaseResource(internalVirtualResource.name);
            internalVirtualResource.release(allocator);
         }
      }
   }

   private BitSet identifyPassesToKeep() {
      Deque<FrameGraphBuilder.Pass> deque = new ArrayDeque<>(this.passes.size());
      BitSet bitSet = new BitSet(this.passes.size());

      for (FrameGraphBuilder.VirtualResource<?> virtualResource : this.externalResources) {
         FrameGraphBuilder.Pass pass = virtualResource.handle.createdBy;
         if (pass != null) {
            this.discoverAllRequiredPasses(pass, bitSet, deque);
         }
      }

      for (FrameGraphBuilder.Pass pass2 : this.passes) {
         if (pass2.disableCulling) {
            this.discoverAllRequiredPasses(pass2, bitSet, deque);
         }
      }

      return bitSet;
   }

   private void discoverAllRequiredPasses(FrameGraphBuilder.Pass pass, BitSet passesToKeep, Deque<FrameGraphBuilder.Pass> output) {
      output.add(pass);

      while (!output.isEmpty()) {
         FrameGraphBuilder.Pass pass2 = output.poll();
         if (!passesToKeep.get(pass2.id)) {
            passesToKeep.set(pass2.id);

            for (int i = pass2.requiredPassIds.nextSetBit(0); i >= 0; i = pass2.requiredPassIds.nextSetBit(i + 1)) {
               output.add(this.passes.get(i));
            }
         }
      }
   }

   private void resolvePassOrder(FrameGraphBuilder.Pass pass, BitSet passesToKeep, BitSet output, List<FrameGraphBuilder.Pass> orderedPasses) {
      if (output.get(pass.id)) {
         String string = output.stream().mapToObj(ix -> this.passes.get(ix).name).collect(Collectors.joining(", "));
         throw new IllegalStateException("Frame graph cycle detected between " + string);
      } else if (passesToKeep.get(pass.id)) {
         output.set(pass.id);
         passesToKeep.clear(pass.id);

         for (int i = pass.requiredPassIds.nextSetBit(0); i >= 0; i = pass.requiredPassIds.nextSetBit(i + 1)) {
            this.resolvePassOrder(this.passes.get(i), passesToKeep, output, orderedPasses);
         }

         for (FrameGraphBuilder.Handle<?> handle : pass.writesFrom) {
            for (int j = handle.readBy.nextSetBit(0); j >= 0; j = handle.readBy.nextSetBit(j + 1)) {
               if (j != pass.id) {
                  this.resolvePassOrder(this.passes.get(j), passesToKeep, output, orderedPasses);
               }
            }
         }

         orderedPasses.add(pass);
         output.clear(pass.id);
      }
   }

   private void assignResourceLifetimes(Collection<FrameGraphBuilder.Pass> passes) {
      FrameGraphBuilder.Pass[] passs = new FrameGraphBuilder.Pass[this.internalResources.size()];

      for (FrameGraphBuilder.Pass pass : passes) {
         for (int i = pass.requiredResourceIds.nextSetBit(0); i >= 0; i = pass.requiredResourceIds.nextSetBit(i + 1)) {
            FrameGraphBuilder.InternalVirtualResource<?> internalVirtualResource = this.internalResources.get(i);
            FrameGraphBuilder.Pass pass2 = passs[i];
            passs[i] = pass;
            if (pass2 == null) {
               pass.resourcesToAcquire.add(internalVirtualResource);
            } else {
               pass2.resourcesToRelease.clear(i);
            }

            pass.resourcesToRelease.set(i);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class ExternalResource<T> extends FrameGraphBuilder.VirtualResource<T> {
      private final T resource;

      public ExternalResource(String name, FrameGraphBuilder.Pass createdBy, T resource) {
         super(name, createdBy);
         this.resource = resource;
      }

      @Override
      public T get() {
         return this.resource;
      }
   }

   @Environment(EnvType.CLIENT)
   static class Handle<T> implements ResourceHandle<T> {
      final FrameGraphBuilder.VirtualResource<T> holder;
      private final int version;
      final FrameGraphBuilder.Pass createdBy;
      final BitSet readBy = new BitSet();
      @Nullable
      private FrameGraphBuilder.Handle<T> aliasedBy;

      Handle(FrameGraphBuilder.VirtualResource<T> holder, int version, FrameGraphBuilder.Pass createdBy) {
         this.holder = holder;
         this.version = version;
         this.createdBy = createdBy;
      }

      @Override
      public T get() {
         return this.holder.get();
      }

      FrameGraphBuilder.Handle<T> writeAndAlias(FrameGraphBuilder.Pass alias) {
         if (this.holder.handle != this) {
            throw new IllegalStateException("Handle " + this + " is no longer valid, as its contents were moved into " + this.aliasedBy);
         } else {
            FrameGraphBuilder.Handle<T> handle = new FrameGraphBuilder.Handle<>(this.holder, this.version + 1, alias);
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

   @Environment(EnvType.CLIENT)
   public interface Inspector {
      FrameGraphBuilder.Inspector NONE = new FrameGraphBuilder.Inspector() {};

      default void acquireResource(String name) {
      }

      default void releaseResource(String name) {
      }

      default void beforeExecutePass(String name) {
      }

      default void afterExecutePass(String name) {
      }
   }

   @Environment(EnvType.CLIENT)
   static class InternalVirtualResource<T> extends FrameGraphBuilder.VirtualResource<T> {
      final int id;
      private final ResourceDescriptor<T> descriptor;
      @Nullable
      private T physicalResource;

      public InternalVirtualResource(int id, String name, FrameGraphBuilder.Pass createdBy, ResourceDescriptor<T> descriptor) {
         super(name, createdBy);
         this.id = id;
         this.descriptor = descriptor;
      }

      @Override
      public T get() {
         return Objects.requireNonNull(this.physicalResource, "Resource is not currently available");
      }

      public void acquire(GraphicsResourceAllocator allocator) {
         if (this.physicalResource != null) {
            throw new IllegalStateException("Tried to acquire physical resource, but it was already assigned");
         } else {
            this.physicalResource = allocator.acquire(this.descriptor);
         }
      }

      public void release(GraphicsResourceAllocator allocator) {
         if (this.physicalResource == null) {
            throw new IllegalStateException("Tried to release physical resource that was not allocated");
         } else {
            allocator.release(this.descriptor, this.physicalResource);
            this.physicalResource = null;
         }
      }
   }

   @Environment(EnvType.CLIENT)
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

      public Pass(final int id, final String name) {
         this.id = id;
         this.name = name;
      }

      private <T> void markResourceRequired(FrameGraphBuilder.Handle<T> handle) {
         if (handle.holder instanceof FrameGraphBuilder.InternalVirtualResource<?> internalVirtualResource) {
            this.requiredResourceIds.set(internalVirtualResource.id);
         }
      }

      private void markPassRequired(FrameGraphBuilder.Pass pass) {
         this.requiredPassIds.set(pass.id);
      }

      @Override
      public <T> ResourceHandle<T> createsInternal(String name, ResourceDescriptor<T> descriptor) {
         FrameGraphBuilder.InternalVirtualResource<T> internalVirtualResource = FrameGraphBuilder.this.createInternalResource(name, descriptor, this);
         this.requiredResourceIds.set(internalVirtualResource.id);
         return internalVirtualResource.handle;
      }

      @Override
      public <T> void reads(ResourceHandle<T> handle) {
         this._reads((FrameGraphBuilder.Handle<T>)handle);
      }

      private <T> void _reads(FrameGraphBuilder.Handle<T> handle) {
         this.markResourceRequired(handle);
         if (handle.createdBy != null) {
            this.markPassRequired(handle.createdBy);
         }

         handle.readBy.set(this.id);
      }

      @Override
      public <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> handle) {
         return this._readsAndWrites((FrameGraphBuilder.Handle<T>)handle);
      }

      @Override
      public void requires(FramePass pass) {
         this.requiredPassIds.set(((FrameGraphBuilder.Pass)pass).id);
      }

      @Override
      public void disableCulling() {
         this.disableCulling = true;
      }

      private <T> FrameGraphBuilder.Handle<T> _readsAndWrites(FrameGraphBuilder.Handle<T> handle) {
         this.writesFrom.add(handle);
         this._reads(handle);
         return handle.writeAndAlias(this);
      }

      @Override
      public void executes(Runnable task) {
         this.task = task;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   @Environment(EnvType.CLIENT)
   abstract static class VirtualResource<T> {
      public final String name;
      public FrameGraphBuilder.Handle<T> handle;

      public VirtualResource(String name, FrameGraphBuilder.Pass createdBy) {
         this.name = name;
         this.handle = new FrameGraphBuilder.Handle<>(this, 0, createdBy);
      }

      public abstract T get();

      @Override
      public String toString() {
         return this.name;
      }
   }
}
