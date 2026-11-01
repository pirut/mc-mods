package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.InactiveProfiler;

public class SimpleReloadInstance<S> implements ReloadInstance {
   private static final int PREPARATION_PROGRESS_WEIGHT = 2;
   private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
   private static final int LISTENER_PROGRESS_WEIGHT = 1;
   protected final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
   protected CompletableFuture<List<S>> allDone;
   final Set<PreparableReloadListener> preparingListeners;
   private final int listenerCount;
   private int startedReloads;
   private int finishedReloads;
   private final AtomicInteger startedTaskCounter = new AtomicInteger();
   private final AtomicInteger doneTaskCounter = new AtomicInteger();

   public static SimpleReloadInstance<Void> of(
      ResourceManager p_10816_, List<PreparableReloadListener> p_10817_, Executor p_10818_, Executor p_10819_, CompletableFuture<Unit> p_10820_
   ) {
      return new SimpleReloadInstance<>(
         p_10818_,
         p_10819_,
         p_10816_,
         p_10817_,
         (p_10829_, p_10830_, p_10831_, p_10832_, p_10833_) -> p_10831_.reload(
            p_10829_, p_10830_, InactiveProfiler.INSTANCE, InactiveProfiler.INSTANCE, p_10818_, p_10833_
         ),
         p_10820_
      );
   }

   protected SimpleReloadInstance(
      Executor p_10808_,
      final Executor p_10809_,
      ResourceManager p_10810_,
      List<PreparableReloadListener> p_10811_,
      SimpleReloadInstance.StateFactory<S> p_10812_,
      CompletableFuture<Unit> p_10813_
   ) {
      this.listenerCount = p_10811_.size();
      this.startedTaskCounter.incrementAndGet();
      p_10813_.thenRun(this.doneTaskCounter::incrementAndGet);
      List<CompletableFuture<S>> $$6 = Lists.newArrayList();
      CompletableFuture<?> $$7 = p_10813_;
      this.preparingListeners = Sets.newHashSet(p_10811_);

      for (final PreparableReloadListener $$8 : p_10811_) {
         final CompletableFuture<?> $$9 = $$7;
         CompletableFuture<S> $$10 = p_10812_.create(new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T p_10858_) {
               p_10809_.execute(() -> {
                  SimpleReloadInstance.this.preparingListeners.remove($$8);
                  if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                     SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
                  }
               });
               return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)$$9, (p_10861_, p_10862_) -> p_10858_);
            }
         }, p_10810_, $$8, p_10842_ -> {
            this.startedTaskCounter.incrementAndGet();
            p_10808_.execute(() -> {
               p_10842_.run();
               this.doneTaskCounter.incrementAndGet();
            });
         }, p_10836_ -> {
            this.startedReloads++;
            p_10809_.execute(() -> {
               p_10836_.run();
               this.finishedReloads++;
            });
         });
         $$6.add($$10);
         $$7 = $$10;
      }

      this.allDone = Util.sequenceFailFast($$6);
   }

   @Override
   public CompletableFuture<?> done() {
      return this.allDone;
   }

   @Override
   public float getActualProgress() {
      int $$0 = this.listenerCount - this.preparingListeners.size();
      float $$1 = this.doneTaskCounter.get() * 2 + this.finishedReloads * 2 + $$0 * 1;
      float $$2 = this.startedTaskCounter.get() * 2 + this.startedReloads * 2 + this.listenerCount * 1;
      return $$1 / $$2;
   }

   public static ReloadInstance create(
      ResourceManager p_203835_,
      List<PreparableReloadListener> p_203836_,
      Executor p_203837_,
      Executor p_203838_,
      CompletableFuture<Unit> p_203839_,
      boolean p_203840_
   ) {
      return (ReloadInstance)(p_203840_
         ? new ProfiledReloadInstance(p_203835_, p_203836_, p_203837_, p_203838_, p_203839_)
         : of(p_203835_, p_203836_, p_203837_, p_203838_, p_203839_));
   }

   protected interface StateFactory<S> {
      CompletableFuture<S> create(
         PreparableReloadListener.PreparationBarrier var1, ResourceManager var2, PreparableReloadListener var3, Executor var4, Executor var5
      );
   }
}
