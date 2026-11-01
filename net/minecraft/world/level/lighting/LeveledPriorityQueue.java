package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

public class LeveledPriorityQueue {
   private final int levelCount;
   private final LongLinkedOpenHashSet[] queues;
   private int firstQueuedLevel;

   public LeveledPriorityQueue(int p_278289_, final int p_278259_) {
      this.levelCount = p_278289_;
      this.queues = new LongLinkedOpenHashSet[p_278289_];

      for (int $$2 = 0; $$2 < p_278289_; $$2++) {
         this.queues[$$2] = new LongLinkedOpenHashSet(p_278259_, 0.5F) {
            protected void rehash(int p_278313_) {
               if (p_278313_ > p_278259_) {
                  super.rehash(p_278313_);
               }
            }
         };
      }

      this.firstQueuedLevel = p_278289_;
   }

   public long removeFirstLong() {
      LongLinkedOpenHashSet $$0 = this.queues[this.firstQueuedLevel];
      long $$1 = $$0.removeFirstLong();
      if ($$0.isEmpty()) {
         this.checkFirstQueuedLevel(this.levelCount);
      }

      return $$1;
   }

   public boolean isEmpty() {
      return this.firstQueuedLevel >= this.levelCount;
   }

   public void dequeue(long p_278232_, int p_278338_, int p_278345_) {
      LongLinkedOpenHashSet $$3 = this.queues[p_278338_];
      $$3.remove(p_278232_);
      if ($$3.isEmpty() && this.firstQueuedLevel == p_278338_) {
         this.checkFirstQueuedLevel(p_278345_);
      }
   }

   public void enqueue(long p_278311_, int p_278335_) {
      this.queues[p_278335_].add(p_278311_);
      if (this.firstQueuedLevel > p_278335_) {
         this.firstQueuedLevel = p_278335_;
      }
   }

   private void checkFirstQueuedLevel(int p_278303_) {
      int $$1 = this.firstQueuedLevel;
      this.firstQueuedLevel = p_278303_;

      for (int $$2 = $$1 + 1; $$2 < p_278303_; $$2++) {
         if (!this.queues[$$2].isEmpty()) {
            this.firstQueuedLevel = $$2;
            break;
         }
      }
   }
}
