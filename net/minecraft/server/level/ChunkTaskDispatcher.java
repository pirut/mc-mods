package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.SharedConstants;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkTaskDispatcher implements ChunkHolder.LevelChangeListener, AutoCloseable {
   public static final int DISPATCHER_PRIORITY_COUNT = 4;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ChunkTaskPriorityQueue queue;
   private final TaskScheduler<Runnable> executor;
   private final PriorityConsecutiveExecutor dispatcher;
   protected boolean sleeping;

   public ChunkTaskDispatcher(TaskScheduler<Runnable> p_371624_, Executor p_371524_) {
      this.queue = new ChunkTaskPriorityQueue(p_371624_.name() + "_queue");
      this.executor = p_371624_;
      this.dispatcher = new PriorityConsecutiveExecutor(4, p_371524_, "dispatcher");
      this.sleeping = true;
   }

   public boolean hasWork() {
      return this.dispatcher.hasWork() || this.queue.hasWork();
   }

   @Override
   public void onLevelChange(ChunkPos p_371270_, IntSupplier p_371804_, int p_371325_, IntConsumer p_371889_) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(0, () -> {
         int i = p_371804_.getAsInt();
         if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("RES {} {} -> {}", new Object[]{p_371270_, i, p_371325_});
         }

         this.queue.resortChunkTasks(i, p_371270_, p_371325_);
         p_371889_.accept(p_371325_);
      }));
   }

   public void release(long p_371596_, Runnable p_371195_, boolean p_371837_) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(1, () -> {
         this.queue.release(p_371596_, p_371837_);
         this.onRelease(p_371596_);
         if (this.sleeping) {
            this.sleeping = false;
            this.pollTask();
         }

         p_371195_.run();
      }));
   }

   public void submit(Runnable p_371654_, long p_371885_, IntSupplier p_371509_) {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(2, () -> {
         int i = p_371509_.getAsInt();
         if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("SUB {} {} {} {}", new Object[]{new ChunkPos(p_371885_), i, this.executor, this.queue});
         }

         this.queue.submit(p_371654_, p_371885_, i);
         if (this.sleeping) {
            this.sleeping = false;
            this.pollTask();
         }
      }));
   }

   protected void pollTask() {
      this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(3, () -> {
         ChunkTaskPriorityQueue.TasksForChunk chunktaskpriorityqueue$tasksforchunk = this.popTasks();
         if (chunktaskpriorityqueue$tasksforchunk == null) {
            this.sleeping = true;
         } else {
            this.scheduleForExecution(chunktaskpriorityqueue$tasksforchunk);
         }
      }));
   }

   protected void scheduleForExecution(ChunkTaskPriorityQueue.TasksForChunk p_371732_) {
      CompletableFuture.allOf(p_371732_.tasks().stream().map(p_371614_ -> this.executor.scheduleWithResult(p_371506_ -> {
         p_371614_.run();
         p_371506_.complete(Unit.INSTANCE);
      })).toArray(CompletableFuture[]::new)).thenAccept(p_371810_ -> this.pollTask());
   }

   protected void onRelease(long p_371807_) {
   }

   @Nullable
   protected ChunkTaskPriorityQueue.TasksForChunk popTasks() {
      return this.queue.pop();
   }

   @Override
   public void close() {
      this.executor.close();
   }
}
