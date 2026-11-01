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
      Predicate<Holder<PoiType>> p_260007_,
      MemoryModuleType<GlobalPos> p_259129_,
      boolean p_259108_,
      Optional<Byte> p_260129_,
      BiPredicate<ServerLevel, BlockPos> p_390607_
   ) {
      return create(p_260007_, p_259129_, p_259129_, p_259108_, p_260129_, p_390607_);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> p_259994_, MemoryModuleType<GlobalPos> p_259167_, boolean p_259077_, Optional<Byte> p_259824_
   ) {
      return create(p_259994_, p_259167_, p_259167_, p_259077_, p_259824_, (p_390574_, p_390575_) -> true);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> p_390608_,
      MemoryModuleType<GlobalPos> p_390595_,
      MemoryModuleType<GlobalPos> p_390601_,
      boolean p_390606_,
      Optional<Byte> p_390610_,
      BiPredicate<ServerLevel, BlockPos> p_390593_
   ) {
      int i = 5;
      int j = 20;
      MutableLong mutablelong = new MutableLong(0L);
      Long2ObjectMap<AcquirePoi.JitteredLinearRetry> long2objectmap = new Long2ObjectOpenHashMap();
      OneShot<PathfinderMob> oneshot = BehaviorBuilder.create(
         p_390560_ -> p_390560_.group(p_390560_.absent(p_390601_))
            .apply(
               p_390560_,
               p_390582_ -> (p_460415_, p_460416_, p_460417_) -> {
                  if (p_390606_ && p_460416_.isBaby()) {
                     return false;
                  } else if (mutablelong.longValue() == 0L) {
                     mutablelong.setValue(p_460415_.getGameTime() + p_460415_.random.nextInt(20));
                     return false;
                  } else if (p_460415_.getGameTime() < mutablelong.longValue()) {
                     return false;
                  } else {
                     mutablelong.setValue(p_460417_ + 20L + p_460415_.getRandom().nextInt(20));
                     PoiManager poimanager = p_460415_.getPoiManager();
                     long2objectmap.long2ObjectEntrySet().removeIf(p_22338_ -> !((AcquirePoi.JitteredLinearRetry)p_22338_.getValue()).isStillValid(p_460417_));
                     Predicate<BlockPos> predicate = p_258266_ -> {
                        AcquirePoi.JitteredLinearRetry acquirepoi$jitteredlinearretry = (AcquirePoi.JitteredLinearRetry)long2objectmap.get(p_258266_.asLong());
                        if (acquirepoi$jitteredlinearretry == null) {
                           return true;
                        } else if (!acquirepoi$jitteredlinearretry.shouldRetry(p_460417_)) {
                           return false;
                        } else {
                           acquirepoi$jitteredlinearretry.markAttempt(p_460417_);
                           return true;
                        }
                     };
                     Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllClosestFirstWithType(
                           p_390608_, predicate, p_460416_.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                        )
                        .limit(5L)
                        .filter(p_390563_ -> p_390593_.test(p_460415_, (BlockPos)p_390563_.getSecond()))
                        .collect(Collectors.toSet());
                     Path path = findPathToPois(p_460416_, set);
                     if (path != null && path.canReach()) {
                        BlockPos blockpos = path.getTarget();
                        poimanager.getType(blockpos).ifPresent(p_448937_ -> {
                           poimanager.take(p_390608_, (p_217108_, p_217109_) -> p_217109_.equals(blockpos), blockpos, 1);
                           p_390582_.set(GlobalPos.of(p_460415_.dimension(), blockpos));
                           p_390610_.ifPresent(p_147369_ -> p_460415_.broadcastEntityEvent(p_460416_, p_147369_));
                           long2objectmap.clear();
                           p_460415_.debugSynchronizers().updatePoi(blockpos);
                        });
                     } else {
                        for (Pair<Holder<PoiType>, BlockPos> pair : set) {
                           long2objectmap.computeIfAbsent(
                              ((BlockPos)pair.getSecond()).asLong(), p_264881_ -> new AcquirePoi.JitteredLinearRetry(p_460415_.random, p_460417_)
                           );
                        }
                     }

                     return true;
                  }
               }
            )
      );
      return p_390601_ == p_390595_
         ? oneshot
         : BehaviorBuilder.create(p_258269_ -> p_258269_.group(p_258269_.absent(p_390595_)).apply(p_258269_, p_258302_ -> oneshot));
   }

   @Nullable
   public static Path findPathToPois(Mob p_217098_, Set<Pair<Holder<PoiType>, BlockPos>> p_217099_) {
      if (p_217099_.isEmpty()) {
         return null;
      } else {
         Set<BlockPos> set = new HashSet<>();
         int i = 1;

         for (Pair<Holder<PoiType>, BlockPos> pair : p_217099_) {
            i = Math.max(i, ((PoiType)((Holder)pair.getFirst()).value()).validRange());
            set.add((BlockPos)pair.getSecond());
         }

         return p_217098_.getNavigation().createPath(set, i);
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

      JitteredLinearRetry(RandomSource p_217111_, long p_217112_) {
         this.random = p_217111_;
         this.markAttempt(p_217112_);
      }

      public void markAttempt(long p_22381_) {
         this.previousAttemptTimestamp = p_22381_;
         int i = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min(i, 400);
         this.nextScheduledAttemptTimestamp = p_22381_ + this.currentDelay;
      }

      public boolean isStillValid(long p_22383_) {
         return p_22383_ - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(long p_22385_) {
         return p_22385_ >= this.nextScheduledAttemptTimestamp;
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
