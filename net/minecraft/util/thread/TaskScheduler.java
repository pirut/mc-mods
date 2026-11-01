package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface TaskScheduler<R extends Runnable> extends AutoCloseable {
   String name();

   void schedule(R var1);

   @Override
   default void close() {
   }

   R wrapRunnable(Runnable var1);

   default <Source> CompletableFuture<Source> scheduleWithResult(Consumer<CompletableFuture<Source>> p_371642_) {
      CompletableFuture<Source> completablefuture = new CompletableFuture<>();
      this.schedule(this.wrapRunnable(() -> p_371642_.accept(completablefuture)));
      return completablefuture;
   }

   static TaskScheduler<Runnable> wrapExecutor(final String p_371598_, final Executor p_371853_) {
      return new TaskScheduler<Runnable>() {
         @Override
         public String name() {
            return p_371598_;
         }

         @Override
         public void schedule(Runnable p_371439_) {
            p_371853_.execute(p_371439_);
         }

         @Override
         public Runnable wrapRunnable(Runnable p_371242_) {
            return p_371242_;
         }

         @Override
         public String toString() {
            return p_371598_;
         }
      };
   }
}
