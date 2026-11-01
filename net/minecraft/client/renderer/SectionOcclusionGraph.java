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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionOcclusionGraph {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
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
         this.currentGraph.set(new SectionOcclusionGraph.GraphState(p_294431_.sections.length));
         this.invalidate();
      } else {
         this.currentGraph.set(null);
      }
   }

   public void invalidate() {
      this.needsFullUpdate = true;
   }

   public void addSectionsInFrustum(Frustum p_294180_, List<SectionRenderDispatcher.RenderSection> p_296160_) {
      for (SectionOcclusionGraph.Node $$2 : this.currentGraph.get().storage().renderSections) {
         if (p_294180_.isVisible($$2.section.getBoundingBox())) {
            p_296160_.add($$2.section);
         }
      }
   }

   public boolean consumeFrustumUpdate() {
      return this.needsFrustumUpdate.compareAndSet(true, false);
   }

   public void onChunkLoaded(ChunkPos p_294122_) {
      SectionOcclusionGraph.GraphEvents $$1 = this.nextGraphEvents.get();
      if ($$1 != null) {
         this.addNeighbors($$1, p_294122_);
      }

      SectionOcclusionGraph.GraphEvents $$2 = this.currentGraph.get().events;
      if ($$2 != $$1) {
         this.addNeighbors($$2, p_294122_);
      }
   }

   public void onSectionCompiled(SectionRenderDispatcher.RenderSection p_295414_) {
      SectionOcclusionGraph.GraphEvents $$1 = this.nextGraphEvents.get();
      if ($$1 != null) {
         $$1.sectionsToPropagateFrom.add(p_295414_);
      }

      SectionOcclusionGraph.GraphEvents $$2 = this.currentGraph.get().events;
      if ($$2 != $$1) {
         $$2.sectionsToPropagateFrom.add(p_295414_);
      }
   }

   public void update(boolean p_294298_, Camera p_294529_, Frustum p_294426_, List<SectionRenderDispatcher.RenderSection> p_295280_) {
      Vec3 $$4 = p_294529_.getPosition();
      if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
         this.scheduleFullUpdate(p_294298_, p_294529_, $$4);
      }

      this.runPartialUpdate(p_294298_, p_294426_, p_295280_, $$4);
   }

   private void scheduleFullUpdate(boolean p_294514_, Camera p_295663_, Vec3 p_295096_) {
      this.needsFullUpdate = false;
      this.fullUpdateTask = Util.backgroundExecutor().submit(() -> {
         SectionOcclusionGraph.GraphState $$3 = new SectionOcclusionGraph.GraphState(this.viewArea.sections.length);
         this.nextGraphEvents.set($$3.events);
         Queue<SectionOcclusionGraph.Node> $$4 = Queues.newArrayDeque();
         this.initializeQueueForFullUpdate(p_295663_, $$4);
         $$4.forEach(p_295724_ -> $$3.storage.sectionToNodeMap.put(p_295724_.section, p_295724_));
         this.runUpdates($$3.storage, p_295096_, $$4, p_294514_, p_294678_ -> {});
         this.currentGraph.set($$3);
         this.nextGraphEvents.set(null);
         this.needsFrustumUpdate.set(true);
      });
   }

   private void runPartialUpdate(boolean p_294795_, Frustum p_294341_, List<SectionRenderDispatcher.RenderSection> p_294796_, Vec3 p_295915_) {
      SectionOcclusionGraph.GraphState $$4 = this.currentGraph.get();
      this.queueSectionsWithNewNeighbors($$4);
      if (!$$4.events.sectionsToPropagateFrom.isEmpty()) {
         Queue<SectionOcclusionGraph.Node> $$5 = Queues.newArrayDeque();

         while (!$$4.events.sectionsToPropagateFrom.isEmpty()) {
            SectionRenderDispatcher.RenderSection $$6 = $$4.events.sectionsToPropagateFrom.poll();
            SectionOcclusionGraph.Node $$7 = $$4.storage.sectionToNodeMap.get($$6);
            if ($$7 != null && $$7.section == $$6) {
               $$5.add($$7);
            }
         }

         Frustum $$8 = LevelRenderer.offsetFrustum(p_294341_);
         Consumer<SectionRenderDispatcher.RenderSection> $$9 = p_295778_ -> {
            if ($$8.isVisible(p_295778_.getBoundingBox())) {
               p_294796_.add(p_295778_);
            }
         };
         this.runUpdates($$4.storage, p_295915_, $$5, p_294795_, $$9);
      }
   }

   private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState p_296471_) {
      LongIterator $$1 = p_296471_.events.chunksWhichReceivedNeighbors.iterator();

      while ($$1.hasNext()) {
         long $$2 = $$1.nextLong();
         List<SectionRenderDispatcher.RenderSection> $$3 = (List<SectionRenderDispatcher.RenderSection>)p_296471_.storage.chunksWaitingForNeighbors.get($$2);
         if ($$3 != null && $$3.get(0).hasAllNeighbors()) {
            p_296471_.events.sectionsToPropagateFrom.addAll($$3);
            p_296471_.storage.chunksWaitingForNeighbors.remove($$2);
         }
      }

      p_296471_.events.chunksWhichReceivedNeighbors.clear();
   }

   private void addNeighbors(SectionOcclusionGraph.GraphEvents p_295866_, ChunkPos p_295968_) {
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x - 1, p_295968_.z));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x, p_295968_.z - 1));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x + 1, p_295968_.z));
      p_295866_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_295968_.x, p_295968_.z + 1));
   }

   private void initializeQueueForFullUpdate(Camera p_295148_, Queue<SectionOcclusionGraph.Node> p_294801_) {
      int $$2 = 16;
      Vec3 $$3 = p_295148_.getPosition();
      BlockPos $$4 = p_295148_.getBlockPosition();
      SectionRenderDispatcher.RenderSection $$5 = this.viewArea.getRenderSectionAt($$4);
      if ($$5 == null) {
         LevelHeightAccessor $$6 = this.viewArea.getLevelHeightAccessor();
         boolean $$7 = $$4.getY() > $$6.getMinBuildHeight();
         int $$8 = $$7 ? $$6.getMaxBuildHeight() - 8 : $$6.getMinBuildHeight() + 8;
         int $$9 = Mth.floor($$3.x / 16.0) * 16;
         int $$10 = Mth.floor($$3.z / 16.0) * 16;
         int $$11 = this.viewArea.getViewDistance();
         List<SectionOcclusionGraph.Node> $$12 = Lists.newArrayList();

         for (int $$13 = -$$11; $$13 <= $$11; $$13++) {
            for (int $$14 = -$$11; $$14 <= $$11; $$14++) {
               SectionRenderDispatcher.RenderSection $$15 = this.viewArea
                  .getRenderSectionAt(new BlockPos($$9 + SectionPos.sectionToBlockCoord($$13, 8), $$8, $$10 + SectionPos.sectionToBlockCoord($$14, 8)));
               if ($$15 != null && this.isInViewDistance($$4, $$15.getOrigin())) {
                  Direction $$16 = $$7 ? Direction.DOWN : Direction.UP;
                  SectionOcclusionGraph.Node $$17 = new SectionOcclusionGraph.Node($$15, $$16, 0);
                  $$17.setDirections($$17.directions, $$16);
                  if ($$13 > 0) {
                     $$17.setDirections($$17.directions, Direction.EAST);
                  } else if ($$13 < 0) {
                     $$17.setDirections($$17.directions, Direction.WEST);
                  }

                  if ($$14 > 0) {
                     $$17.setDirections($$17.directions, Direction.SOUTH);
                  } else if ($$14 < 0) {
                     $$17.setDirections($$17.directions, Direction.NORTH);
                  }

                  $$12.add($$17);
               }
            }
         }

         $$12.sort(Comparator.comparingDouble(p_294459_ -> $$4.distSqr(p_294459_.section.getOrigin().offset(8, 8, 8))));
         p_294801_.addAll($$12);
      } else {
         p_294801_.add(new SectionOcclusionGraph.Node($$5, null, 0));
      }
   }

   private void runUpdates(
      SectionOcclusionGraph.GraphStorage p_295507_,
      Vec3 p_294343_,
      Queue<SectionOcclusionGraph.Node> p_295598_,
      boolean p_295668_,
      Consumer<SectionRenderDispatcher.RenderSection> p_295393_
   ) {
      int $$5 = 16;
      BlockPos $$6 = new BlockPos(Mth.floor(p_294343_.x / 16.0) * 16, Mth.floor(p_294343_.y / 16.0) * 16, Mth.floor(p_294343_.z / 16.0) * 16);
      BlockPos $$7 = $$6.offset(8, 8, 8);

      while (!p_295598_.isEmpty()) {
         SectionOcclusionGraph.Node $$8 = p_295598_.poll();
         SectionRenderDispatcher.RenderSection $$9 = $$8.section;
         if (p_295507_.renderSections.add($$8)) {
            p_295393_.accept($$8.section);
         }

         boolean $$10 = Math.abs($$9.getOrigin().getX() - $$6.getX()) > 60
            || Math.abs($$9.getOrigin().getY() - $$6.getY()) > 60
            || Math.abs($$9.getOrigin().getZ() - $$6.getZ()) > 60;

         for (Direction $$11 : DIRECTIONS) {
            SectionRenderDispatcher.RenderSection $$12 = this.getRelativeFrom($$6, $$9, $$11);
            if ($$12 != null && (!p_295668_ || !$$8.hasDirection($$11.getOpposite()))) {
               if (p_295668_ && $$8.hasSourceDirections()) {
                  SectionRenderDispatcher.CompiledSection $$13 = $$9.getCompiled();
                  boolean $$14 = false;

                  for (int $$15 = 0; $$15 < DIRECTIONS.length; $$15++) {
                     if ($$8.hasSourceDirection($$15) && $$13.facesCanSeeEachother(DIRECTIONS[$$15].getOpposite(), $$11)) {
                        $$14 = true;
                        break;
                     }
                  }

                  if (!$$14) {
                     continue;
                  }
               }

               if (p_295668_ && $$10) {
                  BlockPos $$16 = $$12.getOrigin();
                  BlockPos $$17 = $$16.offset(
                     ($$11.getAxis() == Direction.Axis.X ? $$7.getX() <= $$16.getX() : $$7.getX() >= $$16.getX()) ? 0 : 16,
                     ($$11.getAxis() == Direction.Axis.Y ? $$7.getY() <= $$16.getY() : $$7.getY() >= $$16.getY()) ? 0 : 16,
                     ($$11.getAxis() == Direction.Axis.Z ? $$7.getZ() <= $$16.getZ() : $$7.getZ() >= $$16.getZ()) ? 0 : 16
                  );
                  Vec3 $$18 = new Vec3($$17.getX(), $$17.getY(), $$17.getZ());
                  Vec3 $$19 = p_294343_.subtract($$18).normalize().scale(CEILED_SECTION_DIAGONAL);
                  boolean $$20 = true;

                  while (p_294343_.subtract($$18).lengthSqr() > 3600.0) {
                     $$18 = $$18.add($$19);
                     LevelHeightAccessor $$21 = this.viewArea.getLevelHeightAccessor();
                     if ($$18.y > $$21.getMaxBuildHeight() || $$18.y < $$21.getMinBuildHeight()) {
                        break;
                     }

                     SectionRenderDispatcher.RenderSection $$22 = this.viewArea.getRenderSectionAt(BlockPos.containing($$18.x, $$18.y, $$18.z));
                     if ($$22 == null || p_295507_.sectionToNodeMap.get($$22) == null) {
                        $$20 = false;
                        break;
                     }
                  }

                  if (!$$20) {
                     continue;
                  }
               }

               SectionOcclusionGraph.Node $$23 = p_295507_.sectionToNodeMap.get($$12);
               if ($$23 != null) {
                  $$23.addSourceDirection($$11);
               } else {
                  SectionOcclusionGraph.Node $$24 = new SectionOcclusionGraph.Node($$12, $$11, $$8.step + 1);
                  $$24.setDirections($$8.directions, $$11);
                  if ($$12.hasAllNeighbors()) {
                     p_295598_.add($$24);
                     p_295507_.sectionToNodeMap.put($$12, $$24);
                  } else if (this.isInViewDistance($$6, $$12.getOrigin())) {
                     p_295507_.sectionToNodeMap.put($$12, $$24);
                     ((List)p_295507_.chunksWaitingForNeighbors.computeIfAbsent(ChunkPos.asLong($$12.getOrigin()), p_294377_ -> new ArrayList())).add($$12);
                  }
               }
            }
         }
      }
   }

   private boolean isInViewDistance(BlockPos p_295639_, BlockPos p_295511_) {
      int $$2 = SectionPos.blockToSectionCoord(p_295639_.getX());
      int $$3 = SectionPos.blockToSectionCoord(p_295639_.getZ());
      int $$4 = SectionPos.blockToSectionCoord(p_295511_.getX());
      int $$5 = SectionPos.blockToSectionCoord(p_295511_.getZ());
      return ChunkTrackingView.isInViewDistance($$2, $$3, this.viewArea.getViewDistance(), $$4, $$5);
   }

   @Nullable
   private SectionRenderDispatcher.RenderSection getRelativeFrom(BlockPos p_294150_, SectionRenderDispatcher.RenderSection p_295211_, Direction p_294491_) {
      BlockPos $$3 = p_295211_.getRelativeOrigin(p_294491_);
      if (!this.isInViewDistance(p_294150_, $$3)) {
         return null;
      } else {
         return Mth.abs(p_294150_.getY() - $$3.getY()) > this.viewArea.getViewDistance() * 16 ? null : this.viewArea.getRenderSectionAt($$3);
      }
   }

   @Nullable
   @VisibleForDebug
   protected SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection p_296364_) {
      return this.currentGraph.get().storage.sectionToNodeMap.get(p_296364_);
   }

   @OnlyIn(Dist.CLIENT)
   record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
      public GraphEvents() {
         this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
      }
   }

   @OnlyIn(Dist.CLIENT)
   record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
      public GraphState(int p_295649_) {
         this(new SectionOcclusionGraph.GraphStorage(p_295649_), new SectionOcclusionGraph.GraphEvents());
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class GraphStorage {
      public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
      public final LinkedHashSet<SectionOcclusionGraph.Node> renderSections;
      public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;

      public GraphStorage(int p_296094_) {
         this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(p_296094_);
         this.renderSections = new LinkedHashSet<>(p_296094_);
         this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap();
      }
   }

   @OnlyIn(Dist.CLIENT)
   @VisibleForDebug
   protected static class Node {
      @VisibleForDebug
      protected final SectionRenderDispatcher.RenderSection section;
      private byte sourceDirections;
      byte directions;
      @VisibleForDebug
      protected final int step;

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
      protected boolean hasSourceDirection(int p_294302_) {
         return (this.sourceDirections & 1 << p_294302_) > 0;
      }

      boolean hasSourceDirections() {
         return this.sourceDirections != 0;
      }

      @Override
      public int hashCode() {
         return this.section.getOrigin().hashCode();
      }

      @Override
      public boolean equals(Object p_295498_) {
         return !(p_295498_ instanceof SectionOcclusionGraph.Node $$1) ? false : this.section.getOrigin().equals($$1.section.getOrigin());
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
         int $$1 = p_295721_.index;
         return $$1 >= 0 && $$1 < this.nodes.length ? this.nodes[$$1] : null;
      }
   }
}
