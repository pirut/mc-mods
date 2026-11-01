package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionOcclusionGraph {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
   private static final int MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE = SectionPos.blockToSectionCoord(60);
   private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
   private boolean needsFullUpdate = true;
   @Nullable
   private Future<?> fullUpdateTask;
   @Nullable
   private ViewArea viewArea;
   private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
   private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
   private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);

   public void waitAndReset(@Nullable ViewArea p_294431_) {
      if (this.fullUpdateTask != null) {
         try {
            this.fullUpdateTask.get();
            this.fullUpdateTask = null;
         } catch (Exception var3) {
            LOGGER.warn("Full update failed", var3);
         }
      }

      this.viewArea = p_294431_;
      if (p_294431_ != null) {
         this.currentGraph.set(new SectionOcclusionGraph.GraphState(p_294431_));
         this.invalidate();
      } else {
         this.currentGraph.set(null);
      }
   }

   public void invalidate() {
      this.needsFullUpdate = true;
   }

   public void addSectionsInFrustum(
      Frustum p_294180_, List<SectionRenderDispatcher.RenderSection> p_296160_, List<SectionRenderDispatcher.RenderSection> p_371751_
   ) {
      this.currentGraph.get().storage().sectionTree.visitNodes((p_370300_, p_370301_, p_370302_, p_370303_) -> {
         SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = p_370300_.getSection();
         if (sectionrenderdispatcher$rendersection != null) {
            p_296160_.add(sectionrenderdispatcher$rendersection);
            if (p_370303_) {
               p_371751_.add(sectionrenderdispatcher$rendersection);
            }
         }
      }, p_294180_, 32);
   }

   public boolean consumeFrustumUpdate() {
      return this.needsFrustumUpdate.compareAndSet(true, false);
   }

   public void onChunkReadyToRender(ChunkPos p_294122_) {
      SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
      if (sectionocclusiongraph$graphevents != null) {
         this.addNeighbors(sectionocclusiongraph$graphevents, p_294122_);
      }

      SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
      if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
         this.addNeighbors(sectionocclusiongraph$graphevents1, p_294122_);
      }
   }

   public void schedulePropagationFrom(SectionRenderDispatcher.RenderSection p_295414_) {
      SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
      if (sectionocclusiongraph$graphevents != null) {
         sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(p_295414_);
      }

      SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
      if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
         sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(p_295414_);
      }
   }

   public void update(boolean p_294298_, Camera p_294529_, Frustum p_294426_, List<SectionRenderDispatcher.RenderSection> p_295280_, LongOpenHashSet p_366410_) {
      Vec3 vec3 = p_294529_.position();
      if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
         this.scheduleFullUpdate(p_294298_, p_294529_, vec3, p_366410_);
      }

      this.runPartialUpdate(p_294298_, p_294426_, p_295280_, vec3, p_366410_);
   }

   private void scheduleFullUpdate(boolean p_294514_, Camera p_295663_, Vec3 p_295096_, LongOpenHashSet p_366772_) {
      this.needsFullUpdate = false;
      LongOpenHashSet longopenhashset = p_366772_.clone();
      this.fullUpdateTask = CompletableFuture.runAsync(() -> {
         SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea);
         this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
         Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
         this.initializeQueueForFullUpdate(p_295663_, queue);
         queue.forEach(p_295724_ -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(p_295724_.section, p_295724_));
         this.runUpdates(sectionocclusiongraph$graphstate.storage, p_295096_, queue, p_294514_, p_294678_ -> {}, longopenhashset);
         this.currentGraph.set(sectionocclusiongraph$graphstate);
         this.nextGraphEvents.set(null);
         this.needsFrustumUpdate.set(true);
      }, Util.backgroundExecutor());
   }

   private void runPartialUpdate(
      boolean p_294795_, Frustum p_294341_, List<SectionRenderDispatcher.RenderSection> p_294796_, Vec3 p_295915_, LongOpenHashSet p_366753_
   ) {
      SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
      this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);
      if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
         Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

         while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events
               .sectionsToPropagateFrom
               .poll();
            SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
               .sectionToNodeMap
               .get(sectionrenderdispatcher$rendersection);
            if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection) {
               queue.add(sectionocclusiongraph$node);
            }
         }

         Frustum frustum = LevelRenderer.offsetFrustum(p_294341_);
         Consumer<SectionRenderDispatcher.RenderSection> consumer = p_370305_ -> {
            if (frustum.isVisible(p_370305_.getBoundingBox())) {
               this.needsFrustumUpdate.set(true);
            }
         };
         this.runUpdates(sectionocclusiongraph$graphstate.storage, p_295915_, queue, p_294795_, consumer, p_366753_);
      }
   }

   private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState p_296471_) {
      LongIterator longiterator = p_296471_.events.chunksWhichReceivedNeighbors.iterator();

      while (longiterator.hasNext()) {
         long i = longiterator.nextLong();
         List<SectionRenderDispatcher.RenderSection> list = (List<SectionRenderDispatcher.RenderSection>)p_296471_.storage.chunksWaitingForNeighbors.get(i);
         if (list != null && list.get(0).hasAllNeighbors()) {
            p_296471_.events.sectionsToPropagateFrom.addAll(list);
            p_296471_.storage.chunksWaitingForNeighbors.remove(i);
         }
      }

      p_296471_.events.chunksWhichReceivedNeighbors.clear();
   }

   private void addNeighbors(SectionOcclusionGraph.GraphEvents p_295866_, ChunkPos p_295968_) {
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x - 1, p_295968_.z));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x, p_295968_.z - 1));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x + 1, p_295968_.z));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x, p_295968_.z + 1));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x - 1, p_295968_.z - 1));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x - 1, p_295968_.z + 1));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x + 1, p_295968_.z - 1));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x + 1, p_295968_.z + 1));
   }

   private void initializeQueueForFullUpdate(Camera p_295148_, Queue<SectionOcclusionGraph.Node> p_294801_) {
      BlockPos blockpos = p_295148_.blockPosition();
      long i = SectionPos.asLong(blockpos);
      int j = SectionPos.y(i);
      SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(i);
      if (sectionrenderdispatcher$rendersection == null) {
         LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
         boolean flag = j < levelheightaccessor.getMinSectionY();
         int k = flag ? levelheightaccessor.getMinSectionY() : levelheightaccessor.getMaxSectionY();
         int l = this.viewArea.getViewDistance();
         List<SectionOcclusionGraph.Node> list = Lists.newArrayList();
         int i1 = SectionPos.x(i);
         int j1 = SectionPos.z(i);

         for (int k1 = -l; k1 <= l; k1++) {
            for (int l1 = -l; l1 <= l; l1++) {
               SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                  .getRenderSection(SectionPos.asLong(k1 + i1, k, l1 + j1));
               if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                  Direction direction = flag ? Direction.UP : Direction.DOWN;
                  SectionOcclusionGraph.Node sectionocclusiongraph$node = new SectionOcclusionGraph.Node(sectionrenderdispatcher$rendersection1, direction, 0);
                  sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);
                  if (k1 > 0) {
                     sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                  } else if (k1 < 0) {
                     sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                  }

                  if (l1 > 0) {
                     sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                  } else if (l1 < 0) {
                     sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                  }

                  list.add(sectionocclusiongraph$node);
               }
            }
         }

         list.sort(Comparator.comparingDouble(p_392553_ -> blockpos.distSqr(SectionPos.of(p_392553_.section.getSectionNode()).center())));
         p_294801_.addAll(list);
      } else {
         p_294801_.add(new SectionOcclusionGraph.Node(sectionrenderdispatcher$rendersection, null, 0));
      }
   }

   private void runUpdates(
      SectionOcclusionGraph.GraphStorage p_295507_,
      Vec3 p_294343_,
      Queue<SectionOcclusionGraph.Node> p_295598_,
      boolean p_295668_,
      Consumer<SectionRenderDispatcher.RenderSection> p_295393_,
      LongOpenHashSet p_366468_
   ) {
      SectionPos sectionpos = SectionPos.of(p_294343_);
      long i = sectionpos.asLong();
      BlockPos blockpos = sectionpos.center();

      while (!p_295598_.isEmpty()) {
         SectionOcclusionGraph.Node sectionocclusiongraph$node = p_295598_.poll();
         SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;
         if (!p_366468_.contains(sectionocclusiongraph$node.section.getSectionNode())) {
            if (p_295507_.sectionTree.add(sectionocclusiongraph$node.section)) {
               p_295393_.accept(sectionocclusiongraph$node.section);
            }
         } else {
            sectionocclusiongraph$node.section.sectionMesh.compareAndSet(CompiledSectionMesh.UNCOMPILED, CompiledSectionMesh.EMPTY);
         }

         long j = sectionrenderdispatcher$rendersection.getSectionNode();
         boolean flag = Math.abs(SectionPos.x(j) - sectionpos.x()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
            || Math.abs(SectionPos.y(j) - sectionpos.y()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
            || Math.abs(SectionPos.z(j) - sectionpos.z()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE;

         for (Direction direction : DIRECTIONS) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
               i, sectionrenderdispatcher$rendersection, direction
            );
            if (sectionrenderdispatcher$rendersection1 != null && (!p_295668_ || !sectionocclusiongraph$node.hasDirection(direction.getOpposite()))) {
               if (p_295668_ && sectionocclusiongraph$node.hasSourceDirections()) {
                  SectionMesh sectionmesh = sectionrenderdispatcher$rendersection.getSectionMesh();
                  boolean flag1 = false;

                  for (int k = 0; k < DIRECTIONS.length; k++) {
                     if (sectionocclusiongraph$node.hasSourceDirection(k) && sectionmesh.facesCanSeeEachother(DIRECTIONS[k].getOpposite(), direction)) {
                        flag1 = true;
                        break;
                     }
                  }

                  if (!flag1) {
                     continue;
                  }
               }

               if (p_295668_ && flag) {
                  int l = SectionPos.sectionToBlockCoord(SectionPos.x(j));
                  int i1 = SectionPos.sectionToBlockCoord(SectionPos.y(j));
                  int j1 = SectionPos.sectionToBlockCoord(SectionPos.z(j));
                  boolean flag2 = direction.getAxis() == Direction.Axis.X ? blockpos.getX() > l : blockpos.getX() < l;
                  boolean flag3 = direction.getAxis() == Direction.Axis.Y ? blockpos.getY() > i1 : blockpos.getY() < i1;
                  boolean flag4 = direction.getAxis() == Direction.Axis.Z ? blockpos.getZ() > j1 : blockpos.getZ() < j1;
                  Vector3d vector3d = new Vector3d(l + (flag2 ? 16 : 0), i1 + (flag3 ? 16 : 0), j1 + (flag4 ? 16 : 0));
                  Vector3d vector3d1 = new Vector3d(p_294343_.x, p_294343_.y, p_294343_.z).sub(vector3d).normalize().mul(CEILED_SECTION_DIAGONAL);
                  boolean flag5 = true;

                  while (vector3d.distanceSquared(p_294343_.x, p_294343_.y, p_294343_.z) > 3600.0) {
                     vector3d.add(vector3d1);
                     LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
                     if (vector3d.y > levelheightaccessor.getMaxY() || vector3d.y < levelheightaccessor.getMinY()) {
                        break;
                     }

                     SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                        .getRenderSectionAt(BlockPos.containing(vector3d.x, vector3d.y, vector3d.z));
                     if (sectionrenderdispatcher$rendersection2 == null || p_295507_.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null) {
                        flag5 = false;
                        break;
                     }
                  }

                  if (!flag5) {
                     continue;
                  }
               }

               SectionOcclusionGraph.Node sectionocclusiongraph$node1 = p_295507_.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);
               if (sectionocclusiongraph$node1 != null) {
                  sectionocclusiongraph$node1.addSourceDirection(direction);
               } else {
                  SectionOcclusionGraph.Node sectionocclusiongraph$node2 = new SectionOcclusionGraph.Node(
                     sectionrenderdispatcher$rendersection1, direction, sectionocclusiongraph$node.step + 1
                  );
                  sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);
                  if (sectionrenderdispatcher$rendersection1.hasAllNeighbors()) {
                     p_295598_.add(sectionocclusiongraph$node2);
                     p_295507_.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                  } else if (this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                     p_295507_.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                     long k1 = SectionPos.sectionToChunk(sectionrenderdispatcher$rendersection1.getSectionNode());
                     ((List)p_295507_.chunksWaitingForNeighbors.computeIfAbsent(k1, p_294377_ -> new ArrayList())).add(sectionrenderdispatcher$rendersection1);
                  }
               }
            }
         }
      }
   }

   private boolean isInViewDistance(long p_366544_, long p_366485_) {
      return ChunkTrackingView.isInViewDistance(
         SectionPos.x(p_366544_), SectionPos.z(p_366544_), this.viewArea.getViewDistance(), SectionPos.x(p_366485_), SectionPos.z(p_366485_)
      );
   }

   private SectionRenderDispatcher.RenderSection getRelativeFrom(long p_366669_, SectionRenderDispatcher.RenderSection p_295211_, Direction p_294491_) {
      long i = p_295211_.getNeighborSectionNode(p_294491_);
      if (!this.isInViewDistance(p_366669_, i)) {
         return null;
      } else {
         return Mth.abs(SectionPos.y(p_366669_) - SectionPos.y(i)) > this.viewArea.getViewDistance() ? null : this.viewArea.getRenderSection(i);
      }
   }

   @VisibleForDebug
   @Nullable
   public SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection p_296364_) {
      return this.currentGraph.get().storage.sectionToNodeMap.get(p_296364_);
   }

   public Octree getOctree() {
      return this.currentGraph.get().storage.sectionTree;
   }

   @OnlyIn(Dist.CLIENT)
   record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
      GraphEvents() {
         this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
      GraphState(ViewArea p_366764_) {
         this(new SectionOcclusionGraph.GraphStorage(p_366764_), new SectionOcclusionGraph.GraphEvents());
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class GraphStorage {
      public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
      public final Octree sectionTree;
      public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;

      public GraphStorage(ViewArea p_366828_) {
         this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(p_366828_.sections.length);
         this.sectionTree = new Octree(p_366828_.getCameraSectionPos(), p_366828_.getViewDistance(), p_366828_.sectionGridSizeY, p_366828_.level.getMinY());
         this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap();
      }
   }

   @OnlyIn(Dist.CLIENT)
   @VisibleForDebug
   public static class Node {
      @VisibleForDebug
      protected final SectionRenderDispatcher.RenderSection section;
      private byte sourceDirections;
      byte directions;
      @VisibleForDebug
      public final int step;

      Node(SectionRenderDispatcher.RenderSection p_295110_, @Nullable Direction p_295920_, int p_295951_) {
         this.section = p_295110_;
         if (p_295920_ != null) {
            this.addSourceDirection(p_295920_);
         }

         this.step = p_295951_;
      }

      void setDirections(byte p_295029_, Direction p_296033_) {
         this.directions = (byte)(this.directions | p_295029_ | 1 << p_296033_.ordinal());
      }

      boolean hasDirection(Direction p_294996_) {
         return (this.directions & 1 << p_294996_.ordinal()) > 0;
      }

      void addSourceDirection(Direction p_295444_) {
         this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << p_295444_.ordinal());
      }

      @VisibleForDebug
      public boolean hasSourceDirection(int p_294302_) {
         return (this.sourceDirections & 1 << p_294302_) > 0;
      }

      boolean hasSourceDirections() {
         return this.sourceDirections != 0;
      }

      @Override
      public int hashCode() {
         return Long.hashCode(this.section.getSectionNode());
      }

      @Override
      public boolean equals(Object p_295498_) {
         return p_295498_ instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node
            ? this.section.getSectionNode() == sectionocclusiongraph$node.section.getSectionNode()
            : false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class SectionToNodeMap {
      private final SectionOcclusionGraph.Node[] nodes;

      SectionToNodeMap(int p_296136_) {
         this.nodes = new SectionOcclusionGraph.Node[p_296136_];
      }

      public void put(SectionRenderDispatcher.RenderSection p_295644_, SectionOcclusionGraph.Node p_295953_) {
         this.nodes[p_295644_.index] = p_295953_;
      }

      @Nullable
      public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection p_295721_) {
         int i = p_295721_.index;
         return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
      }
   }
}
