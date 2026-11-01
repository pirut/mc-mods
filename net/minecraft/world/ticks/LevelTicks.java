package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class LevelTicks<T> implements LevelTickAccess<T> {
   private static final Comparator<LevelChunkTicks<?>> CONTAINER_DRAIN_ORDER = (p_193246_, p_193247_) -> ScheduledTick.INTRA_TICK_DRAIN_ORDER
      .compare(p_193246_.peek(), p_193247_.peek());
   private final LongPredicate tickCheck;
   private final Supplier<ProfilerFiller> profiler;
   private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenHashMap();
   private final Long2LongMap nextTickForContainer = Util.make(new Long2LongOpenHashMap(), p_193262_ -> p_193262_.defaultReturnValue(Long.MAX_VALUE));
   private final Queue<LevelChunkTicks<T>> containersToTick = new PriorityQueue<>(CONTAINER_DRAIN_ORDER);
   private final Queue<ScheduledTick<T>> toRunThisTick = new ArrayDeque<>();
   private final List<ScheduledTick<T>> alreadyRunThisTick = new ArrayList<>();
   private final Set<ScheduledTick<?>> toRunThisTickSet = new ObjectOpenCustomHashSet(ScheduledTick.UNIQUE_TICK_HASH);
   private final BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> chunkScheduleUpdater = (p_193249_, p_193250_) -> {
      if (p_193250_.equals(p_193249_.peek())) {
         this.updateContainerScheduling(p_193250_);
      }
   };

   public LevelTicks(LongPredicate p_193211_, Supplier<ProfilerFiller> p_193212_) {
      this.tickCheck = p_193211_;
      this.profiler = p_193212_;
   }

   public void addContainer(ChunkPos p_193232_, LevelChunkTicks<T> p_193233_) {
      long $$2 = p_193232_.toLong();
      this.allContainers.put($$2, p_193233_);
      ScheduledTick<T> $$3 = p_193233_.peek();
      if ($$3 != null) {
         this.nextTickForContainer.put($$2, $$3.triggerTick());
      }

      p_193233_.setOnTickAdded(this.chunkScheduleUpdater);
   }

   public void removeContainer(ChunkPos p_193230_) {
      long $$1 = p_193230_.toLong();
      LevelChunkTicks<T> $$2 = (LevelChunkTicks<T>)this.allContainers.remove($$1);
      this.nextTickForContainer.remove($$1);
      if ($$2 != null) {
         $$2.setOnTickAdded(null);
      }
   }

   @Override
   public void schedule(ScheduledTick<T> p_193252_) {
      long $$1 = ChunkPos.asLong(p_193252_.pos());
      LevelChunkTicks<T> $$2 = (LevelChunkTicks<T>)this.allContainers.get($$1);
      if ($$2 == null) {
         Util.pauseInIde(new IllegalStateException("Trying to schedule tick in not loaded position " + p_193252_.pos()));
      } else {
         $$2.schedule(p_193252_);
      }
   }

   public void tick(long p_193226_, int p_193227_, BiConsumer<BlockPos, T> p_193228_) {
      ProfilerFiller $$3 = this.profiler.get();
      $$3.push("collect");
      this.collectTicks(p_193226_, p_193227_, $$3);
      $$3.popPush("run");
      $$3.incrementCounter("ticksToRun", this.toRunThisTick.size());
      this.runCollectedTicks(p_193228_);
      $$3.popPush("cleanup");
      this.cleanupAfterTick();
      $$3.pop();
   }

   private void collectTicks(long p_193222_, int p_193223_, ProfilerFiller p_193224_) {
      this.sortContainersToTick(p_193222_);
      p_193224_.incrementCounter("containersToTick", this.containersToTick.size());
      this.drainContainers(p_193222_, p_193223_);
      this.rescheduleLeftoverContainers();
   }

   private void sortContainersToTick(long p_193217_) {
      ObjectIterator<Entry> $$1 = Long2LongMaps.fastIterator(this.nextTickForContainer);

      while ($$1.hasNext()) {
         Entry $$2 = (Entry)$$1.next();
         long $$3 = $$2.getLongKey();
         long $$4 = $$2.getLongValue();
         if ($$4 <= p_193217_) {
            LevelChunkTicks<T> $$5 = (LevelChunkTicks<T>)this.allContainers.get($$3);
            if ($$5 == null) {
               $$1.remove();
            } else {
               ScheduledTick<T> $$6 = $$5.peek();
               if ($$6 == null) {
                  $$1.remove();
               } else if ($$6.triggerTick() > p_193217_) {
                  $$2.setValue($$6.triggerTick());
               } else if (this.tickCheck.test($$3)) {
                  $$1.remove();
                  this.containersToTick.add($$5);
               }
            }
         }
      }
   }

   private void drainContainers(long p_193219_, int p_193220_) {
      LevelChunkTicks<T> $$2;
      while (this.canScheduleMoreTicks(p_193220_) && ($$2 = this.containersToTick.poll()) != null) {
         ScheduledTick<T> $$3 = $$2.poll();
         this.scheduleForThisTick($$3);
         this.drainFromCurrentContainer(this.containersToTick, $$2, p_193219_, p_193220_);
         ScheduledTick<T> $$4 = $$2.peek();
         if ($$4 != null) {
            if ($$4.triggerTick() <= p_193219_ && this.canScheduleMoreTicks(p_193220_)) {
               this.containersToTick.add($$2);
            } else {
               this.updateContainerScheduling($$4);
            }
         }
      }
   }

   private void rescheduleLeftoverContainers() {
      for (LevelChunkTicks<T> $$0 : this.containersToTick) {
         this.updateContainerScheduling($$0.peek());
      }
   }

   private void updateContainerScheduling(ScheduledTick<T> p_193280_) {
      this.nextTickForContainer.put(ChunkPos.asLong(p_193280_.pos()), p_193280_.triggerTick());
   }

   private void drainFromCurrentContainer(Queue<LevelChunkTicks<T>> p_193268_, LevelChunkTicks<T> p_193269_, long p_193270_, int p_193271_) {
      if (this.canScheduleMoreTicks(p_193271_)) {
         LevelChunkTicks<T> $$4 = p_193268_.peek();
         ScheduledTick<T> $$5 = $$4 != null ? $$4.peek() : null;

         while (this.canScheduleMoreTicks(p_193271_)) {
            ScheduledTick<T> $$6 = p_193269_.peek();
            if ($$6 == null || $$6.triggerTick() > p_193270_ || $$5 != null && ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare($$6, $$5) > 0) {
               break;
            }

            p_193269_.poll();
            this.scheduleForThisTick($$6);
         }
      }
   }

   private void scheduleForThisTick(ScheduledTick<T> p_193286_) {
      this.toRunThisTick.add(p_193286_);
   }

   private boolean canScheduleMoreTicks(int p_193215_) {
      return this.toRunThisTick.size() < p_193215_;
   }

   private void runCollectedTicks(BiConsumer<BlockPos, T> p_193273_) {
      while (!this.toRunThisTick.isEmpty()) {
         ScheduledTick<T> $$1 = this.toRunThisTick.poll();
         if (!this.toRunThisTickSet.isEmpty()) {
            this.toRunThisTickSet.remove($$1);
         }

         this.alreadyRunThisTick.add($$1);
         p_193273_.accept($$1.pos(), $$1.type());
      }
   }

   private void cleanupAfterTick() {
      this.toRunThisTick.clear();
      this.containersToTick.clear();
      this.alreadyRunThisTick.clear();
      this.toRunThisTickSet.clear();
   }

   @Override
   public boolean hasScheduledTick(BlockPos p_193254_, T p_193255_) {
      LevelChunkTicks<T> $$2 = (LevelChunkTicks<T>)this.allContainers.get(ChunkPos.asLong(p_193254_));
      return $$2 != null && $$2.hasScheduledTick(p_193254_, p_193255_);
   }

   @Override
   public boolean willTickThisTick(BlockPos p_193282_, T p_193283_) {
      this.calculateTickSetIfNeeded();
      return this.toRunThisTickSet.contains(ScheduledTick.probe(p_193283_, p_193282_));
   }

   private void calculateTickSetIfNeeded() {
      if (this.toRunThisTickSet.isEmpty() && !this.toRunThisTick.isEmpty()) {
         this.toRunThisTickSet.addAll(this.toRunThisTick);
      }
   }

   private void forContainersInArea(BoundingBox p_193237_, LevelTicks.PosAndContainerConsumer<T> p_193238_) {
      int $$2 = SectionPos.posToSectionCoord(p_193237_.minX());
      int $$3 = SectionPos.posToSectionCoord(p_193237_.minZ());
      int $$4 = SectionPos.posToSectionCoord(p_193237_.maxX());
      int $$5 = SectionPos.posToSectionCoord(p_193237_.maxZ());

      for (int $$6 = $$2; $$6 <= $$4; $$6++) {
         for (int $$7 = $$3; $$7 <= $$5; $$7++) {
            long $$8 = ChunkPos.asLong($$6, $$7);
            LevelChunkTicks<T> $$9 = (LevelChunkTicks<T>)this.allContainers.get($$8);
            if ($$9 != null) {
               p_193238_.accept($$8, $$9);
            }
         }
      }
   }

   public void clearArea(BoundingBox p_193235_) {
      Predicate<ScheduledTick<T>> $$1 = p_193241_ -> p_193235_.isInside(p_193241_.pos());
      this.forContainersInArea(p_193235_, (p_193276_, p_193277_) -> {
         ScheduledTick<T> $$3 = p_193277_.peek();
         p_193277_.removeIf($$1);
         ScheduledTick<T> $$4 = p_193277_.peek();
         if ($$4 != $$3) {
            if ($$4 != null) {
               this.updateContainerScheduling($$4);
            } else {
               this.nextTickForContainer.remove(p_193276_);
            }
         }
      });
      this.alreadyRunThisTick.removeIf($$1);
      this.toRunThisTick.removeIf($$1);
   }

   public void copyArea(BoundingBox p_193243_, Vec3i p_193244_) {
      this.copyAreaFrom(this, p_193243_, p_193244_);
   }

   public void copyAreaFrom(LevelTicks<T> p_265554_, BoundingBox p_265172_, Vec3i p_265318_) {
      List<ScheduledTick<T>> $$3 = new ArrayList<>();
      Predicate<ScheduledTick<T>> $$4 = p_200922_ -> p_265172_.isInside(p_200922_.pos());
      p_265554_.alreadyRunThisTick.stream().filter($$4).forEach($$3::add);
      p_265554_.toRunThisTick.stream().filter($$4).forEach($$3::add);
      p_265554_.forContainersInArea(p_265172_, (p_200931_, p_200932_) -> p_200932_.getAll().filter($$4).forEach($$3::add));
      LongSummaryStatistics $$5 = $$3.stream().mapToLong(ScheduledTick::subTickOrder).summaryStatistics();
      long $$6 = $$5.getMin();
      long $$7 = $$5.getMax();
      $$3.forEach(
         p_193260_ -> this.schedule(
            new ScheduledTick<>(
               p_193260_.type(), p_193260_.pos().offset(p_265318_), p_193260_.triggerTick(), p_193260_.priority(), p_193260_.subTickOrder() - $$6 + $$7 + 1L
            )
         )
      );
   }

   @Override
   public int count() {
      return this.allContainers.values().stream().mapToInt(TickAccess::count).sum();
   }

   @FunctionalInterface
   interface PosAndContainerConsumer<T> {
      void accept(long var1, LevelChunkTicks<T> var3);
   }
}
