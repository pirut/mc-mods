package com.mojang.realmsclient.gui.task;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.TimeSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DataFetcher {
   static final Logger LOGGER = LogUtils.getLogger();
   final Executor executor;
   final TimeUnit resolution;
   final TimeSource timeSource;

   public DataFetcher(Executor p_239381_, TimeUnit p_239382_, TimeSource p_239383_) {
      this.executor = p_239381_;
      this.resolution = p_239382_;
      this.timeSource = p_239383_;
   }

   public <T> DataFetcher.Task<T> createTask(String p_239623_, Callable<T> p_239624_, Duration p_239625_, RepeatedDelayStrategy p_239626_) {
      long $$4 = this.resolution.convert(p_239625_);
      if ($$4 == 0L) {
         throw new IllegalArgumentException("Period of " + p_239625_ + " too short for selected resolution of " + this.resolution);
      } else {
         return new DataFetcher.Task<>(p_239623_, p_239624_, $$4, p_239626_);
      }
   }

   public DataFetcher.Subscription createSubscription() {
      return new DataFetcher.Subscription();
   }

   @OnlyIn(Dist.CLIENT)
   record ComputationResult<T>(Either<T, Exception> value, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   class SubscribedTask<T> {
      private final DataFetcher.Task<T> task;
      private final Consumer<T> output;
      private long lastCheckTime = -1L;

      SubscribedTask(DataFetcher.Task<T> p_239959_, Consumer<T> p_239960_) {
         this.task = p_239959_;
         this.output = p_239960_;
      }

      void update(long p_239226_) {
         this.task.updateIfNeeded(p_239226_);
         this.runCallbackIfNeeded();
      }

      void runCallbackIfNeeded() {
         DataFetcher.SuccessfulComputationResult<T> $$0 = this.task.lastResult;
         if ($$0 != null && this.lastCheckTime < $$0.time) {
            this.output.accept($$0.value);
            this.lastCheckTime = $$0.time;
         }
      }

      void runCallback() {
         DataFetcher.SuccessfulComputationResult<T> $$0 = this.task.lastResult;
         if ($$0 != null) {
            this.output.accept($$0.value);
            this.lastCheckTime = $$0.time;
         }
      }

      void reset() {
         this.task.reset();
         this.lastCheckTime = -1L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class Subscription {
      private final List<DataFetcher.SubscribedTask<?>> subscriptions = new ArrayList<>();

      public <T> void subscribe(DataFetcher.Task<T> p_239442_, Consumer<T> p_239443_) {
         DataFetcher.SubscribedTask<T> $$2 = DataFetcher.this.new SubscribedTask<>(p_239442_, p_239443_);
         this.subscriptions.add($$2);
         $$2.runCallbackIfNeeded();
      }

      public void forceUpdate() {
         for (DataFetcher.SubscribedTask<?> $$0 : this.subscriptions) {
            $$0.runCallback();
         }
      }

      public void tick() {
         for (DataFetcher.SubscribedTask<?> $$0 : this.subscriptions) {
            $$0.update(DataFetcher.this.timeSource.get(DataFetcher.this.resolution));
         }
      }

      public void reset() {
         for (DataFetcher.SubscribedTask<?> $$0 : this.subscriptions) {
            $$0.reset();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record SuccessfulComputationResult<T>(T value, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   public class Task<T> {
      private final String id;
      private final Callable<T> updater;
      private final long period;
      private final RepeatedDelayStrategy repeatStrategy;
      @Nullable
      private CompletableFuture<DataFetcher.ComputationResult<T>> pendingTask;
      @Nullable
      DataFetcher.SuccessfulComputationResult<T> lastResult;
      private long nextUpdate = -1L;

      Task(String p_239074_, Callable<T> p_239075_, long p_239076_, RepeatedDelayStrategy p_239077_) {
         this.id = p_239074_;
         this.updater = p_239075_;
         this.period = p_239076_;
         this.repeatStrategy = p_239077_;
      }

      void updateIfNeeded(long p_239710_) {
         if (this.pendingTask != null) {
            DataFetcher.ComputationResult<T> $$1 = this.pendingTask.getNow(null);
            if ($$1 == null) {
               return;
            }

            this.pendingTask = null;
            long $$2 = $$1.time;
            $$1.value().ifLeft(p_239691_ -> {
               this.lastResult = new DataFetcher.SuccessfulComputationResult<>((T)p_239691_, $$2);
               this.nextUpdate = $$2 + this.period * this.repeatStrategy.delayCyclesAfterSuccess();
            }).ifRight(p_239281_ -> {
               long $$2x = this.repeatStrategy.delayCyclesAfterFailure();
               DataFetcher.LOGGER.warn("Failed to process task {}, will repeat after {} cycles", new Object[]{this.id, $$2x, p_239281_});
               this.nextUpdate = $$2 + this.period * $$2x;
            });
         }

         if (this.nextUpdate <= p_239710_) {
            this.pendingTask = CompletableFuture.supplyAsync(() -> {
               try {
                  T $$0 = this.updater.call();
                  long $$1x = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.left($$0), $$1x);
               } catch (Exception var4x) {
                  long $$3 = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.right(var4x), $$3);
               }
            }, DataFetcher.this.executor);
         }
      }

      public void reset() {
         this.pendingTask = null;
         this.lastResult = null;
         this.nextUpdate = -1L;
      }
   }
}
