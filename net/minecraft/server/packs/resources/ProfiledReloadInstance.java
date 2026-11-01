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
      ResourceManager p_404634_, List<PreparableReloadListener> p_404750_, Executor p_405435_, Executor p_405297_, CompletableFuture<Unit> p_405418_
   ) {
      ProfiledReloadInstance profiledreloadinstance = new ProfiledReloadInstance(p_404750_);
      profiledreloadinstance.startTasks(
         p_405435_,
         p_405297_,
         p_404634_,
         p_404750_,
         (p_432487_, p_432488_, p_432489_, p_432490_, p_432491_) -> {
            AtomicLong atomiclong = new AtomicLong();
            AtomicLong atomiclong1 = new AtomicLong();
            AtomicLong atomiclong2 = new AtomicLong();
            AtomicLong atomiclong3 = new AtomicLong();
            CompletableFuture<Void> completablefuture = p_432489_.reload(
               p_432487_,
               profiledExecutor(p_432490_, atomiclong, atomiclong1, p_432489_.getName()),
               p_432488_,
               profiledExecutor(p_432491_, atomiclong2, atomiclong3, p_432489_.getName())
            );
            return completablefuture.thenApplyAsync(p_404215_ -> {
               LOGGER.debug("Finished reloading {}", p_432489_.getName());
               return new ProfiledReloadInstance.State(p_432489_.getName(), atomiclong, atomiclong1, atomiclong2, atomiclong3);
            }, p_405297_);
         },
         p_405418_
      );
      return profiledreloadinstance;
   }

   private ProfiledReloadInstance(List<PreparableReloadListener> p_10650_) {
      super(p_10650_);
      this.total.start();
   }

   @Override
   protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(
      Executor p_405318_,
      Executor p_405642_,
      ResourceManager p_404932_,
      List<PreparableReloadListener> p_404797_,
      SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> p_405213_,
      CompletableFuture<?> p_405774_
   ) {
      return super.prepareTasks(p_405318_, p_405642_, p_404932_, p_404797_, p_405213_, p_405774_).thenApplyAsync(this::finish, p_405642_);
   }

   private static Executor profiledExecutor(Executor p_373101_, AtomicLong p_373001_, AtomicLong p_404775_, String p_372795_) {
      return p_404205_ -> p_373101_.execute(() -> {
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push(p_372795_);
         long i = Util.getNanos();
         p_404205_.run();
         p_373001_.addAndGet(Util.getNanos() - i);
         p_404775_.incrementAndGet();
         profilerfiller.pop();
      });
   }

   private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> p_215484_) {
      this.total.stop();
      long i = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledReloadInstance.State profiledreloadinstance$state : p_215484_) {
         long j = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.preparationNanos.get());
         long k = profiledreloadinstance$state.preparationCount.get();
         long l = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.reloadNanos.get());
         long i1 = profiledreloadinstance$state.reloadCount.get();
         long j1 = j + l;
         long k1 = k + i1;
         String s = profiledreloadinstance$state.name;
         LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{s, k1, j1, k, j, i1, l});
         i += l;
      }

      LOGGER.info("Total blocking time: {} ms", i);
      return p_215484_;
   }

   public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
   }
}
