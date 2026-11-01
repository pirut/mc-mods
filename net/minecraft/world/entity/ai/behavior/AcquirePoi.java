package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;

public class AcquirePoi {
   public static final int SCAN_RANGE = 48;

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> acquirablePois,
      MemoryModuleType<GlobalPos> acquiringMemory,
      boolean onlyIfAdult,
      Optional<Byte> entityEventId,
      BiPredicate<ServerLevel, BlockPos> predicate
   ) {
      return create(acquirablePois, acquiringMemory, acquiringMemory, onlyIfAdult, entityEventId, predicate);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> acquirablePois, MemoryModuleType<GlobalPos> acquiringMemory, boolean onlyIfAdult, Optional<Byte> entityEventId
   ) {
      return create(acquirablePois, acquiringMemory, acquiringMemory, onlyIfAdult, entityEventId, (serverLevel, blockPos) -> true);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> acquirablePois,
      MemoryModuleType<GlobalPos> existingAbsentMemory,
      MemoryModuleType<GlobalPos> acquiringMemory,
      boolean onlyIfAdult,
      Optional<Byte> entityEventId,
      BiPredicate<ServerLevel, BlockPos> predicate
   ) {
      int i = 5;
      int j = 20;
      MutableLong mutableLong = new MutableLong(0L);
      Long2ObjectMap<AcquirePoi.JitteredLinearRetry> long2ObjectMap = new Long2ObjectOpenHashMap();
      OneShot<PathfinderMob> oneShot = BehaviorBuilder.create(
         instance -> instance.group(instance.absent(acquiringMemory))
            .apply(
               instance,
               memoryAccessor -> (serverLevel, pathfinderMob, l) -> {
                  if (onlyIfAdult && pathfinderMob.isBaby()) {
                     return false;
                  } else if (mutableLong.longValue() == 0L) {
                     mutableLong.setValue(serverLevel.getGameTime() + serverLevel.random.nextInt(20));
                     return false;
                  } else if (serverLevel.getGameTime() < mutableLong.longValue()) {
                     return false;
                  } else {
                     mutableLong.setValue(l + 20L + serverLevel.getRandom().nextInt(20));
                     PoiManager poiManager = serverLevel.getPoiManager();
                     long2ObjectMap.long2ObjectEntrySet().removeIf(entry -> !((AcquirePoi.JitteredLinearRetry)entry.getValue()).isStillValid(l));
                     Predicate<BlockPos> predicate2 = blockPos -> {
                        AcquirePoi.JitteredLinearRetry jitteredLinearRetry = (AcquirePoi.JitteredLinearRetry)long2ObjectMap.get(blockPos.asLong());
                        if (jitteredLinearRetry == null) {
                           return true;
                        } else if (!jitteredLinearRetry.shouldRetry(l)) {
                           return false;
                        } else {
                           jitteredLinearRetry.markAttempt(l);
                           return true;
                        }
                     };
                     Set<Pair<Holder<PoiType>, BlockPos>> set = poiManager.findAllClosestFirstWithType(
                           acquirablePois, predicate2, pathfinderMob.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                        )
                        .limit(5L)
                        .filter(pairx -> predicate.test(serverLevel, (BlockPos)pairx.getSecond()))
                        .collect(Collectors.toSet());
                     Path path = findPathToPois(pathfinderMob, set);
                     if (path != null && path.canReach()) {
                        BlockPos blockPos = path.getTarget();
                        poiManager.getType(blockPos).ifPresent(holder -> {
                           poiManager.take(acquirablePois, (holderx, blockPos2) -> blockPos2.equals(blockPos), blockPos, 1);
                           memoryAccessor.set(GlobalPos.of(serverLevel.dimension(), blockPos));
                           entityEventId.ifPresent(byte_ -> serverLevel.broadcastEntityEvent(pathfinderMob, byte_));
                           long2ObjectMap.clear();
                           serverLevel.debugSynchronizers().updatePoi(blockPos);
                        });
                     } else {
                        for (Pair<Holder<PoiType>, BlockPos> pair : set) {
                           long2ObjectMap.computeIfAbsent(((BlockPos)pair.getSecond()).asLong(), m -> new AcquirePoi.JitteredLinearRetry(serverLevel.random, l));
                        }
                     }

                     return true;
                  }
               }
            )
      );
      return acquiringMemory == existingAbsentMemory
         ? oneShot
         : BehaviorBuilder.create(instance -> instance.group(instance.absent(existingAbsentMemory)).apply(instance, memoryAccessor -> oneShot));
   }

   @Nullable
   public static Path findPathToPois(Mob mob, Set<Pair<Holder<PoiType>, BlockPos>> poiPositions) {
      if (poiPositions.isEmpty()) {
         return null;
      } else {
         Set<BlockPos> set = new HashSet<>();
         int i = 1;

         for (Pair<Holder<PoiType>, BlockPos> pair : poiPositions) {
            i = Math.max(i, ((PoiType)((Holder)pair.getFirst()).value()).validRange());
            set.add((BlockPos)pair.getSecond());
         }

         return mob.getNavigation().createPath(set, i);
      }
   }

   static class JitteredLinearRetry {
      private static final int MIN_INTERVAL_INCREASE = 40;
      private static final int MAX_INTERVAL_INCREASE = 80;
      private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
      private final RandomSource random;
      private long previousAttemptTimestamp;
      private long nextScheduledAttemptTimestamp;
      private int currentDelay;

      JitteredLinearRetry(RandomSource random, long timestamp) {
         this.random = random;
         this.markAttempt(timestamp);
      }

      public void markAttempt(long timestamp) {
         this.previousAttemptTimestamp = timestamp;
         int i = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min(i, 400);
         this.nextScheduledAttemptTimestamp = timestamp + this.currentDelay;
      }

      public boolean isStillValid(long timestamp) {
         return timestamp - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(long timestamp) {
         return timestamp >= this.nextScheduledAttemptTimestamp;
      }

      @Override
      public String toString() {
         return "RetryMarker{, previousAttemptAt="
            + this.previousAttemptTimestamp
            + ", nextScheduledAttemptAt="
            + this.nextScheduledAttemptTimestamp
            + ", currentDelay="
            + this.currentDelay
            + "}";
      }
   }
}
