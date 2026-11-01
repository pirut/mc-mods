package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;
   private static final int[][] SWIM_XY_DISTANCE_TIERS = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

   public static OneShot<PathfinderMob> stroll(float speedModifier) {
      return stroll(speedModifier, true);
   }

   public static OneShot<PathfinderMob> stroll(float speedModifier, boolean mayStrollFromWater) {
      return strollFlyOrSwim(
         speedModifier,
         pathfinderMob -> LandRandomPos.getPos(pathfinderMob, 10, 7),
         mayStrollFromWater ? pathfinderMob -> true : pathfinderMob -> !pathfinderMob.isInWater()
      );
   }

   public static BehaviorControl<PathfinderMob> stroll(float speedModifier, int maxHorizontalDistance, int maxVerticalDistance) {
      return strollFlyOrSwim(
         speedModifier, pathfinderMob -> LandRandomPos.getPos(pathfinderMob, maxHorizontalDistance, maxVerticalDistance), pathfinderMob -> true
      );
   }

   public static BehaviorControl<PathfinderMob> fly(float speedModifier) {
      return strollFlyOrSwim(speedModifier, pathfinderMob -> getTargetFlyPos(pathfinderMob, 10, 7), pathfinderMob -> true);
   }

   public static BehaviorControl<PathfinderMob> swim(float speedModifier) {
      return strollFlyOrSwim(speedModifier, RandomStroll::getTargetSwimPos, Entity::isInWater);
   }

   private static OneShot<PathfinderMob> strollFlyOrSwim(float speedModifier, Function<PathfinderMob, Vec3> target, Predicate<PathfinderMob> canStroll) {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.absent(MemoryModuleType.WALK_TARGET)).apply(instance, memoryAccessor -> (serverLevel, pathfinderMob, l) -> {
            if (!canStroll.test(pathfinderMob)) {
               return false;
            } else {
               Optional<Vec3> optional = Optional.ofNullable(target.apply(pathfinderMob));
               memoryAccessor.setOrErase(optional.map(vec3 -> new WalkTarget(vec3, speedModifier, 0)));
               return true;
            }
         })
      );
   }

   @Nullable
   private static Vec3 getTargetSwimPos(PathfinderMob mob) {
      Vec3 vec3 = null;
      Vec3 vec32 = null;

      for (int[] is : SWIM_XY_DISTANCE_TIERS) {
         if (vec3 == null) {
            vec32 = BehaviorUtils.getRandomSwimmablePos(mob, is[0], is[1]);
         } else {
            vec32 = mob.position().add(mob.position().vectorTo(vec3).normalize().multiply(is[0], is[1], is[0]));
         }

         boolean bl = GoalUtils.mobRestricted(mob, is[0]);
         if (vec32 == null || mob.level().getFluidState(BlockPos.containing(vec32)).isEmpty() || GoalUtils.isRestricted(bl, mob, vec32)) {
            return vec3;
         }

         vec3 = vec32;
      }

      return vec32;
   }

   @Nullable
   private static Vec3 getTargetFlyPos(PathfinderMob mob, int maxDistance, int yRange) {
      Vec3 vec3 = mob.getViewVector(0.0F);
      return AirAndWaterRandomPos.getPos(mob, maxDistance, yRange, -2, vec3.x, vec3.z, (float) (Math.PI / 2));
   }
}
