package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class ThrottlingChunkTaskDispatcher extends ChunkTaskDispatcher {
   private final LongSet chunkPositionsInExecution = new LongOpenHashSet();
   private final int maxChunksInExecution;
   private final String executorSchedulerName;

   public ThrottlingChunkTaskDispatcher(TaskScheduler<Runnable> p_371877_, Executor p_371691_, int p_371756_) {
      super(p_371877_, p_371691_);
      this.maxChunksInExecution = p_371756_;
      this.executorSchedulerName = p_371877_.name();
   }

   @Override
   protected void onRelease(long p_371820_) {
      this.chunkPositionsInExecution.remove(p_371820_);
   }

   @Nullable
   @Override
   protected ChunkTaskPriorityQueue.TasksForChunk popTasks() {
      return this.chunkPositionsInExecution.size() < this.maxChunksInExecution ? super.popTasks() : null;
   }

   @Override
   protected void scheduleForExecution(ChunkTaskPriorityQueue.TasksForChunk p_371264_) {
      this.chunkPositionsInExecution.add(p_371264_.chunkPos());
      super.scheduleForExecution(p_371264_);
   }

   @VisibleForTesting
   public String getDebugStatus() {
      return this.executorSchedulerName
         + "=["
         + this.chunkPositionsInExecution.longStream().mapToObj(p_386300_ -> p_386300_ + ":" + new ChunkPos(p_386300_)).collect(Collectors.joining(","))
         + "], s="
         + this.sleeping;
   }
}
