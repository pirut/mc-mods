package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.IRenderableSection;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
   private final CompileTaskDynamicQueue compileQueue = new CompileTaskDynamicQueue();
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   final Executor mainThreadUploadExecutor = this.toUpload::add;
   final Queue<SectionMesh> toClose = Queues.newConcurrentLinkedQueue();
   final SectionBufferBuilderPack fixedBuffers;
   private final SectionBufferBuilderPool bufferPool;
   volatile boolean closed;
   private final ConsecutiveExecutor consecutiveExecutor;
   private final TracingExecutor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   Vec3 cameraPosition = Vec3.ZERO;
   final SectionCompiler sectionCompiler;

   public SectionRenderDispatcher(
      ClientLevel p_295274_,
      LevelRenderer p_295323_,
      TracingExecutor p_373128_,
      RenderBuffers p_307511_,
      BlockRenderDispatcher p_350514_,
      BlockEntityRenderDispatcher p_350550_
   ) {
      this.level = p_295274_;
      this.renderer = p_295323_;
      this.fixedBuffers = p_307511_.fixedBufferPack();
      this.bufferPool = p_307511_.sectionBufferPool();
      this.executor = p_373128_;
      this.consecutiveExecutor = new ConsecutiveExecutor(p_373128_, "Section Renderer");
      this.consecutiveExecutor.schedule(this::runTask);
      this.sectionCompiler = new SectionCompiler(p_350514_, p_350550_);
   }

   public void setLevel(ClientLevel p_295112_) {
      this.level = p_295112_;
   }

   private void runTask() {
      if (!this.closed && !this.bufferPool.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.compileQueue.poll(this.cameraPosition);
         if (sectionrenderdispatcher$rendersection$compiletask != null) {
            SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
            CompletableFuture.<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>>supplyAsync(
                  () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack),
                  this.executor.forName(sectionrenderdispatcher$rendersection$compiletask.name())
               )
               .thenCompose(p_296185_ -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)p_296185_)
               .whenComplete((p_370310_, p_370311_) -> {
                  if (p_370311_ != null) {
                     Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_370311_, "Batching sections"));
                  } else {
                     sectionrenderdispatcher$rendersection$compiletask.isCompleted.set(true);
                     this.consecutiveExecutor.schedule(() -> {
                        if (p_370310_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                           sectionbufferbuilderpack.clearAll();
                        } else {
                           sectionbufferbuilderpack.discardAll();
                        }

                        this.bufferPool.release(sectionbufferbuilderpack);
                        this.runTask();
                     });
                  }
               });
         }
      }
   }

   public void setCameraPosition(Vec3 p_427437_) {
      this.cameraPosition = p_427437_;
   }

   public void uploadAllPendingUploads() {
      Runnable runnable;
      while ((runnable = this.toUpload.poll()) != null) {
         runnable.run();
      }

      SectionMesh sectionmesh;
      while ((sectionmesh = this.toClose.poll()) != null) {
         sectionmesh.close();
      }
   }

   public void rebuildSectionSync(SectionRenderDispatcher.RenderSection p_296309_, RenderRegionCache p_294139_) {
      p_296309_.compileSync(p_294139_);
   }

   public void schedule(SectionRenderDispatcher.RenderSection.CompileTask p_295825_) {
      if (!this.closed) {
         this.consecutiveExecutor.schedule(() -> {
            if (!this.closed) {
               this.compileQueue.add(p_295825_);
               this.runTask();
            }
         });
      }
   }

   public void clearCompileQueue() {
      this.compileQueue.clear();
   }

   public boolean isQueueEmpty() {
      return this.compileQueue.size() == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.closed = true;
      this.clearCompileQueue();
      this.uploadAllPendingUploads();
   }

   @VisibleForDebug
   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.compileQueue.size(), this.toUpload.size(), this.bufferPool.getFreeBufferCount());
   }

   @VisibleForDebug
   public int getCompileQueueSize() {
      return this.compileQueue.size();
   }

   @VisibleForDebug
   public int getToUpload() {
      return this.toUpload.size();
   }

   @VisibleForDebug
   public int getFreeBufferCount() {
      return this.bufferPool.getFreeBufferCount();
   }

   @OnlyIn(Dist.CLIENT)
   public class RenderSection implements IRenderableSection {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<SectionMesh> sectionMesh = new AtomicReference<>(CompiledSectionMesh.UNCOMPILED);
      private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
      private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
      private AABB bb;
      private boolean dirty = true;
      volatile long sectionNode = SectionPos.asLong(-1, -1, -1);
      final BlockPos.MutableBlockPos renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private boolean playerChanged;
      private long uploadedTime;
      private long fadeDuration;
      private boolean wasPreviouslyEmpty;

      public RenderSection(int p_295197_, long p_366428_) {
         this.index = p_295197_;
         this.setSectionNode(p_366428_);
      }

      public float getVisibility(long p_460764_) {
         long i = p_460764_ - this.uploadedTime;
         return i >= this.fadeDuration ? 1.0F : (float)i / (float)this.fadeDuration;
      }

      public void setFadeDuration(long p_469503_) {
         this.fadeDuration = p_469503_;
      }

      public void setWasPreviouslyEmpty(boolean p_469627_) {
         this.wasPreviouslyEmpty = p_469627_;
      }

      public boolean wasPreviouslyEmpty() {
         return this.wasPreviouslyEmpty;
      }

      private boolean doesChunkExistAt(long p_366835_) {
         ChunkAccess chunkaccess = SectionRenderDispatcher.this.level.getChunk(SectionPos.x(p_366835_), SectionPos.z(p_366835_), ChunkStatus.FULL, false);
         return chunkaccess != null && SectionRenderDispatcher.this.level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(p_366835_));
      }

      public boolean hasAllNeighbors() {
         return this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.WEST))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.NORTH))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.EAST))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.SOUTH))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, -1))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, 1))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, -1))
            && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, 1));
      }

      @Override
      public AABB getBoundingBox() {
         return this.bb;
      }

      public CompletableFuture<Void> upload(Map<ChunkSectionLayer, MeshData> p_427391_, CompiledSectionMesh p_427357_) {
         if (SectionRenderDispatcher.this.closed) {
            p_427391_.values().forEach(MeshData::close);
            return CompletableFuture.completedFuture(null);
         } else {
            return CompletableFuture.runAsync(() -> p_427391_.forEach((p_465626_, p_465627_) -> {
               try (Zone zone = Profiler.get().zone("Upload Section Layer")) {
                  p_427357_.uploadMeshLayer(p_465626_, p_465627_, this.sectionNode);
                  p_465627_.close();
               }

               if (this.uploadedTime == 0L) {
                  this.uploadedTime = Util.getMillis();
               }
            }), SectionRenderDispatcher.this.mainThreadUploadExecutor);
         }
      }

      public CompletableFuture<Void> uploadSectionIndexBuffer(CompiledSectionMesh p_427234_, ByteBufferBuilder.Result p_410127_, ChunkSectionLayer p_427358_) {
         if (SectionRenderDispatcher.this.closed) {
            p_410127_.close();
            return CompletableFuture.completedFuture(null);
         } else {
            return CompletableFuture.runAsync(() -> {
               try (Zone zone = Profiler.get().zone("Upload Section Indices")) {
                  p_427234_.uploadLayerIndexBuffer(p_427358_, p_410127_, this.sectionNode);
                  p_410127_.close();
               }
            }, SectionRenderDispatcher.this.mainThreadUploadExecutor);
         }
      }

      public void setSectionNode(long p_366482_) {
         this.reset();
         this.sectionNode = p_366482_;
         int i = SectionPos.sectionToBlockCoord(SectionPos.x(p_366482_));
         int j = SectionPos.sectionToBlockCoord(SectionPos.y(p_366482_));
         int k = SectionPos.sectionToBlockCoord(SectionPos.z(p_366482_));
         this.renderOrigin.set(i, j, k);
         this.bb = new AABB(i, j, k, i + 16, j + 16, k + 16);
      }

      public SectionMesh getSectionMesh() {
         return this.sectionMesh.get();
      }

      public void reset() {
         this.cancelTasks();
         this.sectionMesh.getAndSet(CompiledSectionMesh.UNCOMPILED).close();
         this.dirty = true;
         this.uploadedTime = 0L;
         this.wasPreviouslyEmpty = false;
      }

      @Override
      public BlockPos getRenderOrigin() {
         return this.renderOrigin;
      }

      public long getSectionNode() {
         return this.sectionNode;
      }

      public void setDirty(boolean p_295417_) {
         boolean flag = this.dirty;
         this.dirty = true;
         this.playerChanged = p_295417_ | (flag && this.playerChanged);
      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public long getNeighborSectionNode(Direction p_366736_) {
         return SectionPos.offset(this.sectionNode, p_366736_);
      }

      public void resortTransparency(SectionRenderDispatcher p_294363_) {
         if (this.getSectionMesh() instanceof CompiledSectionMesh compiledsectionmesh) {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(compiledsectionmesh);
            p_294363_.schedule(this.lastResortTransparencyTask);
         }
      }

      public boolean hasTranslucentGeometry() {
         return this.getSectionMesh().hasTranslucentGeometry();
      }

      public boolean transparencyResortingScheduled() {
         return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
      }

      protected void cancelTasks() {
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }
      }

      public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache p_295324_) {
         this.cancelTasks();
         List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers = ClientHooks.gatherAdditionalRenderers(
            this.renderOrigin, SectionRenderDispatcher.this.level
         );
         RenderSectionRegion rendersectionregion = p_295324_.createRegion(SectionRenderDispatcher.this.level, this.sectionNode);
         boolean flag = this.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED;
         this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(rendersectionregion, flag, additionalRenderers);
         return this.lastRebuildTask;
      }

      public void rebuildSectionAsync(RenderRegionCache p_294660_) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_294660_);
         SectionRenderDispatcher.this.schedule(sectionrenderdispatcher$rendersection$compiletask);
      }

      public void compileSync(RenderRegionCache p_296018_) {
         SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_296018_);
         sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      void setSectionMesh(SectionMesh p_427364_) {
         SectionMesh sectionmesh = this.sectionMesh.getAndSet(p_427364_);
         SectionRenderDispatcher.this.toClose.add(sectionmesh);
         SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
      }

      VertexSorting createVertexSorting(SectionPos p_393680_) {
         Vec3 vec3 = SectionRenderDispatcher.this.cameraPosition;
         return VertexSorting.byDistance(
            (float)(vec3.x - p_393680_.minBlockX()), (float)(vec3.y - p_393680_.minBlockY()), (float)(vec3.z - p_393680_.minBlockZ())
         );
      }

      @Override
      public boolean isEmpty() {
         return !this.getSectionMesh().hasRenderableLayers();
      }

      @OnlyIn(Dist.CLIENT)
      public abstract class CompileTask {
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
         protected final boolean isRecompile;

         public CompileTask(boolean p_295051_) {
            this.isRecompile = p_295051_;
         }

         public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack var1);

         public abstract void cancel();

         protected abstract String name();

         public boolean isRecompile() {
            return this.isRecompile;
         }

         public BlockPos getRenderOrigin() {
            return RenderSection.this.renderOrigin;
         }
      }

      @OnlyIn(Dist.CLIENT)
      class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         protected final RenderSectionRegion region;
         private final List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers;

         @Deprecated
         public RebuildTask(RenderSectionRegion p_427384_, boolean p_295207_) {
            this(p_427384_, p_295207_, List.of());
         }

         public RebuildTask(RenderSectionRegion p_427384_, boolean p_295207_, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
            super(p_295207_);
            this.region = p_427384_;
            this.additionalRenderers = additionalRenderers;
         }

         @Override
         protected String name() {
            return "rend_chk_rebuild";
         }

         @Override
         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_296138_) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               long i = RenderSection.this.sectionNode;
               SectionPos sectionpos = SectionPos.of(i);
               if (this.isCancelled.get()) {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               } else {
                  SectionCompiler.Results sectioncompiler$results;
                  try (Zone zone = Profiler.get().zone("Compile Section")) {
                     sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                        .compile(sectionpos, this.region, RenderSection.this.createVertexSorting(sectionpos), p_296138_, this.additionalRenderers);
                  }

                  TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
                  if (this.isCancelled.get()) {
                     sectioncompiler$results.release();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     CompiledSectionMesh compiledsectionmesh = new CompiledSectionMesh(translucencypointofview, sectioncompiler$results);
                     CompletableFuture<Void> completablefuture = RenderSection.this.upload(sectioncompiler$results.renderedLayers, compiledsectionmesh);
                     return completablefuture.handle((p_428089_, p_428090_) -> {
                        if (p_428090_ != null && !(p_428090_ instanceof CancellationException) && !(p_428090_ instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_428090_, "Rendering section"));
                        }

                        if (!this.isCancelled.get() && !SectionRenderDispatcher.this.closed) {
                           RenderSection.this.setSectionMesh(compiledsectionmesh);
                           return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                        } else {
                           SectionRenderDispatcher.this.toClose.add(compiledsectionmesh);
                           return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                        }
                     });
                  }
               }
            }
         }

         @Override
         public void cancel() {
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderSection.this.setDirty(false);
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         private final CompiledSectionMesh compiledSectionMesh;

         public ResortTransparencyTask(CompiledSectionMesh p_427414_) {
            super(true);
            this.compiledSectionMesh = p_427414_;
         }

         @Override
         protected String name() {
            return "rend_chk_sort";
         }

         @Override
         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_295160_) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               MeshData.SortState meshdata$sortstate = this.compiledSectionMesh.getTransparencyState();
               if (meshdata$sortstate != null && !this.compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {
                  long i = RenderSection.this.sectionNode;
                  VertexSorting vertexsorting = RenderSection.this.createVertexSorting(SectionPos.of(i));
                  TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
                  if (!this.compiledSectionMesh.isDifferentPointOfView(translucencypointofview) && !translucencypointofview.isAxisAligned()) {
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                        p_295160_.buffer(ChunkSectionLayer.TRANSLUCENT), vertexsorting
                     );
                     if (bytebufferbuilder$result == null) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                     } else if (this.isCancelled.get()) {
                        bytebufferbuilder$result.close();
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                     } else {
                        CompletableFuture<Void> completablefuture = RenderSection.this.uploadSectionIndexBuffer(
                           this.compiledSectionMesh, bytebufferbuilder$result, ChunkSectionLayer.TRANSLUCENT
                        );
                        return completablefuture.handle((p_426929_, p_426930_) -> {
                           if (p_426930_ != null && !(p_426930_ instanceof CancellationException) && !(p_426930_ instanceof InterruptedException)) {
                              Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_426930_, "Rendering section"));
                           }

                           if (this.isCancelled.get()) {
                              return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                           } else {
                              this.compiledSectionMesh.setTranslucencyPointOfView(translucencypointofview);
                              return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                           }
                        });
                     }
                  }
               } else {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               }
            }
         }

         @Override
         public void cancel() {
            this.isCancelled.set(true);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum SectionTaskResult {
      SUCCESSFUL,
      CANCELLED;
   }
}
