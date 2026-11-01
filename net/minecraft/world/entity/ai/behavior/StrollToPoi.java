package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoi {
   public static BehaviorControl<PathfinderMob> create(MemoryModuleType<GlobalPos> poiPosMemory, float speedModifier, int closeEnoughDist, int maxDistFromPoi) {
      MutableLong mutableLong = new MutableLong(0L);
      return BehaviorBuilder.create(
         instance -> instance.group(instance.registered(MemoryModuleType.WALK_TARGET), instance.present(poiPosMemory))
            .apply(instance, (memoryAccessor, memoryAccessor2) -> (serverLevel, pathfinderMob, l) -> {
               GlobalPos globalPos = instance.get(memoryAccessor2);
               if (serverLevel.dimension() != globalPos.dimension() || !globalPos.pos().closerToCenterThan(pathfinderMob.position(), maxDistFromPoi)) {
                  return false;
               } else if (l <= mutableLong.longValue()) {
                  return true;
               } else {
                  memoryAccessor.set(new WalkTarget(globalPos.pos(), speedModifier, closeEnoughDist));
                  mutableLong.setValue(l + 80L);
                  return true;
               }
            })
      );
   }
}
