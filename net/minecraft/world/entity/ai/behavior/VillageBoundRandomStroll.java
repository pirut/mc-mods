package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;

   public static OneShot<PathfinderMob> create(float speedModifier) {
      return create(speedModifier, 10, 7);
   }

   public static OneShot<PathfinderMob> create(float speedModifier, int maxHorizontalDist, int maxVerticalDist) {
      return BehaviorBuilder.create(
         instance -> instance.group(instance.absent(MemoryModuleType.WALK_TARGET))
            .apply(
               instance,
               memoryAccessor -> (serverLevel, pathfinderMob, l) -> {
                  BlockPos blockPos = pathfinderMob.blockPosition();
                  Vec3 vec3;
                  if (serverLevel.isVillage(blockPos)) {
                     vec3 = LandRandomPos.getPos(pathfinderMob, maxHorizontalDist, maxVerticalDist);
                  } else {
                     SectionPos sectionPos = SectionPos.of(blockPos);
                     SectionPos sectionPos2 = BehaviorUtils.findSectionClosestToVillage(serverLevel, sectionPos, 2);
                     if (sectionPos2 != sectionPos) {
                        vec3 = DefaultRandomPos.getPosTowards(
                           pathfinderMob, maxHorizontalDist, maxVerticalDist, Vec3.atBottomCenterOf(sectionPos2.center()), (float) (Math.PI / 2)
                        );
                     } else {
                        vec3 = LandRandomPos.getPos(pathfinderMob, maxHorizontalDist, maxVerticalDist);
                     }
                  }

                  memoryAccessor.setOrErase(Optional.ofNullable(vec3).map(vec3x -> new WalkTarget(vec3x, speedModifier, 0)));
                  return true;
               }
            )
      );
   }
}
