package net.minecraft.world.ticks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;

public class LevelChunkTicks<T> implements SerializableTickContainer<T>, TickContainerAccess<T> {
   private final Queue<ScheduledTick<T>> tickQueue = new PriorityQueue<>(ScheduledTick.DRAIN_ORDER);
   @Nullable
   private List<SavedTick<T>> pendingTicks;
   private final Set<ScheduledTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet(ScheduledTick.UNIQUE_TICK_HASH);
   @Nullable
   private BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> onTickAdded;

   public LevelChunkTicks() {
   }

   public LevelChunkTicks(List<SavedTick<T>> p_193169_) {
      this.pendingTicks = p_193169_;

      for (SavedTick<T> $$1 : p_193169_) {
         this.ticksPerPosition.add(ScheduledTick.probe($$1.type(), $$1.pos()));
      }
   }

   public void setOnTickAdded(@Nullable BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> p_193182_) {
      this.onTickAdded = p_193182_;
   }

   @Nullable
   public ScheduledTick<T> peek() {
      return this.tickQueue.peek();
   }

   @Nullable
   public ScheduledTick<T> poll() {
      ScheduledTick<T> $$0 = this.tickQueue.poll();
      if ($$0 != null) {
         this.ticksPerPosition.remove($$0);
      }

      return $$0;
   }

   @Override
   public void schedule(ScheduledTick<T> p_193177_) {
      if (this.ticksPerPosition.add(p_193177_)) {
         this.scheduleUnchecked(p_193177_);
      }
   }

   private void scheduleUnchecked(ScheduledTick<T> p_193194_) {
      this.tickQueue.add(p_193194_);
      if (this.onTickAdded != null) {
         this.onTickAdded.accept(this, p_193194_);
      }
   }

   @Override
   public boolean hasScheduledTick(BlockPos p_193179_, T p_193180_) {
      return this.ticksPerPosition.contains(ScheduledTick.probe(p_193180_, p_193179_));
   }

   public void removeIf(Predicate<ScheduledTick<T>> p_193184_) {
      Iterator<ScheduledTick<T>> $$1 = this.tickQueue.iterator();

      while ($$1.hasNext()) {
         ScheduledTick<T> $$2 = $$1.next();
         if (p_193184_.test($$2)) {
            $$1.remove();
            this.ticksPerPosition.remove($$2);
         }
      }
   }

   public Stream<ScheduledTick<T>> getAll() {
      return this.tickQueue.stream();
   }

   @Override
   public int count() {
      return this.tickQueue.size() + (this.pendingTicks != null ? this.pendingTicks.size() : 0);
   }

   public ListTag save(long p_193174_, Function<T, String> p_193175_) {
      ListTag $$2 = new ListTag();
      if (this.pendingTicks != null) {
         for (SavedTick<T> $$3 : this.pendingTicks) {
            $$2.add($$3.save(p_193175_));
         }
      }

      for (ScheduledTick<T> $$4 : this.tickQueue) {
         $$2.add(SavedTick.saveTick($$4, p_193175_, p_193174_));
      }

      return $$2;
   }

   public void unpack(long p_193172_) {
      if (this.pendingTicks != null) {
         int $$1 = -this.pendingTicks.size();

         for (SavedTick<T> $$2 : this.pendingTicks) {
            this.scheduleUnchecked($$2.unpack(p_193172_, $$1++));
         }
      }

      this.pendingTicks = null;
   }

   public static <T> LevelChunkTicks<T> load(ListTag p_193186_, Function<String, Optional<T>> p_193187_, ChunkPos p_193188_) {
      Builder<SavedTick<T>> $$3 = ImmutableList.builder();
      SavedTick.loadTickList(p_193186_, p_193187_, p_193188_, $$3::add);
      return new LevelChunkTicks<>($$3.build());
   }
}
