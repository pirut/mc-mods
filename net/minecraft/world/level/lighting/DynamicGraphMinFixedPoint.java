package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.function.LongPredicate;
import net.minecraft.util.Mth;

public abstract class DynamicGraphMinFixedPoint {
   public static final long SOURCE = Long.MAX_VALUE;
   private static final int NO_COMPUTED_LEVEL = 255;
   protected final int levelCount;
   private final LeveledPriorityQueue priorityQueue;
   private final Long2ByteMap computedLevels;
   private volatile boolean hasWork;

   protected DynamicGraphMinFixedPoint(int p_75543_, int p_75544_, final int p_75545_) {
      if (p_75543_ >= 254) {
         throw new IllegalArgumentException("Level count must be < 254.");
      } else {
         this.levelCount = p_75543_;
         this.priorityQueue = new LeveledPriorityQueue(p_75543_, p_75544_);
         this.computedLevels = new Long2ByteOpenHashMap(p_75545_, 0.5F) {
            protected void rehash(int p_75611_) {
               if (p_75611_ > p_75545_) {
                  super.rehash(p_75611_);
               }
            }
         };
         this.computedLevels.defaultReturnValue((byte)-1);
      }
   }

   protected void removeFromQueue(long p_75601_) {
      int $$1 = this.computedLevels.remove(p_75601_) & 255;
      if ($$1 != 255) {
         int $$2 = this.getLevel(p_75601_);
         int $$3 = this.calculatePriority($$2, $$1);
         this.priorityQueue.dequeue(p_75601_, $$3, this.levelCount);
         this.hasWork = !this.priorityQueue.isEmpty();
      }
   }

   public void removeIf(LongPredicate p_75582_) {
      LongList $$1 = new LongArrayList();
      this.computedLevels.keySet().forEach(p_75586_ -> {
         if (p_75582_.test(p_75586_)) {
            $$1.add(p_75586_);
         }
      });
      $$1.forEach(this::removeFromQueue);
   }

   private int calculatePriority(int p_278256_, int p_278328_) {
      return Math.min(Math.min(p_278256_, p_278328_), this.levelCount - 1);
   }

   protected void checkNode(long p_75602_) {
      this.checkEdge(p_75602_, p_75602_, this.levelCount - 1, false);
   }

   protected void checkEdge(long p_75577_, long p_75578_, int p_75579_, boolean p_75580_) {
      this.checkEdge(p_75577_, p_75578_, p_75579_, this.getLevel(p_75578_), this.computedLevels.get(p_75578_) & 255, p_75580_);
      this.hasWork = !this.priorityQueue.isEmpty();
   }

   private void checkEdge(long p_75570_, long p_75571_, int p_75572_, int p_75573_, int p_75574_, boolean p_75575_) {
      if (!this.isSource(p_75571_)) {
         p_75572_ = Mth.clamp(p_75572_, 0, this.levelCount - 1);
         p_75573_ = Mth.clamp(p_75573_, 0, this.levelCount - 1);
         boolean $$6 = p_75574_ == 255;
         if ($$6) {
            p_75574_ = p_75573_;
         }

         int $$7;
         if (p_75575_) {
            $$7 = Math.min(p_75574_, p_75572_);
         } else {
            $$7 = Mth.clamp(this.getComputedLevel(p_75571_, p_75570_, p_75572_), 0, this.levelCount - 1);
         }

         int $$9 = this.calculatePriority(p_75573_, p_75574_);
         if (p_75573_ != $$7) {
            int $$10 = this.calculatePriority(p_75573_, $$7);
            if ($$9 != $$10 && !$$6) {
               this.priorityQueue.dequeue(p_75571_, $$9, $$10);
            }

            this.priorityQueue.enqueue(p_75571_, $$10);
            this.computedLevels.put(p_75571_, (byte)$$7);
         } else if (!$$6) {
            this.priorityQueue.dequeue(p_75571_, $$9, this.levelCount);
            this.computedLevels.remove(p_75571_);
         }
      }
   }

   protected final void checkNeighbor(long p_75594_, long p_75595_, int p_75596_, boolean p_75597_) {
      int $$4 = this.computedLevels.get(p_75595_) & 255;
      int $$5 = Mth.clamp(this.computeLevelFromNeighbor(p_75594_, p_75595_, p_75596_), 0, this.levelCount - 1);
      if (p_75597_) {
         this.checkEdge(p_75594_, p_75595_, $$5, this.getLevel(p_75595_), $$4, p_75597_);
      } else {
         boolean $$6 = $$4 == 255;
         int $$7;
         if ($$6) {
            $$7 = Mth.clamp(this.getLevel(p_75595_), 0, this.levelCount - 1);
         } else {
            $$7 = $$4;
         }

         if ($$5 == $$7) {
            this.checkEdge(p_75594_, p_75595_, this.levelCount - 1, $$6 ? $$7 : this.getLevel(p_75595_), $$4, p_75597_);
         }
      }
   }

   protected final boolean hasWork() {
      return this.hasWork;
   }

   protected final int runUpdates(int p_75589_) {
      if (this.priorityQueue.isEmpty()) {
         return p_75589_;
      } else {
         while (!this.priorityQueue.isEmpty() && p_75589_ > 0) {
            p_75589_--;
            long $$1 = this.priorityQueue.removeFirstLong();
            int $$2 = Mth.clamp(this.getLevel($$1), 0, this.levelCount - 1);
            int $$3 = this.computedLevels.remove($$1) & 255;
            if ($$3 < $$2) {
               this.setLevel($$1, $$3);
               this.checkNeighborsAfterUpdate($$1, $$3, true);
            } else if ($$3 > $$2) {
               this.setLevel($$1, this.levelCount - 1);
               if ($$3 != this.levelCount - 1) {
                  this.priorityQueue.enqueue($$1, this.calculatePriority(this.levelCount - 1, $$3));
                  this.computedLevels.put($$1, (byte)$$3);
               }

               this.checkNeighborsAfterUpdate($$1, $$2, false);
            }
         }

         this.hasWork = !this.priorityQueue.isEmpty();
         return p_75589_;
      }
   }

   public int getQueueSize() {
      return this.computedLevels.size();
   }

   protected boolean isSource(long p_75551_) {
      return p_75551_ == Long.MAX_VALUE;
   }

   protected abstract int getComputedLevel(long var1, long var3, int var5);

   protected abstract void checkNeighborsAfterUpdate(long var1, int var3, boolean var4);

   protected abstract int getLevel(long var1);

   protected abstract void setLevel(long var1, int var3);

   protected abstract int computeLevelFromNeighbor(long var1, long var3, int var5);
}
