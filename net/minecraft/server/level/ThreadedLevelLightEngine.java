package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;

public class ThreadedLevelLightEngine extends LevelLightEngine implements AutoCloseable {
   public static final int DEFAULT_BATCH_SIZE = 1000;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ProcessorMailbox<Runnable> taskMailbox;
   private final ObjectList<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> lightTasks = new ObjectArrayList();
   private final ChunkMap chunkMap;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> sorterMailbox;
   private final int taskPerBatch = 1000;
   private final AtomicBoolean scheduled = new AtomicBoolean();

   public ThreadedLevelLightEngine(
      LightChunkGetter p_9305_,
      ChunkMap p_9306_,
      boolean p_9307_,
      ProcessorMailbox<Runnable> p_9308_,
      ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> p_9309_
   ) {
      super(p_9305_, true, p_9307_);
      this.chunkMap = p_9306_;
      this.sorterMailbox = p_9309_;
      this.taskMailbox = p_9308_;
   }

   @Override
   public void close() {
   }

   @Override
   public int runLightUpdates() {
      throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
   }

   @Override
   public void checkBlock(BlockPos p_9357_) {
      BlockPos $$1 = p_9357_.immutable();
      this.addTask(
         SectionPos.blockToSectionCoord(p_9357_.getX()),
         SectionPos.blockToSectionCoord(p_9357_.getZ()),
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.checkBlock($$1), () -> "checkBlock " + $$1)
      );
   }

   protected void updateChunkStatus(ChunkPos p_9331_) {
      this.addTask(p_9331_.x, p_9331_.z, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
         super.retainData(p_9331_, false);
         super.setLightEnabled(p_9331_, false);

         for (int $$1 = this.getMinLightSection(); $$1 < this.getMaxLightSection(); $$1++) {
            super.queueSectionData(LightLayer.BLOCK, SectionPos.of(p_9331_, $$1), null);
            super.queueSectionData(LightLayer.SKY, SectionPos.of(p_9331_, $$1), null);
         }

         for (int $$2 = this.levelHeightAccessor.getMinSection(); $$2 < this.levelHeightAccessor.getMaxSection(); $$2++) {
            super.updateSectionStatus(SectionPos.of(p_9331_, $$2), true);
         }
      }, () -> "updateChunkStatus " + p_9331_ + " true"));
   }

   @Override
   public void updateSectionStatus(SectionPos p_9364_, boolean p_9365_) {
      this.addTask(
         p_9364_.x(),
         p_9364_.z(),
         () -> 0,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.updateSectionStatus(p_9364_, p_9365_), () -> "updateSectionStatus " + p_9364_ + " " + p_9365_)
      );
   }

   @Override
   public void propagateLightSources(ChunkPos p_285029_) {
      this.addTask(
         p_285029_.x,
         p_285029_.z,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.propagateLightSources(p_285029_), () -> "propagateLight " + p_285029_)
      );
   }

   @Override
   public void setLightEnabled(ChunkPos p_9336_, boolean p_9337_) {
      this.addTask(
         p_9336_.x,
         p_9336_.z,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.setLightEnabled(p_9336_, p_9337_), () -> "enableLight " + p_9336_ + " " + p_9337_)
      );
   }

   @Override
   public void queueSectionData(LightLayer p_285046_, SectionPos p_285496_, @Nullable DataLayer p_285495_) {
      this.addTask(
         p_285496_.x(),
         p_285496_.z(),
         () -> 0,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.queueSectionData(p_285046_, p_285496_, p_285495_), () -> "queueData " + p_285496_)
      );
   }

   private void addTask(int p_9313_, int p_9314_, ThreadedLevelLightEngine.TaskType p_9315_, Runnable p_9316_) {
      this.addTask(p_9313_, p_9314_, this.chunkMap.getChunkQueueLevel(ChunkPos.asLong(p_9313_, p_9314_)), p_9315_, p_9316_);
   }

   private void addTask(int p_9318_, int p_9319_, IntSupplier p_9320_, ThreadedLevelLightEngine.TaskType p_9321_, Runnable p_9322_) {
      this.sorterMailbox.tell(ChunkTaskPriorityQueueSorter.message(() -> {
         this.lightTasks.add(Pair.of(p_9321_, p_9322_));
         if (this.lightTasks.size() >= 1000) {
            this.runUpdate();
         }
      }, ChunkPos.asLong(p_9318_, p_9319_), p_9320_));
   }

   @Override
   public void retainData(ChunkPos p_9370_, boolean p_9371_) {
      this.addTask(
         p_9370_.x,
         p_9370_.z,
         () -> 0,
         ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
         Util.name(() -> super.retainData(p_9370_, p_9371_), () -> "retainData " + p_9370_)
      );
   }

   public CompletableFuture<ChunkAccess> initializeLight(ChunkAccess p_285128_, boolean p_285441_) {
      ChunkPos $$2 = p_285128_.getPos();
      this.addTask($$2.x, $$2.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
         LevelChunkSection[] $$2x = p_285128_.getSections();

         for (int $$3 = 0; $$3 < p_285128_.getSectionsCount(); $$3++) {
            LevelChunkSection $$4 = $$2x[$$3];
            if (!$$4.hasOnlyAir()) {
               int $$5 = this.levelHeightAccessor.getSectionYFromSectionIndex($$3);
               super.updateSectionStatus(SectionPos.of($$2, $$5), false);
            }
         }
      }, () -> "initializeLight: " + $$2));
      return CompletableFuture.supplyAsync(() -> {
         super.setLightEnabled($$2, p_285441_);
         super.retainData($$2, false);
         return p_285128_;
      }, p_215135_ -> this.addTask($$2.x, $$2.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, p_215135_));
   }

   public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess p_9354_, boolean p_9355_) {
      ChunkPos $$2 = p_9354_.getPos();
      p_9354_.setLightCorrect(false);
      this.addTask($$2.x, $$2.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
         if (!p_9355_) {
            super.propagateLightSources($$2);
         }
      }, () -> "lightChunk " + $$2 + " " + p_9355_));
      return CompletableFuture.supplyAsync(() -> {
         p_9354_.setLightCorrect(true);
         return p_9354_;
      }, p_280982_ -> this.addTask($$2.x, $$2.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, p_280982_));
   }

   public void tryScheduleUpdate() {
      if ((!this.lightTasks.isEmpty() || super.hasLightWork()) && this.scheduled.compareAndSet(false, true)) {
         this.taskMailbox.tell(() -> {
            this.runUpdate();
            this.scheduled.set(false);
         });
      }
   }

   private void runUpdate() {
      int $$0 = Math.min(this.lightTasks.size(), 1000);
      ObjectListIterator<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> $$1 = this.lightTasks.iterator();

      int $$2;
      for ($$2 = 0; $$1.hasNext() && $$2 < $$0; $$2++) {
         Pair<ThreadedLevelLightEngine.TaskType, Runnable> $$3 = (Pair<ThreadedLevelLightEngine.TaskType, Runnable>)$$1.next();
         if ($$3.getFirst() == ThreadedLevelLightEngine.TaskType.PRE_UPDATE) {
            ((Runnable)$$3.getSecond()).run();
         }
      }

      $$1.back($$2);
      super.runLightUpdates();

      for (int var5 = 0; $$1.hasNext() && var5 < $$0; var5++) {
         Pair<ThreadedLevelLightEngine.TaskType, Runnable> $$4 = (Pair<ThreadedLevelLightEngine.TaskType, Runnable>)$$1.next();
         if ($$4.getFirst() == ThreadedLevelLightEngine.TaskType.POST_UPDATE) {
            ((Runnable)$$4.getSecond()).run();
         }

         $$1.remove();
      }
   }

   public CompletableFuture<?> waitForPendingTasks(int p_301257_, int p_300921_) {
      return CompletableFuture.runAsync(() -> {}, p_300778_ -> this.addTask(p_301257_, p_300921_, ThreadedLevelLightEngine.TaskType.POST_UPDATE, p_300778_));
   }

   static enum TaskType {
      PRE_UPDATE,
      POST_UPDATE;
   }
}
