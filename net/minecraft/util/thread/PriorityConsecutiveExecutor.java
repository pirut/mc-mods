package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.util.profiling.metrics.MetricsRegistry;

public class PriorityConsecutiveExecutor extends AbstractConsecutiveExecutor<StrictQueue.RunnableWithPriority> {
   public PriorityConsecutiveExecutor(int p_371706_, Executor p_371186_, String p_371555_) {
      super(new StrictQueue.FixedPriorityQueue(p_371706_), p_371186_, p_371555_);
      MetricsRegistry.INSTANCE.add(this);
   }

   public StrictQueue.RunnableWithPriority wrapRunnable(Runnable p_371771_) {
      return new StrictQueue.RunnableWithPriority(0, p_371771_);
   }

   public <Source> CompletableFuture<Source> scheduleWithResult(int p_371862_, Consumer<CompletableFuture<Source>> p_371410_) {
      CompletableFuture<Source> completablefuture = new CompletableFuture<>();
      this.schedule(new StrictQueue.RunnableWithPriority(p_371862_, () -> p_371410_.accept(completablefuture)));
      return completablefuture;
   }
}
