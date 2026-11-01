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

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> inputs, BiFunction<K, U, V> operation, int maxTasksPerBatch, Executor executor) {
      int i = inputs.size();
      if (i == 0) {
         return CompletableFuture.completedFuture(Map.of());
      } else if (i == 1) {
         Entry<K, U> entry = inputs.entrySet().iterator().next();
         K object = entry.getKey();
         U object2 = entry.getValue();
         return CompletableFuture.supplyAsync(() -> {
            V object3 = operation.apply(object, object2);
            return object3 != null ? Map.of(object, object3) : Map.of();
         }, executor);
      } else {
         ParallelMapTransform.SplitterBase<K, U, V> splitterBase = (ParallelMapTransform.SplitterBase<K, U, V>)(i <= maxTasksPerBatch
            ? new ParallelMapTransform.SingleTaskSplitter<>(operation, i)
            : new ParallelMapTransform.BatchedTaskSplitter<>(operation, i, maxTasksPerBatch));
         return splitterBase.scheduleTasks(inputs, executor);
      }
   }

   public static <K, U, V> CompletableFuture<Map<K, V>> schedule(Map<K, U> inputs, BiFunction<K, U, V> operation, Executor executor) {
      int i = Util.maxAllowedExecutorThreads() * 16;
      return schedule(inputs, operation, i, executor);
   }

   static class BatchedTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      private final Map<K, V> result;
      private final int batchSize;
      private final int firstUndersizedBatchIndex;

      BatchedTaskSplitter(BiFunction<K, U, V> biFunction, int i, int j) {
         super(biFunction, i, j);
         this.result = new HashMap<>(i);
         this.batchSize = Mth.positiveCeilDiv(i, j);
         int k = this.batchSize * j;
         int l = k - i;
         this.firstUndersizedBatchIndex = j - l;

         assert this.firstUndersizedBatchIndex > 0 && this.firstUndersizedBatchIndex <= j;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(
         ParallelMapTransform.Container<K, U, V> container, int lastScheduledIndex, int currentIndex, Executor executor
      ) {
         int i = currentIndex - lastScheduledIndex;

         assert i == this.batchSize || i == this.batchSize - 1;

         return CompletableFuture.runAsync(createTask(this.result, lastScheduledIndex, currentIndex, container), executor);
      }

      @Override
      protected int batchSize(int batchIndex) {
         return batchIndex < this.firstUndersizedBatchIndex ? this.batchSize : this.batchSize - 1;
      }

      private static <K, U, V> Runnable createTask(
         Map<K, V> result, int lastScheduledIndex, int currentIndex, ParallelMapTransform.Container<K, U, V> container
      ) {
         return () -> {
            for (int k = lastScheduledIndex; k < currentIndex; k++) {
               container.applyOperation(k);
            }

            synchronized (result) {
               for (int l = lastScheduledIndex; l < currentIndex; l++) {
                  container.copyOut(l, result);
               }
            }
         };
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> future, ParallelMapTransform.Container<K, U, V> container) {
         Map<K, V> map = this.result;
         return future.thenApply(object -> map);
      }
   }

   record Container<K, U, V>(BiFunction<K, U, V> operation, Object[] keys, Object[] values) {
      public Container(BiFunction<K, U, V> operation, int size) {
         this(operation, new Object[size], new Object[size]);
      }

      public void put(int index, K key, U value) {
         this.keys[index] = key;
         this.values[index] = value;
      }

      @Nullable
      private K key(int index) {
         return (K)this.keys[index];
      }

      @Nullable
      private V output(int index) {
         return (V)this.values[index];
      }

      @Nullable
      private U input(int index) {
         return (U)this.values[index];
      }

      public void applyOperation(int index) {
         this.values[index] = this.operation.apply(this.key(index), this.input(index));
      }

      public void copyOut(int index, Map<K, V> outputMap) {
         V object = this.output(index);
         if (object != null) {
            K object2 = this.key(index);
            outputMap.put(object2, object);
         }
      }

      public int size() {
         return this.keys.length;
      }
   }

   static class SingleTaskSplitter<K, U, V> extends ParallelMapTransform.SplitterBase<K, U, V> {
      SingleTaskSplitter(BiFunction<K, U, V> operation, int size) {
         super(operation, size, size);
      }

      @Override
      protected int batchSize(int batchIndex) {
         return 1;
      }

      @Override
      protected CompletableFuture<?> scheduleBatch(
         ParallelMapTransform.Container<K, U, V> container, int lastScheduledIndex, int currentIndex, Executor executor
      ) {
         assert lastScheduledIndex + 1 == currentIndex;

         return CompletableFuture.runAsync(() -> container.applyOperation(lastScheduledIndex), executor);
      }

      @Override
      protected CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> future, ParallelMapTransform.Container<K, U, V> container) {
         return future.thenApply(object -> {
            Map<K, V> map = new HashMap<>(container.size());

            for (int i = 0; i < container.size(); i++) {
               container.copyOut(i, map);
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

      SplitterBase(BiFunction<K, U, V> operation, int containerSize, int numBatches) {
         this.container = new ParallelMapTransform.Container<>(operation, containerSize);
         this.tasks = new CompletableFuture[numBatches];
      }

      private int pendingBatchSize() {
         return this.currentIndex - this.lastScheduledIndex;
      }

      public CompletableFuture<Map<K, V>> scheduleTasks(Map<K, U> inputs, Executor executor) {
         inputs.forEach((object, object2) -> {
            this.container.put(this.currentIndex++, (K)object, (U)object2);
            if (this.pendingBatchSize() == this.batchSize(this.batchIndex)) {
               this.tasks[this.batchIndex++] = this.scheduleBatch(this.container, this.lastScheduledIndex, this.currentIndex, executor);
               this.lastScheduledIndex = this.currentIndex;
            }
         });

         assert this.currentIndex == this.container.size();

         assert this.lastScheduledIndex == this.currentIndex;

         assert this.batchIndex == this.tasks.length;

         return this.scheduleFinalOperation(CompletableFuture.allOf(this.tasks), this.container);
      }

      protected abstract int batchSize(int batchIndex);

      protected abstract CompletableFuture<?> scheduleBatch(
         ParallelMapTransform.Container<K, U, V> container, int lastScheduledIndex, int currentIndex, Executor executor
      );

      protected abstract CompletableFuture<Map<K, V>> scheduleFinalOperation(CompletableFuture<?> future, ParallelMapTransform.Container<K, U, V> container);
   }
}
