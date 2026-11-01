package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Stopwatch total = Stopwatch.createUnstarted();

   public static ReloadInstance of(
      ResourceManager resourceManager,
      List<PreparableReloadListener> listeners,
      Executor backgroundExecutor,
      Executor gameExecutor,
      CompletableFuture<Unit> alsoWaitedFor
   ) {
      ProfiledReloadInstance profiledReloadInstance = new ProfiledReloadInstance(listeners);
      profiledReloadInstance.startTasks(
         backgroundExecutor,
         gameExecutor,
         resourceManager,
         listeners,
         (sharedState, preparationBarrier, preparableReloadListener, executor2, executor3) -> {
            AtomicLong atomicLong = new AtomicLong();
            AtomicLong atomicLong2 = new AtomicLong();
            AtomicLong atomicLong3 = new AtomicLong();
            AtomicLong atomicLong4 = new AtomicLong();
            CompletableFuture<Void> completableFuture = preparableReloadListener.reload(
               sharedState,
               profiledExecutor(executor2, atomicLong, atomicLong2, preparableReloadListener.getName()),
               preparationBarrier,
               profiledExecutor(executor3, atomicLong3, atomicLong4, preparableReloadListener.getName())
            );
            return completableFuture.thenApplyAsync(void_ -> {
               LOGGER.debug("Finished reloading {}", preparableReloadListener.getName());
               return new ProfiledReloadInstance.State(preparableReloadListener.getName(), atomicLong, atomicLong2, atomicLong3, atomicLong4);
            }, gameExecutor);
         },
         alsoWaitedFor
      );
      return profiledReloadInstance;
   }

   private ProfiledReloadInstance(List<PreparableReloadListener> listeners) {
      super(listeners);
      this.total.start();
   }

   @Override
   protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(
      Executor backgroundExecutor,
      Executor gameExecutor,
      ResourceManager resourceManager,
      List<PreparableReloadListener> listeners,
      SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> stateFactory,
      CompletableFuture<?> alsoWaitedFor
   ) {
      return super.prepareTasks(backgroundExecutor, gameExecutor, resourceManager, listeners, stateFactory, alsoWaitedFor)
         .thenApplyAsync(this::finish, gameExecutor);
   }

   private static Executor profiledExecutor(Executor executor, AtomicLong timeTaken, AtomicLong timesRun, String name) {
      return runnable -> executor.execute(() -> {
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.push(name);
         long l = Util.getNanos();
         runnable.run();
         timeTaken.addAndGet(Util.getNanos() - l);
         timesRun.incrementAndGet();
         profilerFiller.pop();
      });
   }

   private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> dataPoints) {
      this.total.stop();
      long l = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledReloadInstance.State state : dataPoints) {
         long m = TimeUnit.NANOSECONDS.toMillis(state.preparationNanos.get());
         long n = state.preparationCount.get();
         long o = TimeUnit.NANOSECONDS.toMillis(state.reloadNanos.get());
         long p = state.reloadCount.get();
         long q = m + o;
         long r = n + p;
         String string = state.name;
         LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{string, r, q, n, m, p, o});
         l += o;
      }

      LOGGER.info("Total blocking time: {} ms", l);
      return dataPoints;
   }

   public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
   }
}
