package net.minecraft.util.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class ParallelMapTransform {
   private static final int DEFAULT_TASKS_PER_THREAD = 16;

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> p_405594_, BiFunction<K, U, V> p_404799_, int p_404974_, Executor p_404942_) {
      int i = p_405594_.size();
      if (i == 0) {
         return CompletableFuture.completedFuture(Map.of());
      } else if (i == 1) {
         Entry<K, U> entry = p_405594_.entrySet().iterator().next();
         K k = entry.getKey();
         U u = entry.getValue();
         return CompletableFuture.supplyAsync(() -> {
            V v = p_404799_.apply(k, u);
            return v != null ? Map.of(k, v) : Map.of();
         }, p_404942_);
      } else {
         ParallelMapTransform.SplitterBase<K, U, V> splitterbase = (ParallelMapTransform.SplitterBase<K, U, V>)(i <= p_404974_
            ? new ParallelMapTransform.SingleTaskSplitter<>(p_404799_, i)
            : new ParallelMapTransform.BatchedTaskSplitter<>(p_404799_, i, p_404974_));
         return splitterbase.scheduleTasks(p_405594_, p_404942_);
      }
   }

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> p_405308_, BiFunction<K, U, V> p_405883_, Executor p_404950_) {
      int i = Util.maxAllowedExecutorThreads() * 16;
      return schedule(p_405308_, p_405883_, i, p_404950_);
   }

   static class BatchedTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      private final Map<K, V> result;
      private final int batchSize;
      private final int firstUndersizedBatchIndex;

      BatchedTaskSplitter(BiFunction<K, U, V> p_405224_, int p_405356_, int p_405090_) {
         super(p_405224_, p_405356_, p_405090_);
         this.result = new HashMap<>(p_405356_);
         this.batchSize = Mth.positiveCeilDiv(p_405356_, p_405090_);
         int i = this.batchSize * p_405090_;
         int j = i - p_405356_;
         this.firstUndersizedBatchIndex = p_405090_ - j;

         assert this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= p_405090_;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_405061_, int p_405498_, int p_405344_, Executor p_404656_) {
         int i = p_405344_ - p_405498_;

         assert i == this.batchSize || i == this.batchSize - 1;

         return CompletableFuture.runAsync(createTask(this.result, p_405498_, p_405344_, p_405061_), p_404656_);
      }

      @Override
      protected int batchSize(int p_405542_) {
         return p_405542_ < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
      }

      private static <K, U, V> Runnable createTask(Map<K, V> p_404882_, int p_404840_, int p_405439_, ParallelMapTransform.Container<K, U, V> p_405382_) {
         return () -> {
            for (int i = p_404840_; i < p_405439_; i++) {
               p_405382_.applyOperation(i);
            }

            synchronized (p_404882_) {
               for (int j = p_404840_; j < p_405439_; j++) {
                  p_405382_.copyOut(j, p_404882_);
               }
            }
         };
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_405225_, ParallelMapTransform.Container<K, U, V> p_404779_) {
         Map<K, V> map = this.result;
         return p_405225_.thenApply(p_405484_ -> map);
      }
   }

   record Container<K, U, V>(BiFunction<K, U, V> operation, Object[] keys, Object[] values) {
      public Container(BiFunction<K, U, V> p_405815_, int p_405453_) {
         this(p_405815_, new Object[p_405453_], new Object[p_405453_]);
      }

      public void put(int p_404987_, K p_404805_, U p_405660_) {
         this.keys[p_404987_] = p_404805_;
         this.values[p_404987_] = p_405660_;
      }

      @Nullable
      private K key(int p_404792_) {
         return (K)this.keys[p_404792_];
      }

      @Nullable
      private V output(int p_405132_) {
         return (V)this.values[p_405132_];
      }

      @Nullable
      private U input(int p_405264_) {
         return (U)this.values[p_405264_];
      }

      public void applyOperation(int p_404789_) {
         this.values[p_404789_] = this.operation.apply(this.key(p_404789_), this.input(p_404789_));
      }

      public void copyOut(int p_405686_, Map<K, V> p_405566_) {
         V v = this.output(p_405686_);
         if (v != null) {
            K k = this.key(p_405686_);
            p_405566_.put(k, v);
         }
      }

      public int size() {
         return this.keys.length;
      }
   }

   static class SingleTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      SingleTaskSplitter(BiFunction<K, U, V> p_405536_, int p_405190_) {
         super(p_405536_, p_405190_, p_405190_);
      }

      @Override
      protected int batchSize(int p_405267_) {
         return 1;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> p_404951_, int p_405718_, int p_404688_, Executor p_404914_) {
         assert p_405718_ + 1 == p_404688_;

         return CompletableFuture.runAsync(() -> p_404951_.applyOperation(p_405718_), p_404914_);
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> p_404928_, ParallelMapTransform.Container<K, U, V> p_405541_) {
         return p_404928_.thenApply(p_405680_ -> {
            Map<K, V> map = new HashMap<>(p_405541_.size());

            for (int i = 0; i < p_405541_.size(); i++) {
               p_405541_.copyOut(i, map);
            }

            return map;
         });
      }
   }

   abstract static class SplitterBase<K, U, V> {
      private int lastScheduledIndex;
      private int currentIndex;
      private final CompletableFuture<?>[] tasks;
      private int batchIndex;
      private final ParallelMapTransform.Container<K, U, V> container;

      SplitterBase(BiFunction<K, U, V> p_405196_, int p_405517_, int p_405788_) {
         this.container = new ParallelMapTransform.Container<>(p_405196_, p_405517_);
         this.tasks = new CompletableFuture[p_405788_];
      }

      private int pendingBatchSize() {
         return this.currentIndex - this.lastScheduledIndex;
      }

      public CompletableFuture<Map<K, V>> scheduleTasks(Map<K, U> p_405332_, Executor p_405350_) {
         p_405332_.forEach((p_405351_, p_405574_) -> {
            this.container.put(this.currentIndex++, (K)p_405351_, (U)p_405574_);
            if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
               this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, p_405350_);
               this.lastScheduledIndex = this.currentIndex;
            }
         });

         assert this.currentIndex == this.container.size();

         assert this.lastScheduledIndex == this.currentIndex;

         assert this.batchIndex == this.tasks.length;

         return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
      }

      protected abstract int batchSize(int var1);

      protected abstract CompletableFuture<?> scheduleBatch(ParallelMapTransform.Container<K, U, V> var1, int var2, int var3, Executor var4);

      protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> var1, ParallelMapTransform.Container<K, U, V> var2);
   }
}
