package net.minecraft.world.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class Brain<E extends LivingEntity> {
   static final Logger LOGGER = LogUtils.getLogger();
   private final Supplier<Codec<Brain<E>>> codec;
   private static final int SCHEDULE_UPDATE_DELAY = 20;
   private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = Maps.newHashMap();
   private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
   private final Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority = Maps.newTreeMap();
   private Schedule schedule = Schedule.EMPTY;
   private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements = Maps.newHashMap();
   private final Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped = Maps.newHashMap();
   private Set<Activity> coreActivities = Sets.newHashSet();
   private final Set<Activity> activeActivities = Sets.newHashSet();
   private Activity defaultActivity = Activity.IDLE;
   private long lastScheduleUpdate = -9999L;

   public static <E extends LivingEntity> Brain.Provider<E> provider(
      Collection<? extends MemoryModuleType<?>> p_21924_, Collection<? extends SensorType<? extends Sensor<? super E>>> p_21925_
   ) {
      return new Brain.Provider<>(p_21924_, p_21925_);
   }

   public static <E extends LivingEntity> Codec<Brain<E>> codec(
      final Collection<? extends MemoryModuleType<?>> p_21947_, final Collection<? extends SensorType<? extends Sensor<? super E>>> p_21948_
   ) {
      final MutableObject<Codec<Brain<E>>> $$2 = new MutableObject();
      $$2.setValue(
         (new MapCodec<Brain<E>>() {
               public <T> Stream<T> keys(DynamicOps<T> p_22029_) {
                  return p_21947_.stream()
                     .flatMap(
                        p_22020_ -> p_22020_.getCodec().map(p_258254_ -> BuiltInRegistries.MEMORY_MODULE_TYPE.getKey((MemoryModuleType<?>)p_22020_)).stream()
                     )
                     .map(p_22018_ -> (T)p_22029_.createString(p_22018_.toString()));
               }

               public <T> DataResult<Brain<E>> decode(DynamicOps<T> p_22022_, MapLike<T> p_22023_) {
                  MutableObject<DataResult<Builder<Brain.MemoryValue<?>>>> $$2x = new MutableObject(DataResult.success(ImmutableList.builder()));
                  p_22023_.entries().forEach(p_344292_ -> {
                     DataResult<MemoryModuleType<?>> $$3x = BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().parse(p_22022_, p_344292_.getFirst());
                     DataResult<? extends Brain.MemoryValue<?>> $$4 = $$3x.flatMap(p_147350_ -> this.captureRead(p_147350_, p_22022_, (T)p_344292_.getSecond()));
                     $$2.setValue(((DataResult)$$2.getValue()).apply2(Builder::add, $$4));
                  });
                  ImmutableList<Brain.MemoryValue<?>> $$3 = ((DataResult)$$2x.getValue())
                     .resultOrPartial(Brain.LOGGER::error)
                     .<ImmutableList<Brain.MemoryValue<?>>>map(Builder::build)
                     .orElseGet(ImmutableList::of);
                  return DataResult.success(new Brain<>(p_21947_, p_21948_, $$3, $$2::getValue));
               }

               private <T, U> DataResult<Brain.MemoryValue<U>> captureRead(MemoryModuleType<U> p_21997_, DynamicOps<T> p_21998_, T p_21999_) {
                  return p_21997_.getCodec()
                     .<DataResult>map(DataResult::success)
                     .orElseGet(() -> DataResult.error(() -> "No codec for memory: " + p_21997_))
                     .flatMap(p_22011_ -> p_22011_.parse(p_21998_, p_21999_))
                     .map(p_21992_ -> new Brain.MemoryValue<>(p_21997_, Optional.of(p_21992_)));
               }

               public <T> RecordBuilder<T> encode(Brain<E> p_21985_, DynamicOps<T> p_21986_, RecordBuilder<T> p_21987_) {
                  p_21985_.memories().forEach(p_22007_ -> p_22007_.serialize(p_21986_, p_21987_));
                  return p_21987_;
               }
            })
            .fieldOf("memories")
            .codec()
      );
      return (Codec<Brain<E>>)$$2.getValue();
   }

   public Brain(
      Collection<? extends MemoryModuleType<?>> p_21855_,
      Collection<? extends SensorType<? extends Sensor<? super E>>> p_21856_,
      ImmutableList<Brain.MemoryValue<?>> p_21857_,
      Supplier<Codec<Brain<E>>> p_21858_
   ) {
      this.codec = p_21858_;

      for (MemoryModuleType<?> $$4 : p_21855_) {
         this.memories.put($$4, Optional.empty());
      }

      for (SensorType<? extends Sensor<? super E>> $$5 : p_21856_) {
         this.sensors.put($$5, (Sensor<? super E>)$$5.create());
      }

      for (Sensor<? super E> $$6 : this.sensors.values()) {
         for (MemoryModuleType<?> $$7 : $$6.requires()) {
            this.memories.put($$7, Optional.empty());
         }
      }

      UnmodifiableIterator var11 = p_21857_.iterator();

      while (var11.hasNext()) {
         Brain.MemoryValue<?> $$8 = (Brain.MemoryValue<?>)var11.next();
         $$8.setMemoryInternal(this);
      }
   }

   public <T> DataResult<T> serializeStart(DynamicOps<T> p_21915_) {
      return this.codec.get().encodeStart(p_21915_, this);
   }

   Stream<Brain.MemoryValue<?>> memories() {
      return this.memories.entrySet().stream().map(p_21929_ -> Brain.MemoryValue.createUnchecked(p_21929_.getKey(), p_21929_.getValue()));
   }

   public boolean hasMemoryValue(MemoryModuleType<?> p_21875_) {
      return this.checkMemory(p_21875_, MemoryStatus.VALUE_PRESENT);
   }

   public void clearMemories() {
      this.memories.keySet().forEach(p_276103_ -> this.memories.put((MemoryModuleType<?>)p_276103_, Optional.empty()));
   }

   public <U> void eraseMemory(MemoryModuleType<U> p_21937_) {
      this.setMemory(p_21937_, Optional.empty());
   }

   public <U> void setMemory(MemoryModuleType<U> p_21880_, @Nullable U p_21881_) {
      this.setMemory(p_21880_, Optional.ofNullable(p_21881_));
   }

   public <U> void setMemoryWithExpiry(MemoryModuleType<U> p_21883_, U p_21884_, long p_21885_) {
      this.setMemoryInternal(p_21883_, Optional.of(ExpirableValue.of(p_21884_, p_21885_)));
   }

   public <U> void setMemory(MemoryModuleType<U> p_21887_, Optional<? extends U> p_21888_) {
      this.setMemoryInternal(p_21887_, p_21888_.map(ExpirableValue::of));
   }

   <U> void setMemoryInternal(MemoryModuleType<U> p_21942_, Optional<? extends ExpirableValue<?>> p_21943_) {
      if (this.memories.containsKey(p_21942_)) {
         if (p_21943_.isPresent() && this.isEmptyCollection(p_21943_.get().getValue())) {
            this.eraseMemory(p_21942_);
         } else {
            this.memories.put(p_21942_, p_21943_);
         }
      }
   }

   public <U> Optional<U> getMemory(MemoryModuleType<U> p_21953_) {
      Optional<? extends ExpirableValue<?>> $$1 = this.memories.get(p_21953_);
      if ($$1 == null) {
         throw new IllegalStateException("Unregistered memory fetched: " + p_21953_);
      } else {
         return $$1.map(ExpirableValue::getValue);
      }
   }

   @Nullable
   public <U> Optional<U> getMemoryInternal(MemoryModuleType<U> p_259344_) {
      Optional<? extends ExpirableValue<?>> $$1 = this.memories.get(p_259344_);
      return $$1 == null ? null : $$1.map(ExpirableValue::getValue);
   }

   public <U> long getTimeUntilExpiry(MemoryModuleType<U> p_147342_) {
      Optional<? extends ExpirableValue<?>> $$1 = this.memories.get(p_147342_);
      return $$1.map(ExpirableValue::getTimeToLive).orElse(0L);
   }

   @Deprecated
   @VisibleForDebug
   public Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> getMemories() {
      return this.memories;
   }

   public <U> boolean isMemoryValue(MemoryModuleType<U> p_21939_, U p_21940_) {
      return !this.hasMemoryValue(p_21939_) ? false : this.getMemory(p_21939_).filter(p_21922_ -> p_21922_.equals(p_21940_)).isPresent();
   }

   public boolean checkMemory(MemoryModuleType<?> p_21877_, MemoryStatus p_21878_) {
      Optional<? extends ExpirableValue<?>> $$2 = this.memories.get(p_21877_);
      return $$2 == null
         ? false
         : p_21878_ == MemoryStatus.REGISTERED
            || p_21878_ == MemoryStatus.VALUE_PRESENT && $$2.isPresent()
            || p_21878_ == MemoryStatus.VALUE_ABSENT && $$2.isEmpty();
   }

   public Schedule getSchedule() {
      return this.schedule;
   }

   public void setSchedule(Schedule p_21913_) {
      this.schedule = p_21913_;
   }

   public void setCoreActivities(Set<Activity> p_21931_) {
      this.coreActivities = p_21931_;
   }

   @Deprecated
   @VisibleForDebug
   public Set<Activity> getActiveActivities() {
      return this.activeActivities;
   }

   @Deprecated
   @VisibleForDebug
   public List<BehaviorControl<? super E>> getRunningBehaviors() {
      List<BehaviorControl<? super E>> $$0 = new ObjectArrayList();

      for (Map<Activity, Set<BehaviorControl<? super E>>> $$1 : this.availableBehaviorsByPriority.values()) {
         for (Set<BehaviorControl<? super E>> $$2 : $$1.values()) {
            for (BehaviorControl<? super E> $$3 : $$2) {
               if ($$3.getStatus() == Behavior.Status.RUNNING) {
                  $$0.add($$3);
               }
            }
         }
      }

      return $$0;
   }

   public void useDefaultActivity() {
      this.setActiveActivity(this.defaultActivity);
   }

   public Optional<Activity> getActiveNonCoreActivity() {
      for (Activity $$0 : this.activeActivities) {
         if (!this.coreActivities.contains($$0)) {
            return Optional.of($$0);
         }
      }

      return Optional.empty();
   }

   public void setActiveActivityIfPossible(Activity p_21890_) {
      if (this.activityRequirementsAreMet(p_21890_)) {
         this.setActiveActivity(p_21890_);
      } else {
         this.useDefaultActivity();
      }
   }

   private void setActiveActivity(Activity p_21961_) {
      if (!this.isActive(p_21961_)) {
         this.eraseMemoriesForOtherActivitesThan(p_21961_);
         this.activeActivities.clear();
         this.activeActivities.addAll(this.coreActivities);
         this.activeActivities.add(p_21961_);
      }
   }

   private void eraseMemoriesForOtherActivitesThan(Activity p_21967_) {
      for (Activity $$1 : this.activeActivities) {
         if ($$1 != p_21967_) {
            Set<MemoryModuleType<?>> $$2 = this.activityMemoriesToEraseWhenStopped.get($$1);
            if ($$2 != null) {
               for (MemoryModuleType<?> $$3 : $$2) {
                  this.eraseMemory($$3);
               }
            }
         }
      }
   }

   public void updateActivityFromSchedule(long p_21863_, long p_21864_) {
      if (p_21864_ - this.lastScheduleUpdate > 20L) {
         this.lastScheduleUpdate = p_21864_;
         Activity $$2 = this.getSchedule().getActivityAt((int)(p_21863_ % 24000L));
         if (!this.activeActivities.contains($$2)) {
            this.setActiveActivityIfPossible($$2);
         }
      }
   }

   public void setActiveActivityToFirstValid(List<Activity> p_21927_) {
      for (Activity $$1 : p_21927_) {
         if (this.activityRequirementsAreMet($$1)) {
            this.setActiveActivity($$1);
            break;
         }
      }
   }

   public void setDefaultActivity(Activity p_21945_) {
      this.defaultActivity = p_21945_;
   }

   public void addActivity(Activity p_21892_, int p_21893_, ImmutableList<? extends BehaviorControl<? super E>> p_21894_) {
      this.addActivity(p_21892_, this.createPriorityPairs(p_21893_, p_21894_));
   }

   public void addActivityAndRemoveMemoryWhenStopped(
      Activity p_21896_, int p_21897_, ImmutableList<? extends BehaviorControl<? super E>> p_21898_, MemoryModuleType<?> p_21899_
   ) {
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> $$4 = ImmutableSet.of(Pair.of(p_21899_, MemoryStatus.VALUE_PRESENT));
      Set<MemoryModuleType<?>> $$5 = ImmutableSet.of(p_21899_);
      this.addActivityAndRemoveMemoriesWhenStopped(p_21896_, this.createPriorityPairs(p_21897_, p_21898_), $$4, $$5);
   }

   public void addActivity(Activity p_21901_, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> p_21902_) {
      this.addActivityAndRemoveMemoriesWhenStopped(p_21901_, p_21902_, ImmutableSet.of(), Sets.newHashSet());
   }

   public void addActivityWithConditions(
      Activity p_21904_,
      ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> p_21905_,
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> p_21906_
   ) {
      this.addActivityAndRemoveMemoriesWhenStopped(p_21904_, p_21905_, p_21906_, Sets.newHashSet());
   }

   public void addActivityAndRemoveMemoriesWhenStopped(
      Activity p_21908_,
      ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> p_21909_,
      Set<Pair<MemoryModuleType<?>, MemoryStatus>> p_21910_,
      Set<MemoryModuleType<?>> p_21911_
   ) {
      this.activityRequirements.put(p_21908_, p_21910_);
      if (!p_21911_.isEmpty()) {
         this.activityMemoriesToEraseWhenStopped.put(p_21908_, p_21911_);
      }

      UnmodifiableIterator var5 = p_21909_.iterator();

      while (var5.hasNext()) {
         Pair<Integer, ? extends BehaviorControl<? super E>> $$4 = (Pair<Integer, ? extends BehaviorControl<? super E>>)var5.next();
         this.availableBehaviorsByPriority
            .computeIfAbsent((Integer)$$4.getFirst(), p_21917_ -> Maps.newHashMap())
            .computeIfAbsent(p_21908_, p_21972_ -> Sets.newLinkedHashSet())
            .add((BehaviorControl<? super E>)$$4.getSecond());
      }
   }

   @VisibleForTesting
   public void removeAllBehaviors() {
      this.availableBehaviorsByPriority.clear();
   }

   public boolean isActive(Activity p_21955_) {
      return this.activeActivities.contains(p_21955_);
   }

   public Brain<E> copyWithoutBehaviors() {
      Brain<E> $$0 = new Brain<>(this.memories.keySet(), this.sensors.keySet(), ImmutableList.of(), this.codec);

      for (Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> $$1 : this.memories.entrySet()) {
         MemoryModuleType<?> $$2 = $$1.getKey();
         if ($$1.getValue().isPresent()) {
            $$0.memories.put($$2, $$1.getValue());
         }
      }

      return $$0;
   }

   public void tick(ServerLevel p_21866_, E p_21867_) {
      this.forgetOutdatedMemories();
      this.tickSensors(p_21866_, p_21867_);
      this.startEachNonRunningBehavior(p_21866_, p_21867_);
      this.tickEachRunningBehavior(p_21866_, p_21867_);
   }

   private void tickSensors(ServerLevel p_21950_, E p_21951_) {
      for (Sensor<? super E> $$2 : this.sensors.values()) {
         $$2.tick(p_21950_, p_21951_);
      }
   }

   private void forgetOutdatedMemories() {
      for (Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> $$0 : this.memories.entrySet()) {
         if ($$0.getValue().isPresent()) {
            ExpirableValue<?> $$1 = (ExpirableValue<?>)$$0.getValue().get();
            if ($$1.hasExpired()) {
               this.eraseMemory($$0.getKey());
            }

            $$1.tick();
         }
      }
   }

   public void stopAll(ServerLevel p_21934_, E p_21935_) {
      long $$2 = p_21935_.level().getGameTime();

      for (BehaviorControl<? super E> $$3 : this.getRunningBehaviors()) {
         $$3.doStop(p_21934_, p_21935_, $$2);
      }
   }

   private void startEachNonRunningBehavior(ServerLevel p_21958_, E p_21959_) {
      long $$2 = p_21958_.getGameTime();

      for (Map<Activity, Set<BehaviorControl<? super E>>> $$3 : this.availableBehaviorsByPriority.values()) {
         for (Entry<Activity, Set<BehaviorControl<? super E>>> $$4 : $$3.entrySet()) {
            Activity $$5 = $$4.getKey();
            if (this.activeActivities.contains($$5)) {
               for (BehaviorControl<? super E> $$7 : $$4.getValue()) {
                  if ($$7.getStatus() == Behavior.Status.STOPPED) {
                     $$7.tryStart(p_21958_, p_21959_, $$2);
                  }
               }
            }
         }
      }
   }

   private void tickEachRunningBehavior(ServerLevel p_21964_, E p_21965_) {
      long $$2 = p_21964_.getGameTime();

      for (BehaviorControl<? super E> $$3 : this.getRunningBehaviors()) {
         $$3.tickOrStop(p_21964_, p_21965_, $$2);
      }
   }

   private boolean activityRequirementsAreMet(Activity p_21970_) {
      if (!this.activityRequirements.containsKey(p_21970_)) {
         return false;
      } else {
         for (Pair<MemoryModuleType<?>, MemoryStatus> $$1 : this.activityRequirements.get(p_21970_)) {
            MemoryModuleType<?> $$2 = (MemoryModuleType<?>)$$1.getFirst();
            MemoryStatus $$3 = (MemoryStatus)$$1.getSecond();
            if (!this.checkMemory($$2, $$3)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isEmptyCollection(Object p_21919_) {
      return p_21919_ instanceof Collection && ((Collection)p_21919_).isEmpty();
   }

   ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> createPriorityPairs(
      int p_21860_, ImmutableList<? extends BehaviorControl<? super E>> p_21861_
   ) {
      int $$2 = p_21860_;
      Builder<Pair<Integer, ? extends BehaviorControl<? super E>>> $$3 = ImmutableList.builder();
      UnmodifiableIterator var5 = p_21861_.iterator();

      while (var5.hasNext()) {
         BehaviorControl<? super E> $$4 = (BehaviorControl<? super E>)var5.next();
         $$3.add(Pair.of($$2++, $$4));
      }

      return $$3.build();
   }

   static final class MemoryValue<U> {
      private final MemoryModuleType<U> type;
      private final Optional<? extends ExpirableValue<U>> value;

      static <U> Brain.MemoryValue<U> createUnchecked(MemoryModuleType<U> p_22060_, Optional<? extends ExpirableValue<?>> p_22061_) {
         return new Brain.MemoryValue<>(p_22060_, (Optional<? extends ExpirableValue<U>>)p_22061_);
      }

      MemoryValue(MemoryModuleType<U> p_22033_, Optional<? extends ExpirableValue<U>> p_22034_) {
         this.type = p_22033_;
         this.value = p_22034_;
      }

      void setMemoryInternal(Brain<?> p_22043_) {
         p_22043_.setMemoryInternal(this.type, this.value);
      }

      public <T> void serialize(DynamicOps<T> p_22048_, RecordBuilder<T> p_22049_) {
         this.type
            .getCodec()
            .ifPresent(
               p_22053_ -> this.value
                  .ifPresent(
                     p_344296_ -> p_22049_.add(
                        BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().encodeStart(p_22048_, this.type), p_22053_.encodeStart(p_22048_, p_344296_)
                     )
                  )
            );
      }
   }

   public static final class Provider<E extends LivingEntity> {
      private final Collection<? extends MemoryModuleType<?>> memoryTypes;
      private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes;
      private final Codec<Brain<E>> codec;

      Provider(Collection<? extends MemoryModuleType<?>> p_22066_, Collection<? extends SensorType<? extends Sensor<? super E>>> p_22067_) {
         this.memoryTypes = p_22066_;
         this.sensorTypes = p_22067_;
         this.codec = Brain.codec(p_22066_, p_22067_);
      }

      public Brain<E> makeBrain(Dynamic<?> p_22074_) {
         return this.codec
            .parse(p_22074_)
            .resultOrPartial(Brain.LOGGER::error)
            .orElseGet(() -> new Brain<>(this.memoryTypes, this.sensorTypes, ImmutableList.of(), () -> this.codec));
      }
   }
}
