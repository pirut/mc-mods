package net.minecraft.server.packs.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class SimpleReloadInstance<S> implements ReloadInstance {
   private static final int PREPARATION_PROGRESS_WEIGHT = 2;
   private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
   private static final int LISTENER_PROGRESS_WEIGHT = 1;
   final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
   @Nullable
   private CompletableFuture<List<S>> allDone;
   final Set<PreparableReloadListener> preparingListeners;
   private final int listenerCount;
   private final AtomicInteger startedTasks = new AtomicInteger();
   private final AtomicInteger finishedTasks = new AtomicInteger();
   private final AtomicInteger startedReloads = new AtomicInteger();
   private final AtomicInteger finishedReloads = new AtomicInteger();

   public static ReloadInstance of(
      ResourceManager p_10816_, List<PreparableReloadListener> p_10817_, Executor p_10818_, Executor p_10819_, CompletableFuture<Unit> p_10820_
   ) {
      SimpleReloadInstance<Void> simplereloadinstance = new SimpleReloadInstance<>(p_10817_);
      simplereloadinstance.startTasks(p_10818_, p_10819_, p_10816_, p_10817_, SimpleReloadInstance.StateFactory.SIMPLE, p_10820_);
      return simplereloadinstance;
   }

   protected SimpleReloadInstance(List<PreparableReloadListener> p_10811_) {
      this.listenerCount = p_10811_.size();
      this.preparingListeners = new HashSet<>(p_10811_);
   }

   protected void startTasks(
      Executor p_405149_,
      Executor p_404751_,
      ResourceManager p_404875_,
      List<PreparableReloadListener> p_404801_,
      SimpleReloadInstance.StateFactory<S> p_405705_,
      CompletableFuture<?> p_405320_
   ) {
      this.allDone = this.prepareTasks(p_405149_, p_404751_, p_404875_, p_404801_, p_405705_, p_405320_);
   }

   protected CompletableFuture<List<S>> prepareTasks(
      Executor p_405268_,
      Executor p_405682_,
      ResourceManager p_404829_,
      List<PreparableReloadListener> p_405049_,
      SimpleReloadInstance.StateFactory<S> p_405573_,
      CompletableFuture<?> p_405777_
   ) {
      Executor executor = p_404224_ -> {
         this.startedTasks.incrementAndGet();
         p_405268_.execute(() -> {
            p_404224_.run();
            this.finishedTasks.incrementAndGet();
         });
      };
      Executor executor1 = p_404227_ -> {
         this.startedReloads.incrementAndGet();
         p_405682_.execute(() -> {
            p_404227_.run();
            this.finishedReloads.incrementAndGet();
         });
      };
      this.startedTasks.incrementAndGet();
      p_405777_.thenRun(this.finishedTasks::incrementAndGet);
      PreparableReloadListener.SharedState preparablereloadlistener$sharedstate = new PreparableReloadListener.SharedState(p_404829_);
      p_405049_.forEach(p_432493_ -> p_432493_.prepareSharedState(preparablereloadlistener$sharedstate));
      CompletableFuture<?> completablefuture = p_405777_;
      List<CompletableFuture<S>> list = new ArrayList<>();

      for (PreparableReloadListener preparablereloadlistener : p_405049_) {
         PreparableReloadListener.PreparationBarrier preparablereloadlistener$preparationbarrier = this.createBarrierForListener(
            preparablereloadlistener, completablefuture, p_405682_
         );
         CompletableFuture<S> completablefuture1 = p_405573_.create(
            preparablereloadlistener$sharedstate, preparablereloadlistener$preparationbarrier, preparablereloadlistener, executor, executor1
         );
         list.add(completablefuture1);
         completablefuture = completablefuture1;
      }

      return Util.sequenceFailFast(list);
   }

   private PreparableReloadListener.PreparationBarrier createBarrierForListener(
      final PreparableReloadListener p_405338_, final CompletableFuture<?> p_404885_, final Executor p_405684_
   ) {
      return new PreparableReloadListener.PreparationBarrier() {
         @Override
         public <T> CompletableFuture<T> wait(T p_10858_) {
            p_405684_.execute(() -> {
               SimpleReloadInstance.this.preparingListeners.remove(p_405338_);
               if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                  SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
               }
            });
            return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)p_404885_, (p_10861_, p_10862_) -> p_10858_);
         }
      };
   }

   @Override
   public CompletableFuture<?> done() {
      return Objects.requireNonNull(this.allDone, "not started");
   }

   @Override
   public float getActualProgress() {
      int i = this.listenerCount - this.preparingListeners.size();
      float f = weightProgress(this.finishedTasks.get(), this.finishedReloads.get(), i);
      float f1 = weightProgress(this.startedTasks.get(), this.startedReloads.get(), this.listenerCount);
      return f / f1;
   }

   private static int weightProgress(int p_404933_, int p_405172_, int p_405849_) {
      return p_404933_ * 2 + p_405172_ * 2 + p_405849_ * 1;
   }

   public static ReloadInstance create(
      ResourceManager p_203835_,
      List<PreparableReloadListener> p_203836_,
      Executor p_203837_,
      Executor p_203838_,
      CompletableFuture<Unit> p_203839_,
      boolean p_203840_
   ) {
      return p_203840_
         ? ProfiledReloadInstance.of(p_203835_, p_203836_, p_203837_, p_203838_, p_203839_)
         : of(p_203835_, p_203836_, p_203837_, p_203838_, p_203839_);
   }

   @FunctionalInterface
   protected interface StateFactory<S> {
      SimpleReloadInstance.StateFactory<Void> SIMPLE = (p_432494_, p_432495_, p_432496_, p_432497_, p_432498_) -> p_432496_.reload(
         p_432494_, p_432497_, p_432495_, p_432498_
      );

      CompletableFuture<S> create(
         PreparableReloadListener.SharedState var1,
         PreparableReloadListener.PreparationBarrier var2,
         PreparableReloadListener var3,
         Executor var4,
         Executor var5
      );
   }
}
