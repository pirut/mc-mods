package net.minecraft.server.level;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class ChunkTaskPriorityQueue {
   public static final int PRIORITY_LEVEL_COUNT = ChunkLevel.MAX_LEVEL + 2;
   private final List<Long2ObjectLinkedOpenHashMap<List<Runnable>>> queuesPerPriority = IntStream.range(0, PRIORITY_LEVEL_COUNT)
      .mapToObj(i -> new Long2ObjectLinkedOpenHashMap())
      .toList();
   private volatile int topPriorityQueueIndex = PRIORITY_LEVEL_COUNT;
   private final String name;

   public ChunkTaskPriorityQueue(String name) {
      this.name = name;
   }

   protected void resortChunkTasks(int queueLevel, ChunkPos chunkPos, int ticketLevel) {
      if (queueLevel < PRIORITY_LEVEL_COUNT) {
         Long2ObjectLinkedOpenHashMap<List<Runnable>> long2ObjectLinkedOpenHashMap = this.queuesPerPriority.get(queueLevel);
         List<Runnable> list = (List<Runnable>)long2ObjectLinkedOpenHashMap.remove(chunkPos.toLong());
         if (queueLevel == this.topPriorityQueueIndex) {
            while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
               this.topPriorityQueueIndex++;
            }
         }

         if (list != null && !list.isEmpty()) {
            ((List)this.queuesPerPriority.get(ticketLevel).computeIfAbsent(chunkPos.toLong(), l -> Lists.newArrayList())).addAll(list);
            this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, ticketLevel);
         }
      }
   }

   protected void submit(Runnable task, long chunkPos, int queueLevel) {
      ((List)this.queuesPerPriority.get(queueLevel).computeIfAbsent(chunkPos, l -> Lists.newArrayList())).add(task);
      this.topPriorityQueueIndex = Math.min(this.topPriorityQueueIndex, queueLevel);
   }

   protected void release(long chunkPos, boolean fullClear) {
      for (Long2ObjectLinkedOpenHashMap<List<Runnable>> long2ObjectLinkedOpenHashMap : this.queuesPerPriority) {
         List<Runnable> list = (List<Runnable>)long2ObjectLinkedOpenHashMap.get(chunkPos);
         if (list != null) {
            if (fullClear) {
               list.clear();
            }

            if (list.isEmpty()) {
               long2ObjectLinkedOpenHashMap.remove(chunkPos);
            }
         }
      }

      while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
         this.topPriorityQueueIndex++;
      }
   }

   @Nullable
   public ChunkTaskPriorityQueue.TasksForChunk pop() {
      if (!this.hasWork()) {
         return null;
      } else {
         int i = this.topPriorityQueueIndex;
         Long2ObjectLinkedOpenHashMap<List<Runnable>> long2ObjectLinkedOpenHashMap = this.queuesPerPriority.get(i);
         long l = long2ObjectLinkedOpenHashMap.firstLongKey();
         List<Runnable> list = (List<Runnable>)long2ObjectLinkedOpenHashMap.removeFirst();

         while (this.hasWork() && this.queuesPerPriority.get(this.topPriorityQueueIndex).isEmpty()) {
            this.topPriorityQueueIndex++;
         }

         return new ChunkTaskPriorityQueue.TasksForChunk(l, list);
      }
   }

   public boolean hasWork() {
      return this.topPriorityQueueIndex < PRIORITY_LEVEL_COUNT;
   }

   @Override
   public String toString() {
      return this.name + " " + this.topPriorityQueueIndex + "...";
   }

   public record TasksForChunk(long chunkPos, List<Runnable> tasks) {
   }
}
